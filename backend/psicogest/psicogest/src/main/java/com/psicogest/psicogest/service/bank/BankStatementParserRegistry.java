package com.psicogest.psicogest.service.bank;

import java.util.List;

import org.springframework.stereotype.Component;

import com.psicogest.psicogest.infrastructure.bank.parser.BankStatementParser;
import com.psicogest.psicogest.infrastructure.bank.parser.OfxBankStatementParser;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry de parsers de extrato bancário
 * 
 * Detecta formato por conteúdo (não por nome)
 */
@Slf4j
@Component
public class BankStatementParserRegistry {

    private final List<BankStatementParser> parsers;

    public BankStatementParserRegistry(
            List<BankStatementParser> parsers
    ) {
        this.parsers = parsers;
        log.info("Parsers registrados: {}", parsers.size());
    }

    /**
     * Detecta parser apropriado por conteúdo
     * 
     * @param rawData bytes do arquivo
     * @return parser detectado ou null
     */
    public BankStatementParser detectParser(byte[] rawData) {

        for (BankStatementParser parser : parsers) {

            try {

                // Tentar parse com este parser
                parser.parse(rawData);

                log.debug(
                        "Parser detectado: {}",
                        parser.source()
                );

                return parser;

            } catch (Exception e) {

                // Continuar com próximo parser
                log.debug(
                        "Parser {} não aceitou: {}",
                        parser.source(),
                        e.getMessage()
                );
            }
        }

        return null;
    }
}
