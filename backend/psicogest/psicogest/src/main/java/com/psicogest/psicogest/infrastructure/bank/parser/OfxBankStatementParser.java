package com.psicogest.psicogest.infrastructure.bank.parser;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * O parser aceita OFX 1.x (SGML) e OFX 2.x (XML) sem habilitar
 * resolução de entidades externas. O conteúdo é limitado e os campos
 * usados no domínio são extraídos por tags conhecidas.
 */
@Slf4j
@Component
public class OfxBankStatementParser implements BankStatementParser {

    private static final int MAX_SIZE = 50 * 1024 * 1024;
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "(?is)<STMTTRN\\b[^>]*>(.*?)(?=</?STMTTRN\\b|</?BANKTRANLIST\\b|$)");
    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public BankStatementSource source() {
        return BankStatementSource.OFX;
    }

    @Override
    public ParsedBankStatement parse(byte[] rawData) {

        if (rawData == null || rawData.length == 0) {
            throw new BankStatementParser.BankStatementParsingException("Arquivo OFX vazio");
        }
        if (rawData.length > MAX_SIZE) {
            throw new BankStatementParser.BankStatementParsingException("Arquivo OFX excede o limite permitido");
        }

        String content = decode(rawData).replace("\uFEFF", "");

        // Validar formato OFX
        if (!content.stripLeading().toUpperCase(Locale.ROOT).startsWith("OFXHEADER:") &&
                !Pattern.compile("(?is)<OFX\\b").matcher(content).find()) {

            throw new BankStatementParser.BankStatementParsingException(
                    "Não é um arquivo OFX válido"
            );
        }

        log.info(
                "Parseando OFX: {} bytes",
                rawData.length
        );

        String bankCode = firstTag(content, "BANKID");
        String branch = firstTag(content, "BRANCHID");
        String accountId = firstTag(content, "ACCTID");
        if (bankCode == null || branch == null || accountId == null) {
            throw new BankStatementParser.BankStatementParsingException(
                    "OFX sem banco, agência ou conta");
        }

        Matcher transactionMatcher = TRANSACTION_PATTERN.matcher(content);
        List<ParsedBankTransaction> transactions = new ArrayList<>();
        String currency = firstTag(content, "CURDEF");
        while (transactionMatcher.find()) {
            String block = transactionMatcher.group(1);
            transactions.add(parseTransaction(block, currency));
        }
        if (transactions.isEmpty() && Pattern.compile("(?is)<STMTTRN\\b").matcher(content).find()) {
            throw new BankStatementParser.BankStatementParsingException(
                    "OFX contém transação sem bloco válido");
        }

        return new ParsedBankStatement(
                clean(bankCode),
                clean(branch),
                maskAccount(clean(accountId)),
                List.copyOf(transactions)
        );
    }

    private ParsedBankTransaction parseTransaction(String block, String statementCurrency) {
        String type = requiredTag(block, "TRNTYPE");
        String rawDate = requiredTag(block, "DTPOSTED");
        String rawAmount = requiredTag(block, "TRNAMT");
        LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(rawDate.substring(0, 8), OFX_DATE);
        } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
            throw new BankStatementParser.BankStatementParsingException("Data de transação OFX inválida", exception);
        }

        BigDecimal signedAmount;
        try {
            signedAmount = parseAmount(rawAmount);
        } catch (NumberFormatException exception) {
            throw new BankStatementParser.BankStatementParsingException("Valor de transação OFX inválido", exception);
        }
        if (signedAmount.signum() == 0) {
            throw new BankStatementParser.BankStatementParsingException("Transação OFX com valor zero");
        }

        BankTransactionDirection direction = signedAmount.signum() < 0
                || "DEBIT".equalsIgnoreCase(type)
                ? BankTransactionDirection.DEBIT
                : BankTransactionDirection.CREDIT;
        String description = join(firstTag(block, "NAME"), firstTag(block, "MEMO"));
        String reference = join(firstTag(block, "CHECKNUM"), firstTag(block, "REFNUM"));
        String externalId = firstTag(block, "FITID");

        return new ParsedBankTransaction(
                clean(externalId),
                direction,
                signedAmount.abs().setScale(2),
                firstTag(block, "CURSYM") == null
                        ? (statementCurrency == null ? "BRL" : statementCurrency).toUpperCase(Locale.ROOT)
                        : clean(firstTag(block, "CURSYM")).toUpperCase(Locale.ROOT),
                bookingDate,
                parsePostedAt(rawDate, bookingDate),
                limit(description, 500),
                limit(reference, 255)
        );
    }

    private String decode(byte[] rawData) {
        String ascii = new String(rawData, StandardCharsets.US_ASCII);
        Matcher charsetMatcher = Pattern.compile("(?im)^CHARSET\\s*:\\s*([A-Z0-9_-]+)").matcher(ascii);
        if (charsetMatcher.find()) {
            try {
                return new String(rawData, Charset.forName(charsetMatcher.group(1).trim()));
            } catch (Exception ignored) {
                // Mantém US-ASCII como fallback seguro para headers OFX.
            }
        }
        return ascii;
    }

    private String requiredTag(String content, String tag) {
        String value = firstTag(content, tag);
        if (value == null || value.isBlank()) {
            throw new BankStatementParser.BankStatementParsingException("OFX sem campo obrigatório " + tag);
        }
        return value;
    }

    private String firstTag(String content, String tag) {
        Matcher matcher = Pattern.compile("(?is)<" + Pattern.quote(tag) + "\\s*>([^<\\r\\n]*)").matcher(content);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private BigDecimal parseAmount(String raw) {
        String value = clean(raw).replace(" ", "");
        if (value.indexOf(',') >= 0 && value.indexOf('.') >= 0) {
            value = value.lastIndexOf(',') > value.lastIndexOf('.')
                    ? value.replace(".", "").replace(',', '.')
                    : value.replace(",", "");
        } else if (value.indexOf(',') >= 0) {
            value = value.replace(',', '.');
        }
        return new BigDecimal(value);
    }

    private Instant parsePostedAt(String rawDate, LocalDate bookingDate) {
        if (rawDate.length() < 8) {
            return bookingDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        }
        return bookingDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    }

    private String maskAccount(String account) {
        if (account.length() <= 4) {
            return "****" + account;
        }
        return "****" + account.substring(account.length() - 4);
    }

    private String join(String left, String right) {
        String first = clean(left);
        String second = clean(right);
        if (first == null) return second;
        if (second == null || second.equals(first)) return first;
        return first + " · " + second;
    }

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim()
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String limit(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(max, value.length()));
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
