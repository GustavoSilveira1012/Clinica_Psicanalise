package com.psicogest.psicogest.service.fiscal;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.exception.FiscalValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Validador de XML fiscal contra XSD
 * 
 * Valida DPS XML conforme layout ABRASF padrão
 */
@Slf4j
@Service
public class FiscalXmlValidator {

    /**
     * Valida XML fiscal contra XSD
     * 
     * @param xml Conteúdo XML em bytes
     * @param layoutVersion Versão do layout (2.02, 2.03, etc)
     * @throws FiscalValidationException se inválido
     */
    public void validate(
            byte[] xml,
            String layoutVersion
    ) {

        if (xml == null || xml.length == 0) {
            throw new FiscalValidationException(
                    "XML vazio"
            );
        }

        log.info(
                "Validando XML fiscal: layoutVersion={}, size={}",
                layoutVersion,
                xml.length
        );

        // TODO: Implementar validação XSD
        // 1. Carregar XSD conforme layoutVersion
        // 2. Validar XML contra XSD
        // 3. Lançar FiscalValidationException se inválido
    }
}
