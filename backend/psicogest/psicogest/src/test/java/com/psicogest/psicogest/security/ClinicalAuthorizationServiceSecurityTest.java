package com.psicogest.psicogest.security;

import com.psicogest.psicogest.repository.AddendumRepository;
import com.psicogest.psicogest.repository.AppointmentRepository;
import com.psicogest.psicogest.repository.MedicalRecordRepository;
import com.psicogest.psicogest.repository.MedicalRecordRevisionRepository;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.security.authorization.ClinicalAuthorizationService;
import com.psicogest.psicogest.security.authorization.SecurityContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalAuthorizationServiceSecurityTest {

    @Mock private MedicalRecordRepository records;
    @Mock private PsychoanalystRepository psychoanalysts;
    @Mock private AddendumRepository addendums;
    @Mock private MedicalRecordRevisionRepository revisions;
    @Mock private SecurityContextService context;
    @Mock private PatientRepository patients;
    @Mock private AppointmentRepository appointments;

    @Test
    void professionalWithRelationshipCanReadButUnrelatedProfessionalCannot() {
        ClinicalAuthorizationService service = service();
        UUID recordId = UUID.randomUUID();
        Authentication professionalA = auth(10L, "PSYCHOANALYST");
        Authentication professionalB = auth(20L, "PSYCHOANALYST");

        when(context.userId(professionalA)).thenReturn(Optional.of(10L));
        when(context.userId(professionalB)).thenReturn(Optional.of(20L));
        when(context.hasRole(any(), eq("PSYCHOANALYST"))).thenReturn(true);
        when(psychoanalysts.findByUserId(10L)).thenReturn(Optional.of(
                com.psicogest.psicogest.model.entity.Psychoanalyst.builder().id(100L).build()));
        when(psychoanalysts.findByUserId(20L)).thenReturn(Optional.of(
                com.psicogest.psicogest.model.entity.Psychoanalyst.builder().id(200L).build()));
        when(records.existsReadableBy(recordId, 100L)).thenReturn(true);
        when(records.existsReadableBy(recordId, 200L)).thenReturn(false);

        assertThat(service.canReadMedicalRecord(professionalA, recordId)).isTrue();
        assertThat(service.canReadMedicalRecord(professionalB, recordId)).isFalse();
    }

    @Test
    void clinicAdminAndSystemAdminCannotReadMedicalRecordByDefault() {
        ClinicalAuthorizationService service = service();
        UUID recordId = UUID.randomUUID();

        Authentication clinicAdmin = auth(30L, "CLINIC_ADMIN");
        Authentication systemAdmin = auth(40L, "SYSTEM_ADMIN");
        when(context.hasRole(clinicAdmin, "PSYCHOANALYST")).thenReturn(false);
        when(context.hasRole(systemAdmin, "PSYCHOANALYST")).thenReturn(false);

        assertThat(service.canReadMedicalRecord(clinicAdmin, recordId)).isFalse();
        assertThat(service.canReadMedicalRecord(systemAdmin, recordId)).isFalse();
        verifyNoInteractions(records, psychoanalysts);
    }

    @Test
    void patientProfileAuthorizationDoesNotGrantClinicalRecordAccess() {
        ClinicalAuthorizationService service = service();
        Authentication clinicAdmin = auth(30L, "CLINIC_ADMIN");
        UUID recordId = UUID.randomUUID();
        when(context.hasRole(clinicAdmin, "PSYCHOANALYST")).thenReturn(false);

        assertThat(service.canReadMedicalRecord(clinicAdmin, recordId)).isFalse();
    }

    private ClinicalAuthorizationService service() {
        return new ClinicalAuthorizationService(records, psychoanalysts, addendums, revisions,
                context, patients, appointments);
    }

    private Authentication auth(Long id, String role) {
        return new TestingAuthenticationToken(String.valueOf(id), "n/a",
                new SimpleGrantedAuthority("ROLE_" + role));
    }
}
