package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.dto.PaymentCollectionCreateDTO;
import com.psicogest.psicogest.dto.PaymentCollectionResponseDTO;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityActorFactory;
import com.psicogest.psicogest.service.PaymentCollectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentCollectionController {

    private final PaymentCollectionService collectionService;
    private final SecurityActorFactory securityActorFactory;

    public PaymentCollectionController(
            PaymentCollectionService collectionService,
            SecurityActorFactory securityActorFactory
    ) {
        this.collectionService = collectionService;
        this.securityActorFactory = securityActorFactory;
    }

    @PostMapping("/collect")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentCollectionResponseDTO collect(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentCollectionCreateDTO dto,
            Authentication authentication,
            HttpServletRequest request
    ) {
        SecurityActor actor = securityActorFactory.from(authentication, request);
        return collectionService.collect(idempotencyKey, dto, actor);
    }
}
