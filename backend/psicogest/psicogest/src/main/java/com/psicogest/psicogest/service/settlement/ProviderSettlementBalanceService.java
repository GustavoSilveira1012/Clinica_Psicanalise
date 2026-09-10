package com.psicogest.psicogest.service.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.model.enums.ProviderSettlementDirection;
import com.psicogest.psicogest.model.vo.SettlementNet;
import com.psicogest.psicogest.repository.ProviderSettlementItemRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de cálculo de saldo líquido de repasse
 * 
 * Calcula:
 * - Saldo assinado (créditos - débitos)
 * - Normaliza (arredonda para 2 casas decimais)
 * - Determina direção (CREDIT_TO_FINANCIAL_ENTITY ou DEBIT_FROM_FINANCIAL_ENTITY)
 */
@Slf4j
@Service
public class ProviderSettlementBalanceService {

    private final ProviderSettlementItemRepository itemRepository;

    public ProviderSettlementBalanceService(
            ProviderSettlementItemRepository itemRepository
    ) {
        this.itemRepository = itemRepository;
    }

    /**
     * Calcula o saldo líquido de um repasse
     * 
     * @param settlementId ID do repasse
     * @return SettlementNet com direção e valor
     */
    public SettlementNet calculateNet(UUID settlementId) {

        // Calcular saldo assinado (créditos - débitos)
        BigDecimal signed =
                itemRepository
                        .calculateSignedNet(
                                settlementId
                        )
                        // Normalizar: arredondar para 2 casas decimais
                        .setScale(2, java.math.RoundingMode.HALF_UP);

        // Determinar direção
        if (signed.signum() >= 0) {

            return new SettlementNet(
                    ProviderSettlementDirection
                            .CREDIT_TO_FINANCIAL_ENTITY,

                    signed
            );
        }

        return new SettlementNet(
                ProviderSettlementDirection
                        .DEBIT_FROM_FINANCIAL_ENTITY,

                signed.abs()
        );
    }
}
