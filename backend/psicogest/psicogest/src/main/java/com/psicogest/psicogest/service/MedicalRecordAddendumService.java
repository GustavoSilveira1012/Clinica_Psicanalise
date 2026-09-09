package com.psicogest.psicogest.service;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.infrastructure.exfiltration.ClinicalAccessDetector;
import com.psicogest.psicogest.repository.AddendumRepository;
import com.psicogest.psicogest.repository.MedicalRecordRepository;
import com.psicogest.psicogest.repository.PsychoanalystRepository;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.crypto.ClinicalEncryptionService;

@Service
public class MedicalRecordAddendumService {

    private final MedicalRecordRepository
            medicalRecordRepository;

    private final AddendumRepository
            addendumRepository;

    private final PsychoanalystRepository
            psychoanalystRepository;

    private final ClinicalEncryptionService
            encryptionService;

    private final AuditService
            auditService;

    private final ClinicalAccessDetector
            accessDetector;

    public MedicalRecordAddendumService(
            MedicalRecordRepository medicalRecordRepository,
            AddendumRepository addendumRepository,
            PsychoanalystRepository psychoanalystRepository,
            ClinicalEncryptionService encryptionService,
            AuditService auditService,
            ClinicalAccessDetector accessDetector
    ) {

        this.medicalRecordRepository =
                medicalRecordRepository;

        this.addendumRepository =
                addendumRepository;

        this.psychoanalystRepository =
                psychoanalystRepository;

        this.encryptionService =
                encryptionService;

        this.auditService =
                auditService;

        this.accessDetector =
                accessDetector;
    }
}