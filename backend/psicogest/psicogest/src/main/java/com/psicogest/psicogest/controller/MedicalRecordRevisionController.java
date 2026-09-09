package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordRevisionResponseDTO;
import com.psicogest.psicogest.dto.medicalrecord.MedicalRecordRevisionSummaryDTO;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.MedicalRecordRevisionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 23. Controller para revisões de prontuários
 * 
 * Endpoints:
 * - GET /medical-records/{recordId}/revisions (listagem, metadados)
 * - GET /medical-record-revisions/{revisionId} (leitura completa)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class MedicalRecordRevisionController {

    private final MedicalRecordRevisionService revisionService;
    private final SecurityActorFactory securityActorFactory;

    public MedicalRecordRevisionController(
            MedicalRecordRevisionService revisionService,
            SecurityActorFactory securityActorFactory
    ) {
        this.revisionService = revisionService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * 23. Listagem de revisões
     * 
     * GET /medical-records/{recordId}/revisions
     * 
     * @PreAuthorize: canReadMedicalRecord (pode ler o prontuário atual)
     * 
     * Retorna:
     * [
     *   {
     *     "id": "...",
     *     "revisionNumber": 4,
     *     "authorPsychoanalystId": 10,
     *     "authorName": "Dr. João",
     *     "createdAt": "2026-09-09T18:30:00Z"
     *   },
     *   {
     *     "id": "...",
     *     "revisionNumber": 3,
     *     "authorPsychoanalystId": 10,
     *     "authorName": "Dr. João",
     *     "createdAt": "2026-09-09T18:25:00Z"
     *   }
     * ]
     * 
     * Fluxo:
     * 1. Validar JWT + MFA + Session
     * 2. Verificar canReadMedicalRecord(recordId)
     * 3. Service.findByMedicalRecord() → lista metadados (sem content)
     * 4. Retorna List<MedicalRecordRevisionSummaryDTO>
     */
    @GetMapping("/medical-records/{recordId}/revisions")
    @PreAuthorize("""
            @clinicalAuthorization.canReadMedicalRecord(
                authentication,
                #recordId
            )
            """)
    public List<MedicalRecordRevisionSummaryDTO> findRevisions(
            @PathVariable UUID recordId
    ) {
        log.info("GET /medical-records/{}/revisions - listar revisões", recordId);

        return revisionService.findByMedicalRecord(recordId);
    }

    /**
     * 23. Leitura de revisão completa
     * 
     * GET /medical-record-revisions/{revisionId}
     * 
     * @PreAuthorize: canReadMedicalRecordRevision (autor original apenas)
     * 
     * Retorna revisão com conteúdo descriptografado:
     * {
     *   "id": "...",
     *   "medicalRecordId": "...",
     *   "revisionNumber": 3,
     *   "authorPsychoanalystId": 10,
     *   "authorName": "Dr. João",
     *   "content": "Conteúdo cifrado descriptografado",
     *   "createdAt": "2026-09-09T18:25:00Z"
     * }
     * 
     * Fluxo:
     * 1. Validar JWT + MFA + Session
     * 2. Verificar canReadMedicalRecordRevision(revisionId)
     *    → APENAS se usuário é autor original
     * 3. Service.findRevision() → descriptografa, audita, detecta exfiltração
     * 4. Retorna MedicalRecordRevisionResponseDTO (com content)
     */
    @GetMapping("/medical-record-revisions/{revisionId}")
    @PreAuthorize("""
            @clinicalAuthorization.canReadMedicalRecordRevision(
                authentication,
                #revisionId
            )
            """)
    public MedicalRecordRevisionResponseDTO findRevision(
            @PathVariable UUID revisionId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        log.info("GET /medical-record-revisions/{} - ler revisão completa", revisionId);

        return revisionService.findRevision(
                revisionId,
                securityActorFactory.from(authentication, request)
        );
    }
}
