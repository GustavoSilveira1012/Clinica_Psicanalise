package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.AddendumCreateDTO;
import com.psicogest.psicogest.dto.AddendumResponseDTO;
import com.psicogest.psicogest.dto.AddendumSummaryDTO;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.AddendumService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class AddendumController {

    private final AddendumService service;
    private final SecurityActorFactory securityActorFactory;

    public AddendumController(
            AddendumService service,
            SecurityActorFactory securityActorFactory
    ) {
        this.service = service;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * 29. POST /medical-records/{recordId}/addendums
     * Criar novo addendum em prontuário finalizado
     * 
     * @PreAuthorize: canAddAddendum (autorização baseada em recordId)
     * @ResponseStatus: CREATED (201)
     * 
     * Fluxo:
     * 1. Validar JWT + MFA + Session
     * 2. Verificar canAddAddendum(recordId)
     * 3. Service.create() -> cifra content, persiste, audita
     * 4. Retorna AddendumResponseDTO
     */
    @PostMapping("/medical-records/{recordId}/addendums")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
            @clinicalAuthorization.canAddAddendum(
                authentication,
                #recordId
            )
            """)
    public AddendumResponseDTO create(
            @PathVariable UUID recordId,
            @Valid @RequestBody AddendumCreateDTO dto,
            Authentication authentication,
            HttpServletRequest request
    ) {
        log.info("POST /medical-records/{}/addendums - criar", recordId);

        return service.create(
                recordId,
                dto,
                securityActorFactory.from(authentication, request)
        );
    }

    /**
     * 30. GET /medical-record-addendums/{addendumId}
     * Ler addendum completo com content descriptografado
     * 
     * @PreAuthorize: canReadMedicalRecordAddendum (autorização baseada em addendumId)
     * 
     * Fluxo:
     * 1. Validar JWT + MFA + Session
     * 2. Verificar canReadMedicalRecordAddendum(addendumId)
     * 3. Service.findById() -> descriptografa, audita, detecta exfiltração
     * 4. Retorna AddendumResponseDTO
     */
    @GetMapping("/medical-record-addendums/{addendumId}")
    @PreAuthorize("""
            @clinicalAuthorization.canReadMedicalRecordAddendum(
                authentication,
                #addendumId
            )
            """)
    public AddendumResponseDTO findById(
            @PathVariable UUID addendumId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        log.info("GET /medical-record-addendums/{} - ler individual", addendumId);

        return service.findById(
                addendumId,
                securityActorFactory.from(authentication, request)
        );
    }

    /**
     * 31. GET /medical-records/{recordId}/addendums
     * Listar addendums de prontuário (timeline com metadados apenas)
     * 
     * Retorna List<AddendumSummaryDTO> sem content para evitar múltiplas descriptografias.
     * Usuário clica em addendum → GET /medical-record-addendums/{id} para ler completo.
     * 
     * Exemplo resposta:
     * [
     *   {
     *     "id": "...",
     *     "authorName": "Dr. João",
     *     "reason": "CORRECTION",
     *     "createdAt": "2026-09-09T10:30:00Z"
     *   }
     * ]
     * 
     * Fluxo:
     * 1. Validar JWT + MFA + Session
     * 2. Service.findByMedicalRecord() -> lista sem descriptografar
     * 3. Retorna List<AddendumSummaryDTO>
     */
    @GetMapping("/medical-records/{recordId}/addendums")
    public List<AddendumSummaryDTO> findByMedicalRecord(
            @PathVariable UUID recordId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        log.info("GET /medical-records/{}/addendums - listar timeline", recordId);

        return service.findByMedicalRecord(
                recordId,
                securityActorFactory.from(authentication, request)
        );
    }
}
