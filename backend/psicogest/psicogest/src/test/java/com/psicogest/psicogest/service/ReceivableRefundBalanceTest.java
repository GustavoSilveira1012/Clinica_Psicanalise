package com.psicogest.psicogest.service;

import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.Receivable;
import com.psicogest.psicogest.repository.AppointmentRepository;
import com.psicogest.psicogest.repository.ClinicRepository;
import com.psicogest.psicogest.repository.ClinicUserMembershipRepository;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.ReceivableRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.service.finance.FinanceAuthorizationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceivableRefundBalanceTest {

    @Test
    void reportsPaidAndOutstandingAmountsAfterConfirmedRefunds() {
        ReceivableRepository receivables = mock(ReceivableRepository.class);
        PatientRepository patients = mock(PatientRepository.class);
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        ClinicRepository clinics = mock(ClinicRepository.class);
        ClinicUserMembershipRepository memberships = mock(ClinicUserMembershipRepository.class);
        FinanceAuthorizationService authorization = mock(FinanceAuthorizationService.class);
        FinanceBalanceService balances = mock(FinanceBalanceService.class);
        UUID receivableId = UUID.randomUUID();
        Clinic clinic = Clinic.builder().id(33L).build();
        Patient patient = Patient.builder().id(71L).build();
        Receivable receivable = Receivable.builder()
                .id(receivableId)
                .clinic(clinic)
                .patient(patient)
                .description("Consulta")
                .grossAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .netAmount(new BigDecimal("100.00"))
                .dueDate(LocalDate.now().plusDays(1))
                .status(Receivable.ReceivableStatus.PARTIALLY_PAID)
                .createdAt(Instant.now())
                .build();
        SecurityActor actor = new SecurityActor(71L, null, null, null, null);
        when(receivables.findById(receivableId)).thenReturn(Optional.of(receivable));
        when(balances.allocatedAmount(receivableId)).thenReturn(new BigDecimal("60.00"));

        ReceivableService service = new ReceivableService(receivables, Clock.systemUTC(), patients,
                appointments, clinics, memberships, authorization, balances);
        var response = service.findById(receivableId, actor);

        assertThat(response.paidAmount()).isEqualByComparingTo("60.00");
        assertThat(response.outstandingAmount()).isEqualByComparingTo("40.00");
        verify(authorization).validateClinicAccess(33L, actor);
        verify(balances).allocatedAmount(receivableId);
    }
}
