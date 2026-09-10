package com.psicogest.psicogest.infrastructure.bank.parser;

/**
 * Identificação da fonte do extrato bancário
 */
public enum BankStatementSource {
    /**
     * OFX (Open Financial Exchange)
     */
    OFX,

    /**
     * Open Finance (futuro)
     */
    OPEN_FINANCE,

    /**
     * CSV genérico
     */
    CSV,

    /**
     * Outro formato
     */
    OTHER
}
