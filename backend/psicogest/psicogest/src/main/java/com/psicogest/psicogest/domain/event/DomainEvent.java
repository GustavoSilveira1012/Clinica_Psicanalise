package com.psicogest.psicogest.domain.event;

import java.time.Instant;

/**
 * Interface base para eventos de domínio
 * 
 * Eventos de domínio representam fatos importantes que ocorreram
 * no sistema (ex: cobrança recebida, pacote ativado).
 * 
 * São processados por PolicyHandlers assincronamente via outbox.
 */
public interface DomainEvent {

    /**
     * ID do agregado que originou o evento
     * 
     * @return UUID como string
     */
    String aggregateId();

    /**
     * Tipo do evento
     * 
     * Exemplos: RECEIVABLE_PAYMENT_STARTED, RECEIVABLE_PAID, PATIENT_PACKAGE_ACTIVATED
     * 
     * @return Tipo do evento
     */
    String eventType();

    /**
     * Timestamp do evento
     * 
     * @return Instant de quando o evento ocorreu
     */
    Instant occurredAt();
}
