package com.psicogest.psicogest.infrastructure.bank.parser;

/**
 * Parser desacoplado para extratos bancários
 * 
 * Cada fonte (OFX, Open Finance, CSV) implementa sua estratégia.
 * O domínio recebe ParsedBankStatement agnóstico.
 * 
 * Padrão Strategy: OFX → OfxBankStatementParser → ParsedBankStatement → BankReconciliationService
 * Futuro: Open Finance → OpenFinanceStatementAdapter → ParsedBankStatement → BankReconciliationService
 * 
 * Domínio não muda com novos formatos.
 */
public interface BankStatementParser {

    /**
     * Identifica a fonte do parser (OFX, Open Finance, CSV, etc)
     */
    BankStatementSource source();

    /**
     * Faz parse de um arquivo bruto
     * 
     * @param rawData bytes do arquivo
     * @return extrato parseado
     * @throws BankStatementParsingException se parse falhar
     */
    ParsedBankStatement parse(byte[] rawData);

    /**
     * Exception de parsing
     */
    class BankStatementParsingException extends RuntimeException {

        public BankStatementParsingException(String message) {
            super(message);
        }

        public BankStatementParsingException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}
