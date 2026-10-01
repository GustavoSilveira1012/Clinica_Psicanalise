package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.model.entity.Refund.RefundStatus;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.repository.RefundRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.service.finance.FinanceAuthorizationService;
import com.psicogest.psicogest.domain.finance.ReceivableStateMachine;
import com.psicogest.psicogest.exception.AuthorizationException;
import com.psicogest.psicogest.dto.PaymentAllocationCreateDTO;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAllocationAuthorizationTest {

    private static final UUID PAYMENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVABLE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final SecurityActor ACTOR = new SecurityActor(7L, UUID.randomUUID(), "correlation", null, null);

    private final PaymentAllocationRepository allocationRepository = mock(PaymentAllocationRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final RefundRepository refundRepository = mock(RefundRepository.class);
    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final FinanceBalanceService balanceService = mock(FinanceBalanceService.class);
    private final ReceivableStateMachine stateMachine = mock(ReceivableStateMachine.class);
    private final AuditService auditService = mock(AuditService.class);
    private final FinanceAuthorizationService authorizationService = mock(FinanceAuthorizationService.class);
    private final PaymentAllocationService service = new PaymentAllocationService(
            allocationRepository,
            paymentRepository,
            refundRepository,
            receivableRepository,
            balanceService,
            stateMachine,
            auditService,
            authorizationService
    );

    @Test
    void rejectsAnUnauthorizedPaymentBeforeReadingItsReceivable() {
        var payment = mock(Payment.class);
        var clinic = mock(Clinic.class);
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(payment.getClinic()).thenReturn(clinic);
        when(clinic.getId()).thenReturn(11L);
        doThrow(new AuthorizationException("forbidden"))
                .when(authorizationService).validateClinicAccess(11L, ACTOR);

        assertThatThrownBy(() -> service.allocate(
                PAYMENT_ID,
                new PaymentAllocationCreateDTO(RECEIVABLE_ID, new java.math.BigDecimal("25.00")),
                ACTOR
        )).isInstanceOf(AuthorizationException.class);

        verify(receivableRepository, never()).findByIdForUpdate(any());
        verify(allocationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnUnauthorizedReceivableBeforeFinancialStateChecksOrWrites() {
        var payment = mock(Payment.class);
        var paymentClinic = mock(Clinic.class);
        var receivable = mock(Receivable.class);
        var receivableClinic = mock(Clinic.class);
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(payment.getClinic()).thenReturn(paymentClinic);
        when(paymentClinic.getId()).thenReturn(11L);
        when(payment.getStatus()).thenReturn(Payment.PaymentStatus.CONFIRMED);
        when(refundRepository.existsByPaymentIdAndStatus(PAYMENT_ID, RefundStatus.PENDING)).thenReturn(false);
        when(receivableRepository.findByIdForUpdate(RECEIVABLE_ID)).thenReturn(Optional.of(receivable));
        when(receivable.getClinic()).thenReturn(receivableClinic);
        when(receivableClinic.getId()).thenReturn(22L);
        doThrow(new AuthorizationException("forbidden"))
                .when(authorizationService).validateClinicAccess(22L, ACTOR);

        assertThatThrownBy(() -> service.allocate(
                PAYMENT_ID,
                new PaymentAllocationCreateDTO(RECEIVABLE_ID, new java.math.BigDecimal("25.00")),
                ACTOR
        )).isInstanceOf(AuthorizationException.class);

        verify(authorizationService).validateClinicAccess(11L, ACTOR);
        verify(authorizationService).validateClinicAccess(22L, ACTOR);
        verify(balanceService, never()).availablePaymentAmount(any());
        verify(allocationRepository, never()).saveAndFlush(any());
    }
}
