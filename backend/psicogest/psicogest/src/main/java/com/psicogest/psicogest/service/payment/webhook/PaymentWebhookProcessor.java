package com.psicogest.psicogest.service.payment.webhook;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.psicogest.psicogest.infrastructure.payment.provider.webhook.PaymentWebhookAdapterRegistry;
import com.psicogest.psicogest.repository.PaymentWebhookInboxRepository;
import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.service.PaymentService;
import com.psicogest.psicogest.service.RefundService;

@Service
public class PaymentWebhookProcessor {

    private final PaymentWebhookInboxRepository inboxRepository;

    private final PaymentWebhookAdapterRegistry adapterRegistry;

    private final PaymentService paymentService;

    private final RefundService refundService;

    private final ApplicationEncryptionService encryptionService;

    private final Clock clock;

    public PaymentWebhookProcessor(
            PaymentWebhookInboxRepository inboxRepository,
            PaymentWebhookAdapterRegistry adapterRegistry,
            PaymentService paymentService,
            RefundService refundService,
            ApplicationEncryptionService encryptionService,
            Clock clock
    ) {
        this.inboxRepository = inboxRepository;
        this.adapterRegistry = adapterRegistry;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.encryptionService = encryptionService;
        this.clock = clock;
    }

    // ...
}