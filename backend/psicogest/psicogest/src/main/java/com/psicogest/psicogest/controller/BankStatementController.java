package com.psicogest.psicogest.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.bank.BankStatementImportService;
import com.psicogest.psicogest.service.bank.BankStatementImportService.BankStatementImportResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller para importação de extratos bancários
 * 
 * Endpoint: POST /bank-accounts/{bankAccountId}/statements
 * Content-Type: multipart/form-data
 */
@Slf4j
@RestController
@RequestMapping("/bank-accounts/{bankAccountId}/statements")
public class BankStatementController {

    private final BankStatementImportService importService;

    private final SecurityActorFactory securityActorFactory;

    public BankStatementController(
            BankStatementImportService importService,
            SecurityActorFactory securityActorFactory
    ) {
        this.importService = importService;
        this.securityActorFactory = securityActorFactory;
    }

    /**
     * Importa extrato bancário
     * 
     * @param bankAccountId ID da conta
     * @param file arquivo de extrato (OFX, CSV, etc)
     * @param authentication contexto de segurança
     * @param request requisição HTTP
     * @return resultado da importação
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BankStatementImportResult> importStatement(
            @PathVariable
            UUID bankAccountId,

            @RequestParam("file")
            MultipartFile file,

            Authentication authentication,

            HttpServletRequest request
    ) throws Exception {

        SecurityActor actor =
                securityActorFactory
                        .from(authentication, request);

        log.info(
                "Importando extrato bancário: " +
                        "bankAccountId={}, filename={}, size={}",
                bankAccountId,
                file.getOriginalFilename(),
                file.getSize()
        );

        // Obter bytes do arquivo
        byte[] rawData = file.getBytes();

        // Importar
        BankStatementImportResult result =
                importService.importStatement(
                        bankAccountId,
                        rawData,
                        actor
                );

        return ResponseEntity.ok(result);
    }
}
