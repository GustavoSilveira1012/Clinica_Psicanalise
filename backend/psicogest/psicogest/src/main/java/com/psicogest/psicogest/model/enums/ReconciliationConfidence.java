package com.psicogest.psicogest.model.enums;

/**
 * Nível de confiança de uma sugestão de reconciliação
 * 
 * HIGH: pode ser auto-matcheado
 * MEDIUM: mostrar ao usuário para confirmar
 * LOW: apenas informativo
 */
public enum ReconciliationConfidence {

    HIGH,

    MEDIUM,

    LOW
}
