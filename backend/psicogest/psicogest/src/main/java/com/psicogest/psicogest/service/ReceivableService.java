package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.MoneyRules;
import com.psicogest.psicogest.dto.ReceivableResponseDTO;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.Receivable.ReceivableStatus;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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

    public ReceivableService(
            ReceivableRepository receivableRepository,
            PaymentAllocationRepository allocationRepository,
            Clock clock
    ) {
        this.receivableRepository = receivableRepository;
        this.allocationRepository = allocationRepository;
        this.clock = clock;
    }

    /**
     * 37, 38. Busca cobrança com saldos calculados
     * 
     * GET /receivables/{receivableId}
     * 
     * @param receivableId ID da cobrança
     * @return DTO com saldos e status temporal
     */
    public ReceivableResponseDTO findById(UUID receivableId) {

        Receivable receivable =
                receivableRepository
                        .findById(receivableId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Cobrança não encontrada"
                                        )
                        );

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
}
