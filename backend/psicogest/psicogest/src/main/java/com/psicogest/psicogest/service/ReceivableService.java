package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.dto.ReceivableResponseDTO;
import com.psicogest.psicogest.dto.appointment.ReceivableCreateDTO;
import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.Receivable.ReceivableStatus;
import com.psicogest.psicogest.model.enums.ClinicUserMembershipStatus;
import com.psicogest.psicogest.model.enums.ReceivableOriginType;
import com.psicogest.psicogest.repository.AppointmentRepository;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.ClinicRepository;
import com.psicogest.psicogest.repository.ClinicUserMembershipRepository;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import com.psicogest.psicogest.service.finance.FinanceAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service para operações com contas a receber
 * 
 * 37. GET /receivables/{receivableId} com saldos
 * 38. Usa Clock injetável para testes temporais
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final Clock clock;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicUserMembershipRepository membershipRepository;
    private final FinanceAuthorizationService financeAuthorizationService;

    public ReceivableService(
            ReceivableRepository receivableRepository,
            PaymentAllocationRepository allocationRepository,
            Clock clock,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            ClinicRepository clinicRepository,
            ClinicUserMembershipRepository membershipRepository,
            FinanceAuthorizationService financeAuthorizationService
    ) {
        this.receivableRepository = receivableRepository;
        this.allocationRepository = allocationRepository;
        this.clock = clock;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicRepository = clinicRepository;
        this.membershipRepository = membershipRepository;
        this.financeAuthorizationService = financeAuthorizationService;
    }

    public List<ReceivableResponseDTO> findAll(SecurityActor actor) {
        Clinic clinic = resolveFinancialClinic(actor);
        financeAuthorizationService.validateClinicAccess(clinic.getId(), actor);
        return receivableRepository.findByClinicIdOrderByDueDateAsc(clinic.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReceivableResponseDTO create(ReceivableCreateDTO dto, SecurityActor actor) {
        Clinic clinic = resolveFinancialClinic(actor);
        financeAuthorizationService.validateClinicAccess(clinic.getId(), actor);
        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        if (!Boolean.TRUE.equals(patient.getActive())) {
            throw new FinanceValidationException("Não é possível lançar cobrança para paciente inativo");
        }

        Appointment appointment = null;
        if (dto.appointmentId() != null) {
            appointment = appointmentRepository.findById(dto.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada"));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new FinanceValidationException("A consulta não pertence ao paciente informado");
            }
            if (appointment.getClinicMembership() == null ||
                    !clinic.getId().equals(appointment.getClinicMembership().getClinic().getId())) {
                throw new FinanceValidationException("A consulta não pertence à clínica financeira selecionada");
            }
            if (receivableRepository.existsByAppointmentId(appointment.getId())) {
                throw new FinanceValidationException("Já existe um recebível para esta consulta");
            }
        }

        BigDecimal gross = MoneyRules.normalize(dto.grossAmount());
        BigDecimal discount = MoneyRules.normalize(dto.discountAmount());
        BigDecimal net = MoneyRules.normalize(gross.subtract(discount));
        if (discount.compareTo(gross) >= 0 || net.signum() <= 0) {
            throw new FinanceValidationException("O desconto deve ser menor que o valor bruto");
        }

        Instant now = Instant.now(clock);
        Receivable saved = receivableRepository.saveAndFlush(Receivable.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .clinic(clinic)
                .appointment(appointment)
                .originType(appointment == null ? ReceivableOriginType.MANUAL : ReceivableOriginType.APPOINTMENT)
                .description(dto.description().trim())
                .grossAmount(gross)
                .discountAmount(discount)
                .netAmount(net)
                .currency("BRL")
                .dueDate(dto.dueDate())
                .status(ReceivableStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build());
        return toResponse(saved);
    }

    /**
     * 37, 38. Busca cobrança com saldos calculados
     * 
     * GET /receivables/{receivableId}
     * 
     * @param receivableId ID da cobrança
     * @return DTO com saldos e status temporal
     */
    public ReceivableResponseDTO findById(UUID receivableId, SecurityActor actor) {

        Receivable receivable =
                receivableRepository
                        .findById(receivableId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Cobrança não encontrada"
                                        )
                        );

        if (receivable.getClinic() == null) {
            throw new ResourceNotFoundException("Cobrança não encontrada");
        }
        financeAuthorizationService.validateClinicAccess(receivable.getClinic().getId(), actor);

        // 37. Calcular saldos
        BigDecimal paidAmount =
                allocationRepository
                        .sumEffectiveAllocation(
                                receivableId
                        );

        paidAmount =
                MoneyRules.normalize(paidAmount);

        BigDecimal outstandingAmount =
                receivable.getNetAmount()
                        .subtract(paidAmount);

        outstandingAmount =
                MoneyRules.normalize(
                        outstandingAmount
                );

        // 37, 38. Calcular overdue com Clock
        LocalDate today =
                LocalDate.now(clock);

        boolean overdue =
                receivable
                        .getDueDate()
                        .isBefore(today)

                        &&
                        outstandingAmount.signum() > 0

                        &&
                        receivable.getStatus()
                                != ReceivableStatus.CANCELLED;

        log.info(
                "Cobrança consultada: id={}, status={}, overdue={}",
                receivableId,
                receivable.getStatus(),
                overdue
        );

        return new ReceivableResponseDTO(

                receivable.getId(),

                receivable.getPatient() != null
                        ? receivable.getPatient().getId()
                        : null,

                receivable.getClinic() != null
                        ? receivable.getClinic().getId()
                        : null,

                receivable.getAppointment() != null
                        ? receivable.getAppointment().getId()
                        : null,

                receivable.getDescription(),

                receivable.getGrossAmount(),

                receivable.getDiscountAmount(),

                receivable.getNetAmount(),

                paidAmount,

                outstandingAmount,

                overdue,

                receivable.getStatus(),

                receivable.getDueDate(),

                receivable.getCreatedAt()
        );
    }

    private ReceivableResponseDTO toResponse(Receivable receivable) {
        BigDecimal paidAmount = MoneyRules.normalize(
                allocationRepository.sumEffectiveAllocation(receivable.getId()));
        BigDecimal outstandingAmount = MoneyRules.normalize(
                receivable.getNetAmount().subtract(paidAmount));
        boolean overdue = receivable.getDueDate().isBefore(LocalDate.now(clock))
                && outstandingAmount.signum() > 0
                && receivable.getStatus() != ReceivableStatus.CANCELLED;
        return new ReceivableResponseDTO(
                receivable.getId(),
                receivable.getPatient() == null ? null : receivable.getPatient().getId(),
                receivable.getClinic() == null ? null : receivable.getClinic().getId(),
                receivable.getAppointment() == null ? null : receivable.getAppointment().getId(),
                receivable.getDescription(),
                receivable.getGrossAmount(),
                receivable.getDiscountAmount(),
                receivable.getNetAmount(),
                paidAmount,
                outstandingAmount,
                overdue,
                receivable.getStatus(),
                receivable.getDueDate(),
                receivable.getCreatedAt());
    }

    private Clinic resolveFinancialClinic(SecurityActor actor) {
        if (actor == null || actor.userId() == null) {
            throw new FinanceValidationException("Usuário financeiro não identificado");
        }
        var tenant = TenantContextHolder.get();
        if (tenant != null && tenant.organizationId() != null) {
            return clinicRepository.findFirstByOrganizationIdAndActiveTrue(tenant.organizationId())
                    .orElseThrow(() -> new FinanceValidationException(
                            "Nenhuma clínica ativa está vinculada à organização selecionada"));
        }
        return membershipRepository.findByUserIdAndStatus(actor.userId(), ClinicUserMembershipStatus.ACTIVE)
                .stream()
                .map(membership -> membership.getClinic())
                .filter(clinic -> clinic != null && Boolean.TRUE.equals(clinic.getActive()))
                .findFirst()
                .orElseThrow(() -> new FinanceValidationException(
                        "Usuário não possui uma clínica financeira ativa"));
    }
}
