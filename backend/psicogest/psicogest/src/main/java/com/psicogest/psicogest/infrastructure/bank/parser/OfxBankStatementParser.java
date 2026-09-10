package com.psicogest.psicogest.infrastructure.bank.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 11. Parser OFX (Open Financial Exchange)
 * 
 * OFX é um formato textual para intercâmbio de dados financeiros
 * Usado por bancos brasileiros para download de extratos
 * 
 * Formato: SGML (tipo XML antigo, sem tags fechadas)
 * Exemplo:
 * <OFX>
 * <SIGNONMSGSRSV1>
 * ...
 * <STMTTRS>
 * <CURDEF>BRL
 * <BANKTRANLIST>
 * <STMTTRN>
 * <TRNTYPE>DEBIT
 * <DTPOSTED>20260909
 * <TRNAMT>-100.00
 * <FITID>123456789
 * <NAME>PIX ENVIADO
 * </STMTTRN>
 * ...
 * </BANKTRANLIST>
 * </STMTTRS>
 * </OFX>
 * 
 * TODO: Integração com Open4j ou similar
 * Por enquanto: stub com detecção de formato
 */
@Slf4j
@Component
public class OfxBankStatementParser implements BankStatementParser {

    @Override
    public BankStatementSource source() {
        return BankStatementSource.OFX;
    }

    @Override
    public ParsedBankStatement parse(byte[] rawData) {

        String content = new String(
                rawData,
                StandardCharsets.US_ASCII
        );

        // Validar formato OFX
        if (!content.startsWith("OFXHEADER:") &&
                !content.contains("<OFX>")) {

            throw new BankStatementParser.BankStatementParsingException(
                    "Não é um arquivo OFX válido"
            );
        }

        log.info(
                "Parseando OFX: {} bytes",
                rawData.length
        );

        // TODO: Implementar parse completo usando open4j
        // Por enquanto retorna stub

        return new ParsedBankStatement(
                "001",  // Placeholder: código do banco
                "0001", // Placeholder: agência
                "****1234", // Placeholder: referência da conta
                new ArrayList<>() // Placeholder: lista vazia de transações
        );
    }

    /**
     * Detecta se é OFX por conteúdo (não por nome de arquivo)
     */
    public static boolean isOfxFormat(byte[] rawData) {

        if (rawData.length < 10) {
            return false;
        }

        try {

            String header = new String(
                    java.util.Arrays.copyOf(
                            rawData,
                            Math.min(15, rawData.length)
                    ),
                    StandardCharsets.US_ASCII
            );

            return header.startsWith("OFXHEADER:");

        } catch (Exception e) {

            return false;
        }
    }
}
