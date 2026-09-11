package com.psicogest.psicogest.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Alocador de valores monetários para pacotes
 * 
 * Implementa alocação cumulativa determinística para evitar problema
 * de centavos perdidos na divisão de valores por quantidade.
 * 
 * Exemplo: R$ 100 / 3 sessões
 * - sessão 1 acumulado: 33,33
 * - sessão 2 acumulado: 66,67
 * - sessão 3 acumulado: 100,00
 * 
 * Os centavos nunca desaparecem pois usamos proporção cumulativa.
 */
@Component
public class PackageMonetaryAllocator {

    /**
     * Calcula o valor cumulativo consumido até uma quantidade específica
     * 
     * Fórmula: allocatedAmount * (consumedQuantity / totalQuantity)
     * Arredondamento: HALF_EVEN (banker's rounding)
     * Escala: 2 casas decimais
     * 
     * @param allocatedAmount Valor total alocado para o pacote
     * @param totalQuantity Quantidade total de itens (sessões)
     * @param consumedQuantity Quantidade consumida até agora
     * @return Valor cumulativo arredondado a 2 casas decimais
     * @throws IllegalArgumentException se consumedQuantity for inválida
     */
    public BigDecimal cumulativeValue(
            BigDecimal allocatedAmount,
            int totalQuantity,
            int consumedQuantity
    ) {

        // Validação: quantidade consumida deve estar entre 0 e total
        if (
                consumedQuantity < 0
                ||
                consumedQuantity > totalQuantity
        ) {

            throw new IllegalArgumentException(
                    "Quantidade consumida inválida: "
                            + consumedQuantity
                            + " (total: "
                            + totalQuantity
                            + ")"
            );
        }

        // Caso 1: nenhuma sessão consumida
        if (consumedQuantity == 0) {

            return BigDecimal.ZERO
                    .setScale(2);
        }

        // Caso 2: todas as sessões consumidas
        if (consumedQuantity == totalQuantity) {

            return allocatedAmount
                    .setScale(2);
        }

        // Caso 3: consumo parcial
        // Cálculo: allocatedAmount * (consumedQuantity / totalQuantity)
        return allocatedAmount

                .multiply(
                        BigDecimal.valueOf(
                                consumedQuantity
                        )
                )

                .divide(
                        BigDecimal.valueOf(
                                totalQuantity
                        ),
                        2,
                        RoundingMode.HALF_EVEN
                );
    }
}
