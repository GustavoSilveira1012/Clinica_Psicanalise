package com.psicogest.psicogest.service;

import com.psicogest.psicogest.dto.PaymentAllocationCreateDTO;
import com.psicogest.psicogest.dto.PaymentAllocationResponseDTO;
import com.psicogest.psicogest.dto.PaymentCollectionCreateDTO;
import com.psicogest.psicogest.dto.PaymentCreateDTO;
import com.psicogest.psicogest.dto.PaymentResponseDTO;
import com.psicogest.psicogest.exception.IdempotencyConflictException;
import com.psicogest.psicogest.model.entity.Payment.PaymentMethod;
import com.psicogest.psicogest.model.entity.Payment.PaymentStatus;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.PaymentAllocation;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCollectionServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVABLE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ALLOCATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final BigDecimal AMOUNT = new BigDecimal("280.00");

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentAllocationService allocationService = mock(PaymentAllocationService.class);
    private final PaymentAllocationRepository allocationRepository = mock(PaymentAllocationRepository.class);
    private final PaymentCollectionService service = new PaymentCollectionService(
            paymentService, allocationService, allocationRepository);

    @Test
    void createsConfirmsAndAllocatesAsOneCollectionOperation() {
        var request = request(RECEIVABLE_ID);
        when(paymentService.create(eq("operation-key"), any(PaymentCreateDTO.class), eq(null)))
                .thenReturn(payment(PaymentStatus.PENDING, AMOUNT));
        when(paymentService.confirm(eq(PAYMENT_ID), eq(null), eq(null)))
                .thenReturn(payment(PaymentStatus.CONFIRMED, AMOUNT));
        when(allocationRepository.findByPaymentId(PAYMENT_ID)).thenReturn(List.of());
        var allocation = new PaymentAllocationResponseDTO(ALLOCATION_ID, PAYMENT_ID, RECEIVABLE_ID, AMOUNT, Instant.EPOCH);
        when(allocationService.allocate(eq(PAYMENT_ID), any(PaymentAllocationCreateDTO.class), eq(null)))
                .thenReturn(allocation);

        var result = service.collect("operation-key", request, null);

        assertThat(result.payment().status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(result.payment().allocatedAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(result.payment().availableAmount()).isZero();
        assertThat(result.allocation()).isEqualTo(allocation);
        verify(paymentService).confirm(PAYMENT_ID, null, null);
        verify(allocationService).allocate(eq(PAYMENT_ID), any(PaymentAllocationCreateDTO.class), eq(null));
    }

    @Test
    void returnsTheExistingAllocationWhenTheSameRequestIsRetried() {
        var request = request(RECEIVABLE_ID);
        var savedAllocation = mock(PaymentAllocation.class);
        var receivable = mock(Receivable.class);
        var paymentEntity = mock(Payment.class);
        when(paymentService.create(eq("operation-key"), any(PaymentCreateDTO.class), eq(null)))
                .thenReturn(payment(PaymentStatus.CONFIRMED, BigDecimal.ZERO));
        when(allocationRepository.findByPaymentId(PAYMENT_ID)).thenReturn(List.of(savedAllocation));
        when(savedAllocation.getReceivable()).thenReturn(receivable);
        when(receivable.getId()).thenReturn(RECEIVABLE_ID);
        when(savedAllocation.getId()).thenReturn(ALLOCATION_ID);
        when(savedAllocation.getPayment()).thenReturn(paymentEntity);
        when(paymentEntity.getId()).thenReturn(PAYMENT_ID);
        when(savedAllocation.getAmount()).thenReturn(AMOUNT);
        when(savedAllocation.getCreatedAt()).thenReturn(Instant.EPOCH);

        var result = service.collect("operation-key", request, null);

        assertThat(result.allocation()).isEqualTo(new PaymentAllocationResponseDTO(
                ALLOCATION_ID, PAYMENT_ID, RECEIVABLE_ID, AMOUNT, Instant.EPOCH));
        verify(paymentService, never()).confirm(any(), any(), any());
        verify(allocationService, never()).allocate(any(), any(), any());
    }

    @Test
    void rejectsReusingThePaymentKeyForADifferentReceivable() {
        var request = request(RECEIVABLE_ID);
        var savedAllocation = mock(PaymentAllocation.class);
        var receivable = mock(Receivable.class);
        when(paymentService.create(eq("operation-key"), any(PaymentCreateDTO.class), eq(null)))
                .thenReturn(payment(PaymentStatus.CONFIRMED, BigDecimal.ZERO));
        when(allocationRepository.findByPaymentId(PAYMENT_ID)).thenReturn(List.of(savedAllocation));
        when(savedAllocation.getReceivable()).thenReturn(receivable);
        when(receivable.getId()).thenReturn(UUID.fromString("20000000-0000-0000-0000-000000000002"));

        assertThatThrownBy(() -> service.collect("operation-key", request, null))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(allocationService, never()).allocate(any(), any(), any());
    }

    private static PaymentCollectionCreateDTO request(UUID receivableId) {
        return new PaymentCollectionCreateDTO(42L, receivableId, AMOUNT, PaymentMethod.PIX, "Sessão");
    }

    private static PaymentResponseDTO payment(PaymentStatus status, BigDecimal availableAmount) {
        return new PaymentResponseDTO(PAYMENT_ID, 42L, 7L, AMOUNT,
                status == PaymentStatus.PENDING ? BigDecimal.ZERO : AMOUNT.subtract(availableAmount),
                availableAmount, "BRL", PaymentMethod.PIX, status, Instant.EPOCH, Instant.EPOCH);
    }
}
