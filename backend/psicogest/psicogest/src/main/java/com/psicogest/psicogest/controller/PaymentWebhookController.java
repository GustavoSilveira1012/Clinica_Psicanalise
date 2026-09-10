package com.psicogest.psicogest.controller;

import com.psicogest.psicogest.infrastructure.payment.provider.PaymentProviderType;
import com.psicogest.psicogest.infrastructure.payment.webhook.PaymentWebhookIngressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 18. Webhook Controller
 * 
 * Endpoint: POST /webhooks/payments/{provider}
 * 
 * Não tem JWT.
 * O gateway não possui usuário PsicoGest.
 * 
 * A confiança vem da assinatura, verificada pelo adapter
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/payments")
public class PaymentWebhookController {

    private final PaymentWebhookIngressService ingressService;

    public PaymentWebhookController(
            PaymentWebhookIngressService ingressService
    ) {

        this.ingressService = ingressService;
    }

    /**
     * Recebe webhook de pagamento
     * 
     * @param provider tipo de provider (STRIPE, MERCADO_PAGO, PAYPAL)
     * @param rawBody bytes brutos do webhook
     * @param headers headers HTTP
     * @param request requisição HTTP (para extrair IP)
     * @return 200 OK ou erro
     * @throws com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentWebhookAdapter.InvalidSignatureException
     */
    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(
            @PathVariable
            PaymentProviderType provider,

            @RequestBody
            byte[] rawBody,

            HttpHeaders headers,

            HttpServletRequest request
    ) throws com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentWebhookAdapter.InvalidSignatureException {

        String sourceIp = getClientIp(request);

        log.info(
                "Webhook recebido: provider={}, size={}, sourceIp={}",
                provider,
                rawBody.length,
                sourceIp
        );

        // Extrai headers em Map<String, String>
        Map<String, String> headerMap =
                extractHeaders(headers);

        // Processa webhook
        ingressService.receive(
                provider,
                rawBody,
                headerMap,
                sourceIp
        );

        // Retorna 200 OK
        // (webhook armazenado no inbox, processamento assíncrono depois)
        return ResponseEntity.ok().build();
    }

    /**
     * Extrai headers HTTP para Map
     */
    private Map<String, String> extractHeaders(
            HttpHeaders headers
    ) {

        Map<String, String> headerMap =
                new HashMap<>();

        headers.forEach(
                (key, values) -> {

                    if (values != null && !values.isEmpty()) {

                        headerMap.put(
                                key,
                                values.get(0)
                        );
                    }
                }
        );

        return headerMap;
    }

    /**
     * Extrai IP de origem da requisição
     * 
     * Tenta headers comuns:
     * - X-Forwarded-For (proxy)
     * - X-Real-IP (nginx)
     * - RemoteAddr (direto)
     */
    private String getClientIp(
            HttpServletRequest request
    ) {

        String xForwardedFor =
                request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null &&
                !xForwardedFor.isEmpty()) {

            // Pode ter múltiplos IPs, pega o primeiro
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp =
                request.getHeader("X-Real-IP");

        if (xRealIp != null &&
                !xRealIp.isEmpty()) {

            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
