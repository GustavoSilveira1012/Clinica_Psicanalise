package com.psicogest.psicogest.service.bank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.exception.FinanceValidationException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.infrastructure.bank.parser.BankStatementParser;
import com.psicogest.psicogest.infrastructure.bank.parser.ParsedBankStatement;
import com.psicogest.psicogest.infrastructure.bank.parser.ParsedBankTransaction;
import com.psicogest.psicogest.model.entity.BankAccount;
import com.psicogest.psicogest.model.entity.BankTransaction;
import com.psicogest.psicogest.repository.BankAccountRepository;
import com.psicogest.psicogest.repository.BankTransactionRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.SecurityHashService;
import com.psicogest.psicogest.security.audit.AuditAction;
import com.psicogest.psicogest.security.audit.AuditCommand;
import com.psicogest.psicogest.security.audit.AuditOutcome;
import com.psicogest.psicogest.security.audit.AuditService;
import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import com.psicogest.psicogest.security.crypto.EncryptionContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de importação de extratos bancários
 * 
 * Fluxo:
 * 1. Validar limite de tamanho
 * 2. Calcular SHA-256
 * 3. Detectar parser (por conteúdo, não nome)
 * 4. Parse → ParsedBankStatement
 * 5. Validar conta
 * 6. Gerar fingerprints
 * 7. INSERT transações (com deduplicação)
 * 8. AuditLog
 * 9. COMMIT
 */
@Slf4j
@Service
@Transactional
public class BankStatementImportService {

    private static final int MAX_STATEMENT_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final int BATCH_SIZE = 100;

    private final BankAccountRepository bankAccountRepository;

    private final BankTransactionRepository bankTransactionRepository;

    private final BankStatementParserRegistry parserRegistry;

    private final ApplicationEncryptionService encryptionService;

    private final SecurityHashService hashService;

    private final AuditService auditService;

    private final Clock clock;

    public BankStatementImportService(
            BankAccountRepository bankAccountRepository,
            BankTransactionRepository bankTransactionRepository,
            BankStatementParserRegistry parserRegistry,
            ApplicationEncryptionService encryptionService,
            SecurityHashService hashService,
            AuditService auditService,
            Clock clock
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.parserRegistry = parserRegistry;
        this.encryptionService = encryptionService;
        this.hashService = hashService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Importa extrato bancário
     * 
     * @param bankAccountId ID da conta
     * @param rawData bytes do arquivo
     * @param actor usuário que está importando
     * @return resultado da importação
     */
    public BankStatementImportResult importStatement(
            UUID bankAccountId,
            byte[] rawData,
            SecurityActor actor
    ) {

        Instant now = clock.instant();

        try {

            // 1. Validar limite
            if (rawData.length > MAX_STATEMENT_SIZE) {

                throw new FinanceValidationException(
                        "Arquivo de extrato excede limite de " + (MAX_STATEMENT_SIZE / 1024 / 1024) + " MB"
                );
            }

            // 2. Calcular SHA-256
            String fileHash = hashService.sha256(rawData);

            // 3. Buscar conta
            BankAccount account =
                    bankAccountRepository
                            .findById(bankAccountId)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Conta bancária não encontrada"
                                            )
                            );

            // 4. Detectar parser
            BankStatementParser parser =
                    parserRegistry.detectParser(rawData);

            if (parser == null) {

                throw new FinanceValidationException(
                        "Formato de extrato não reconhecido"
                );
            }

            // 5. Parse
            ParsedBankStatement statement =
                    parser.parse(rawData);

            // 6. Validar conta (bankCode + branch)
            if (!statement.bankCode().equals(account.getBankCode()) ||
                    !statement.branch().equals(account.getBranch())) {

                throw new FinanceValidationException(
                        "Dados do banco/agência do extrato não correspondem à conta configurada"
                );
            }

            // 7. Importar transações
            int importedCount = importTransactions(
                    account,
                    statement.transactions(),
                    now
            );

            // 8. Auditoria
            auditService.recordCriticalWrite(

                    new AuditCommand(

                            actor.userId(),

                            actor.sessionId(),

                            AuditAction.BANK_STATEMENT_IMPORTED,

                            "BANK_ACCOUNT",

                            bankAccountId.toString(),

                            null,

                            account.getClinic().getId(),

                            AuditOutcome.SUCCESS,

                            actor.correlationId(),

                            actor.sourceIp(),

                            actor.userAgentHash(),

                            Map.of(
                                    "fileHash",
                                    fileHash,

                                    "parser",
                                    parser.source().name(),

                                    "transactionCount",
                                    String.valueOf(statement.transactions().size()),

                                    "importedCount",
                                    String.valueOf(importedCount)
                            )
                    )
            );

            log.info(
                    "Extrato bancário importado: " +
                            "bankAccountId={}, parser={}, transactions={}, imported={}, hash={}",
                    bankAccountId,
                    parser.source(),
                    statement.transactions().size(),
                    importedCount,
                    fileHash
            );

            return new BankStatementImportResult(
                    true,
                    "Extrato importado com sucesso",
                    importedCount,
                    statement.transactions().size() - importedCount
            );

        } catch (Exception e) {

            log.error(
                    "Erro ao importar extrato bancário: bankAccountId={}",
                    bankAccountId,
                    e
            );

            // Auditoria de erro
            auditService.recordCriticalWrite(

                    new AuditCommand(

                            actor.userId(),

                            actor.sessionId(),

                            AuditAction.BANK_STATEMENT_IMPORT_FAILED,

                            "BANK_ACCOUNT",

                            bankAccountId.toString(),

                            null,

                            null,

                            AuditOutcome.FAILURE,

                            actor.correlationId(),

                            actor.sourceIp(),

                            actor.userAgentHash(),

                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    )
            );

            throw e;
        }
    }

    /**
     * Importa transações individuais com deduplicação
     */
    private int importTransactions(
            BankAccount account,
            List<ParsedBankTransaction> transactions,
            Instant now
    ) {

        int importedCount = 0;

        for (ParsedBankTransaction parsed : transactions) {

            try {

                // Gerar fingerprint
                String fingerprint = generateFingerprint(parsed);

                // Verificar duplicação
                if (bankTransactionRepository
                        .existsByBankAccountIdAndTransactionFingerprint(
                                account.getId(),
                                fingerprint
                        )) {

                    log.debug(
                            "Transação duplicada: fingerprint={}",
                            fingerprint
                    );

                    continue;
                }

                // Criar entity
                BankTransaction transaction =
                        BankTransaction.builder()

                                .id(UUID.randomUUID())

                                .bankAccount(account)

                                .externalTransactionId(
                                        parsed.externalId()
                                )

                                .direction(parsed.direction())

                                .amount(parsed.amount())

                                .currency(parsed.currency())

                                .bookingDate(parsed.bookingDate())

                                .postedAt(parsed.postedAt())

                                .reference(parsed.reference())

                                .transactionFingerprint(fingerprint)

                                .createdAt(now)

                                .build();

                // Criptografar description
                if (parsed.description() != null) {

                    encryptDescription(
                            transaction,
                            parsed.description(),
                            account.getId()
                    );
                }

                bankTransactionRepository.save(transaction);

                importedCount++;

            } catch (Exception e) {

                log.warn(
                        "Erro ao importar transação individual: date={}, amount={}",
                        parsed.bookingDate(),
                        parsed.amount(),
                        e
                );

                // Continuar com próxima transação
            }
        }

        return importedCount;
    }

    /**
     * Gera fingerprint de transação
     */
    private String generateFingerprint(
            ParsedBankTransaction transaction
    ) {

        String canonical =
                String.join(
                        "|",

                        transaction.bookingDate()
                                .toString(),

                        transaction.amount()
                                .toPlainString(),

                        transaction.currency(),

                        transaction.direction()
                                .name(),

                        transaction.description() != null
                                ? transaction.description()
                                : "",

                        transaction.reference() != null
                                ? transaction.reference()
                                : "",

                        transaction.externalId() != null
                                ? transaction.externalId()
                                : ""
                );

        return hashService.sha256(canonical);
    }

    /**
     * Criptografa description da transação
     */
    private void encryptDescription(
            BankTransaction transaction,
            String description,
            UUID bankAccountId
    ) {

        EncryptionContext context =
                new EncryptionContext(
                        "BANK_TRANSACTION",

                        transaction.getId().toString(),

                        "description",

                        Map.of(
                                "financialEntityId",
                                bankAccountId.toString()
                        )
                );

        EncryptedEnvelope encrypted =
                encryptionService.encrypt(
                        description,
                        context
                );

        transaction.setEncryptedDescription(
                encrypted.ciphertext()
        );

        transaction.setDescriptionIv(
                encrypted.iv()
        );

        transaction.setDescriptionEncryptedDek(
                encrypted.wrappedDataKey()
        );

        transaction.setCryptoVersion(
                encrypted.cryptoVersion()
        );

        transaction.setCryptoAlgorithm(
                encrypted.algorithm()
        );

        transaction.setKeyId(encrypted.keyId());
    }

    /**
     * Resultado da importação
     */
    public record BankStatementImportResult(
            boolean success,
            String message,
            int importedCount,
            int duplicateCount
    ) {
    }
}
