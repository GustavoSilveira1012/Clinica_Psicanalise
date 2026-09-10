package com.psicogest.psicogest.infrastructure.bank.parser;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Transação bancária parseada de um extrato
 * 
 * Record agnóstico que representa transação de qualquer fonte
 * (OFX, Open Finance, CSV, etc)
 */
public record ParsedBankTransaction(

        /**
         * ID externo do banco (ex: OFX FITID)
         * Pode ser null se não disponível
         */
        String externalId,

        /**
         * Crédito ou débito
         */
        BankTransactionDirection direction,

        /**
         * Valor da transação
         */
        BigDecimal amount,

        /**
         * Código da moeda (BRL, USD, etc)
         */
        String currency,

        /**
         * Data de lançamento
         */
        LocalDate bookingDate,

        /**
         * Timestamp exato (se disponível)
         */
        Instant postedAt,

        /**
         * Descrição (pode conter PII)
         * Será criptografada no armazenamento
         */
        String description,

        /**
         * Referência adicional (código de transação, etc)
         */
        String reference

) {
}
