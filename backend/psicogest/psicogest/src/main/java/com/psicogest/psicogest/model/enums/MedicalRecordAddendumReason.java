package com.psicogest.psicogest.model.enums;

/**
 * Enum para código de motivo de addendum
 * 
 * Não contém informação clínica sensível - apenas categoriza o tipo
 * A explicação clínica fica no conteúdo cifrado do addendum
 */
public enum MedicalRecordAddendumReason {

    /**
     * Esclarecimento de informação anterior
     */
    CLARIFICATION("Esclarecimento"),

    /**
     * Correção de erro registrado
     */
    CORRECTION("Correção"),

    /**
     * Complementação/adição de informação
     */
    COMPLEMENT("Complementação"),

    /**
     * Outro motivo (descrição no content cifrado)
     */
    OTHER("Outro");

    private final String label;

    MedicalRecordAddendumReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
