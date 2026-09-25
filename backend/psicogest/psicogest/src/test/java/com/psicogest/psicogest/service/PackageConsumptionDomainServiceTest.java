package com.psicogest.psicogest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.psicogest.psicogest.model.entity.Appointment;
import com.psicogest.psicogest.model.entity.PackageConsumption;
import com.psicogest.psicogest.model.entity.PackagePlanItem;
import com.psicogest.psicogest.model.entity.PackagePlanVersion;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.PatientPackage;
import com.psicogest.psicogest.model.entity.SessionCreditEntry;
import com.psicogest.psicogest.model.enums.PackageConsumptionStatus;
import com.psicogest.psicogest.model.enums.SessionCreditDirection;
import com.psicogest.psicogest.model.enums.SessionCreditEntryType;
import com.psicogest.psicogest.repository.PackageConsumptionRepository;
import com.psicogest.psicogest.repository.PatientPackageRepository;
import com.psicogest.psicogest.repository.SessionCreditEntryRepository;

class PackageConsumptionDomainServiceTest {

    private final PackageConsumptionRepository consumptions = mock(PackageConsumptionRepository.class);
    private final SessionCreditEntryRepository ledger = mock(SessionCreditEntryRepository.class);
    private final PatientPackageRepository packages = mock(PatientPackageRepository.class);
    private final PackageConsumptionPolicy policy = mock(PackageConsumptionPolicy.class);
    private final Instant now = Instant.parse("2026-09-24T15:00:00Z");
    private final PackageConsumptionDomainService service = new PackageConsumptionDomainService(
            consumptions, ledger, packages, policy,
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void consumptionWritesDebitLedgerAndLinksDatabaseConsumption() {
        long patientId = 42L;
        UUID packageId = UUID.randomUUID();
        UUID packageItemId = UUID.randomUUID();
        UUID financialEntityId = UUID.randomUUID();
        Appointment appointment = mock(Appointment.class);
        Patient patient = mock(Patient.class);
        PatientPackage patientPackage = mock(PatientPackage.class);
        PackagePlanVersion version = mock(PackagePlanVersion.class);
        PackagePlanItem item = mock(PackagePlanItem.class);
        when(appointment.getId()).thenReturn(101L);
        when(appointment.getPatient()).thenReturn(patient);
        when(patient.getId()).thenReturn(patientId);
        when(policy.shouldConsume(eq(appointment), eq(null), eq(now))).thenReturn(true);
        when(packages.findActivePackagesForPatient(patientId, financialEntityId, now))
                .thenReturn(List.of(patientPackage));
        when(patientPackage.getId()).thenReturn(packageId);
        when(patientPackage.getPackagePlanVersion()).thenReturn(version);
        when(patientPackage.getPatient()).thenReturn(patient);
        when(version.getItems()).thenReturn(List.of(item));
        when(item.getId()).thenReturn(packageItemId);
        when(item.getQuantity()).thenReturn(3);
        when(consumptions.countActiveByPackageAndItem(packageId, packageItemId)).thenReturn(1L);
        when(ledger.saveAndFlush(any(SessionCreditEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.consumeSession(appointment, patientId, financialEntityId, packageItemId)).isTrue();

        ArgumentCaptor<SessionCreditEntry> debitCaptor = ArgumentCaptor.forClass(SessionCreditEntry.class);
        verify(ledger).saveAndFlush(debitCaptor.capture());
        SessionCreditEntry debit = debitCaptor.getValue();
        assertThat(debit.getDirection()).isEqualTo(SessionCreditDirection.DEBIT);
        assertThat(debit.getEntryType()).isEqualTo(SessionCreditEntryType.APPOINTMENT_CONSUMPTION);
        assertThat(debit.getSessionCount()).isEqualTo(1L);

        ArgumentCaptor<PackageConsumption> consumptionCaptor = ArgumentCaptor.forClass(PackageConsumption.class);
        verify(consumptions).save(consumptionCaptor.capture());
        assertThat(consumptionCaptor.getValue().getDebitEntry()).isSameAs(debit);
        assertThat(consumptionCaptor.getValue().getStatus()).isEqualTo(PackageConsumptionStatus.ACTIVE);
    }

    @Test
    void exhaustedPackageItemIsNotDebited() {
        long patientId = 42L;
        UUID packageId = UUID.randomUUID();
        UUID packageItemId = UUID.randomUUID();
        UUID financialEntityId = UUID.randomUUID();
        Appointment appointment = mock(Appointment.class);
        Patient patient = mock(Patient.class);
        PatientPackage patientPackage = mock(PatientPackage.class);
        PackagePlanVersion version = mock(PackagePlanVersion.class);
        PackagePlanItem item = mock(PackagePlanItem.class);
        when(appointment.getId()).thenReturn(101L);
        when(appointment.getPatient()).thenReturn(patient);
        when(patient.getId()).thenReturn(patientId);
        when(policy.shouldConsume(eq(appointment), eq(null), eq(now))).thenReturn(true);
        when(packages.findActivePackagesForPatient(patientId, financialEntityId, now))
                .thenReturn(List.of(patientPackage));
        when(patientPackage.getId()).thenReturn(packageId);
        when(patientPackage.getPackagePlanVersion()).thenReturn(version);
        when(version.getItems()).thenReturn(List.of(item));
        when(item.getId()).thenReturn(packageItemId);
        when(item.getQuantity()).thenReturn(1);
        when(consumptions.countActiveByPackageAndItem(packageId, packageItemId)).thenReturn(1L);

        assertThat(service.consumeSession(appointment, patientId, financialEntityId, packageItemId)).isFalse();

        verify(ledger, org.mockito.Mockito.never()).saveAndFlush(any(SessionCreditEntry.class));
        verify(consumptions, org.mockito.Mockito.never()).save(any(PackageConsumption.class));
    }
}
