package com.psicogest.psicogest.infrastructure.bank.parser;

import java.util.List;

/**
 * Extrato bancário parseado de um arquivo
 * 
 * Record agnóstico que representa extrato de qualquer fonte
 */
public record ParsedBankStatement(

        /**
         * Código do banco (3 dígitos para Brasil)
         * Ex: 001 (Banco do Brasil), 033 (Banco Santander)
         */
        String bankCode,

        /**
         * Número da agência
         */
        String branch,

        /**
         * Referência da conta (pode ser mascarada por segurança)
         * Ex: ****0123 em vez de 0001234567
         */
        String accountReference,

        /**
         * Transações do extrato
         */
        List<ParsedBankTransaction> transactions

) {
}
