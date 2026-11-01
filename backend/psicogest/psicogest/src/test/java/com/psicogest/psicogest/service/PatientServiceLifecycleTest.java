package com.psicogest.psicogest.service;

import com.psicogest.psicogest.domain.lifecycle.LifecycleManager;
import com.psicogest.psicogest.dto.common.DeactivateDTO;
import com.psicogest.psicogest.model.entity.Patient;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.repository.AppointmentRepository;
import com.psicogest.psicogest.repository.PatientRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientServiceLifecycleTest {
    private final UUID organizationId = UUID.randomUUID();
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PatientService service = new PatientService(patients, mock(UserRepository.class),
            mock(PasswordEncoder.class), new LifecycleManager(), mock(AppointmentRepository.class),
            mock(PsychoanalystRepository.class), mock(TherapeuticRelationshipService.class));
    private Patient patient;

    @BeforeEach
    void setUp() {
        TenantContextHolder.set(new TenantContext(organizationId, 7L, null));
        patient = Patient.builder().id(1L).organizationId(organizationId)
                .user(User.builder().id(2L).name("Synthetic patient").email("synthetic@example.invalid")
                        .active(true).build()).active(true).build();
        when(patients.findByIdAndOrganizationId(1L, organizationId)).thenReturn(Optional.of(patient));
        when(patients.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void deactivationReportsPatientStateWhileAccountRemainsActive() {
        assertFalse(service.deactivate(1L, new DeactivateDTO("Synthetic lifecycle check")).active());
        assertFalse(patient.getActive());
        assertTrue(patient.getUser().getActive());
        verify(patients).save(patient);
    }

    @Test
    void profileReadReportsPreviouslyDeactivatedPatient() {
        patient.setActive(false);
        assertFalse(service.findById(1L).active());
    }

    @Test
    void reactivationReportsActivePatient() {
        patient.setActive(false);
        assertTrue(service.reactivate(1L).active());
        assertTrue(patient.getActive());
        verify(patients).save(patient);
    }
}
