package com.psicogest.psicogest.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.psicogest.psicogest.repository.*;

@Service
@Transactional(readOnly = true)
public class ClinicalTimelineService {

    private final AppointmentRepository appointmentRepository;

    private final MedicalRecordRepository medicalRecordRepository;

    private final MedicalRecordAddendumRepository addendumRepository;

    private final TherapeuticRelationshipRepository relationshipRepository;

    public ClinicalTimelineService(
            AppointmentRepository appointmentRepository,
            MedicalRecordRepository medicalRecordRepository,
            MedicalRecordAddendumRepository addendumRepository,
            TherapeuticRelationshipRepository relationshipRepository
    ) {

        this.appointmentRepository =
                appointmentRepository;

        this.medicalRecordRepository =
                medicalRecordRepository;

        this.addendumRepository =
                addendumRepository;

        this.relationshipRepository =
                relationshipRepository;
    }
}