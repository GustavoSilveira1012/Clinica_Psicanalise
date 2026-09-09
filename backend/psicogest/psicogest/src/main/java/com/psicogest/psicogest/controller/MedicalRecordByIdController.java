package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.MedicalRecordResponseDTO;
import com.psicogest.psicogest.dto.MedicalRecordUpdateDTO;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.MedicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller para operações de prontuários por ID
 * Leitura completa com content decriptografado
 */
@Slf4j
@RestController
@RequestMapping("/medical-records")
public class MedicalRecordByIdController {

    private final MedicalRecordService medicalRecordService;
    private final SecurityActorFactory securityActorFactory;

    public MedicalRecordByIdController(
            MedicalRecordService medicalRecordService,
            SecurityActorFactory securityActorFactory
    ) {
        this.medicalRecordService = medicalRecordService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * GET /medical-records/{recordId}
     * Ler prontuário médico completo com content
     * Requer autorização: autor ou vínculo ACTIVE/SUSPENDED
     */
    @GetMapping("/{recordId}")
    @PreAuthorize("""
            @clinicalAuthorizationService.canReadMedicalRecord(
                authentication,
                #recordId
            )
            """)
    public MedicalRecordResponseDTO findById(
            @PathVariable UUID recordId,
            HttpServletRequest request,
            Authentication authentication
    ) {
        SecurityActor actor = securityActorFactory.from(authentication, request);
        return medicalRecordService.findById(recordId, actor);
    }

    /**
     * PUT /medical-records/{recordId}
     * Atualizar prontuário (somente autor, apenas DRAFT)
     */
    @PutMapping("/{recordId}")
    @PreAuthorize("""
            @clinicalAuthorizationService.canEditMedicalRecord(
                authentication,
                #recordId
            )
            """)
    public MedicalRecordResponseDTO update(
            @PathVariable UUID recordId,
            @Valid @RequestBody MedicalRecordUpdateDTO dto,
            HttpServletRequest request,
            Authentication authentication
    ) {
        SecurityActor actor = securityActorFactory.from(authentication, request);
        return medicalRecordService.update(recordId, dto, actor);
    }

    /**
     * PATCH /medical-records/{recordId}/finalize
     * Finalizar prontuário (tornar imutável)
     * Somente autor pode finalizar
     */
    @PatchMapping("/{recordId}/finalize")
    @PreAuthorize("""
            @clinicalAuthorizationService.canEditMedicalRecord(
                authentication,
                #recordId
            )
            """)
    public MedicalRecordResponseDTO finalizeRecord(
            @PathVariable UUID recordId,
            HttpServletRequest request,
            Authentication authentication
    ) {
        SecurityActor actor = securityActorFactory.from(authentication, request);
        return medicalRecordService.finalizeRecord(recordId, actor);
    }
}
