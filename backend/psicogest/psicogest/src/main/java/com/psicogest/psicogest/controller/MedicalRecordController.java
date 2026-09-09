package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.MedicalRecordCreateDTO;
import com.psicogest.psicogest.dto.MedicalRecordSummaryDTO;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.MedicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para operações de prontuários dentro de contexto de paciente
 * Listagem de metadados (sem content decriptografado)
 */
@Slf4j
@RestController
@RequestMapping("/patients/{patientId}/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final SecurityActorFactory securityActorFactory;

    public MedicalRecordController(
            MedicalRecordService medicalRecordService,
            SecurityActorFactory securityActorFactory
    ) {
        this.medicalRecordService = medicalRecordService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * POST /patients/{patientId}/medical-records
     * Criar novo prontuário médico
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
            @clinicalAuthorizationService.canWriteClinicalData(
                authentication,
                #patientId
            )
            """)
    public MedicalRecordSummaryDTO create(
            @PathVariable Long patientId,
            @Valid @RequestBody MedicalRecordCreateDTO dto,
            HttpServletRequest request,
            Authentication authentication
    ) {
        SecurityActor actor = securityActorFactory.from(authentication, request);
        return medicalRecordService.create(patientId, dto, actor);
    }

    /**
     * GET /patients/{patientId}/medical-records
     * Listar prontuários do paciente (metadados apenas, sem content)
     * Exfiltration detection: cada content requer GET separado
     */
    @GetMapping
    @PreAuthorize("""
            @clinicalAuthorizationService.canReadPatientClinicalData(
                authentication,
                #patientId
            )
            """)
    public List<MedicalRecordSummaryDTO> findByPatient(
            @PathVariable Long patientId,
            HttpServletRequest request,
            Authentication authentication
    ) {
        SecurityActor actor = securityActorFactory.from(authentication, request);
        return medicalRecordService.findByPatientId(patientId, actor);
    }
}
