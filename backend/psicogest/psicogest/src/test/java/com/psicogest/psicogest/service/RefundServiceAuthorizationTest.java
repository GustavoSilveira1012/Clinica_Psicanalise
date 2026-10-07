package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.finance.RefundStateMachine;
import com.psicogest.psicogest.dto.RefundCreateDTO;
import com.psicogest.psicogest.exception.AuthorizationException;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.Refund.RefundReason;
import com.psicogest.psicogest.repository.PaymentAllocationRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.repository.RefundAllocationRepository;
import com.psicogest.psicogest.repository.RefundRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityHashService;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.service.finance.FinanceAuthorizationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class RefundServiceAuthorizationTest {

    private static final UUID PAYMENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID REFUND_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final SecurityActor ACTOR = new SecurityActor(7L, UUID.randomUUID(), "correlation", null, null);

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentAllocationRepository paymentAllocationRepository = mock(PaymentAllocationRepository.class);
    private final RefundRepository refundRepository = mock(RefundRepository.class);
    private final RefundAllocationRepository refundAllocationRepository = mock(RefundAllocationRepository.class);
    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final FinanceBalanceService balanceService = mock(FinanceBalanceService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final SecurityHashService hashService = mock(SecurityHashService.class);
    private final RefundStateMachine stateMachine = mock(RefundStateMachine.class);
    private final FinanceAuthorizationService authorizationService = mock(FinanceAuthorizationService.class);
    private final RefundService service = new RefundService(
            paymentRepository,
            paymentAllocationRepository,
            refundRepository,
            refundAllocationRepository,
            receivableRepository,
            balanceService,
            auditService,
            hashService,
            stateMachine,
            Clock.systemUTC(),
            authorizationService,
            mock(org.springframework.jdbc.core.JdbcTemplate.class)
    );

    @Test
    void rejectsRefundCreationBeforeCheckingOrCreatingRefunds() {
        var payment = unauthorizedPayment();
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        doThrow(new AuthorizationException("forbidden"))
                .when(authorizationService).validateClinicAccess(11L, ACTOR);

        assertThatThrownBy(() -> service.create(
                PAYMENT_ID,
                "idempotency-key",
                new RefundCreateDTO(BigDecimal.TEN, RefundReason.OTHER, List.of()),
                ACTOR
        )).isInstanceOf(AuthorizationException.class);

        verify(refundRepository, never()).existsByPaymentIdAndStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(refundRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRefundConfirmationBeforeLockingOrChangingTheRefund() {
        var payment = unauthorizedPayment();
        when(refundRepository.findPaymentId(REFUND_ID)).thenReturn(Optional.of(PAYMENT_ID));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        doThrow(new AuthorizationException("forbidden"))
                .when(authorizationService).validateClinicAccess(11L, ACTOR);

        assertThatThrownBy(() -> service.confirm(REFUND_ID, ACTOR))
                .isInstanceOf(AuthorizationException.class);

        verify(refundRepository, never()).findByIdForUpdate(REFUND_ID);
        verify(refundRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRefundFailureBeforeLockingOrChangingTheRefund() {
        assertManualRefundTransitionIsDeniedBeforeRefundLock(service::fail);
    }

    @Test
    void rejectsRefundCancellationBeforeLockingOrChangingTheRefund() {
        assertManualRefundTransitionIsDeniedBeforeRefundLock(service::cancel);
    }

    private void assertManualRefundTransitionIsDeniedBeforeRefundLock(RefundTransition transition) {
        var payment = unauthorizedPayment();
        when(refundRepository.findPaymentId(REFUND_ID)).thenReturn(Optional.of(PAYMENT_ID));
        when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        doThrow(new AuthorizationException("forbidden"))
                .when(authorizationService).validateClinicAccess(11L, ACTOR);

        assertThatThrownBy(() -> transition.apply(REFUND_ID, ACTOR))
                .isInstanceOf(AuthorizationException.class);

        verify(refundRepository, never()).findByIdForUpdate(REFUND_ID);
        verify(refundRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private Payment unauthorizedPayment() {
        var payment = mock(Payment.class);
        var clinic = mock(Clinic.class);
        when(payment.getClinic()).thenReturn(clinic);
        when(clinic.getId()).thenReturn(11L);
        return payment;
    }

    @FunctionalInterface
    private interface RefundTransition {
        Object apply(UUID refundId, SecurityActor actor);
    }
}
