package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.PaymentAllocationCreateDTO;
import com.psicogest.psicogest.dto.PaymentAllocationResponseDTO;
import com.psicogest.psicogest.dto.PaymentCollectionCreateDTO;
import com.psicogest.psicogest.dto.PaymentCollectionResponseDTO;
import com.psicogest.psicogest.dto.PaymentCreateDTO;
import com.psicogest.psicogest.dto.PaymentResponseDTO;
import com.psicogest.psicogest.exception.FinanceConflictException;
import com.psicogest.psicogest.exception.IdempotencyConflictException;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.security.SecurityActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Records a manually received payment and allocates it in one transaction.
 * The payment idempotency key also identifies the single receivable allocation.
 */
@Service
public class PaymentCollectionService {

    private final PaymentService paymentService;
    private final PaymentAllocationService allocationService;
    private final PaymentAllocationRepository allocationRepository;

    public PaymentCollectionService(
            PaymentService paymentService,
            PaymentAllocationService allocationService,
            PaymentAllocationRepository allocationRepository
    ) {
        this.paymentService = paymentService;
        this.allocationService = allocationService;
        this.allocationRepository = allocationRepository;
    }

    @Transactional
    public PaymentCollectionResponseDTO collect(
            String idempotencyKey,
            PaymentCollectionCreateDTO request,
            SecurityActor actor
    ) {
        PaymentCreateDTO paymentRequest = new PaymentCreateDTO(
                request.patientId(),
                request.amount(),
                request.paymentMethod(),
                null,
                null,
                request.description()
        );
        PaymentResponseDTO payment = paymentService.create(idempotencyKey, paymentRequest, actor);

        List<PaymentAllocation> existing = allocationRepository.findByPaymentId(payment.id());
        if (!existing.isEmpty()) {
            if (existing.size() == 1 && matches(existing.get(0), request)) {
                return new PaymentCollectionResponseDTO(payment, toResponse(existing.getFirst()));
            }
            throw new IdempotencyConflictException(
                    "A chave de idempotência já foi usada para outra alocação"
            );
        }

        if (payment.status() == PaymentStatus.PENDING) {
            payment = paymentService.confirm(payment.id(), null, actor);
        } else if (payment.status() != PaymentStatus.CONFIRMED) {
            throw new FinanceConflictException("Pagamento não pode ser confirmado para alocação");
        }

        if (payment.availableAmount().compareTo(request.amount()) != 0) {
            throw new IdempotencyConflictException(
                    "O pagamento já possui uma operação financeira diferente"
            );
        }

        PaymentAllocationResponseDTO allocation = allocationService.allocate(
                payment.id(),
                new PaymentAllocationCreateDTO(request.receivableId(), request.amount()),
                actor
        );
        PaymentResponseDTO collected = new PaymentResponseDTO(payment.id(), payment.patientId(),
                payment.clinicId(), payment.amount(), payment.allocatedAmount().add(allocation.amount()),
                payment.availableAmount().subtract(allocation.amount()), payment.currency(),
                payment.paymentMethod(), payment.status(), payment.receivedAt(), payment.createdAt());
        return new PaymentCollectionResponseDTO(collected, allocation);
    }

    private static boolean matches(PaymentAllocation allocation, PaymentCollectionCreateDTO request) {
        return allocation.getReceivable().getId().equals(request.receivableId())
                && allocation.getAmount().compareTo(request.amount()) == 0;
    }

    private static PaymentAllocationResponseDTO toResponse(PaymentAllocation allocation) {
        return new PaymentAllocationResponseDTO(
                allocation.getId(),
                allocation.getPayment().getId(),
                allocation.getReceivable().getId(),
                allocation.getAmount(),
                allocation.getCreatedAt()
        );
    }
}
