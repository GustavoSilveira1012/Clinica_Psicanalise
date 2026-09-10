package com.psicogest.psicogest.domain.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 20. Regras de cálculo monetário
 * 
 * Centraliza tratamento de valores (normalização, arredondamento)
 * 
 * Nota: Regras fiscais específicas conforme documento/tributo
 * podem variar. Essa classe fornece padrão HALF_EVEN para casos gerais.
 * Fiscal domain pode sobrescrever se necessário.
 */
public final class MoneyRules {

    private MoneyRules() {
        // Utility class
    }

    /**
     * Normaliza valor monetário para 2 casas decimais
     * com arredondamento HALF_EVEN (padrão bancário)
     * 
     * @param value valor a normalizar
     * @return valor normalizado com 2 casas decimais
     * @throws IllegalArgumentException se valor for null
     */
    public static BigDecimal normalize(
            BigDecimal value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Valor monetário obrigatório"
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_EVEN
        );
    }
}
