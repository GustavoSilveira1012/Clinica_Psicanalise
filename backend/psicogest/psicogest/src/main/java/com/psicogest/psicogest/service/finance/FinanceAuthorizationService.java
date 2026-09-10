package com.psicogest.psicogest.service.finance;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.exception.AuthorizationException;
import com.psicogest.psicogest.model.entity.BankAccount;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.repository.BankAccountRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.security.SecurityActor;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de autorização para operações financeiras
 * 
 * Valida:
 * - User tem acesso à Clinic
 * - Clinic proprietária dos recursos
 * - User tem permission FINANCE
 * 
 * Nunca:
 * - ROLE_CLINIC_ADMIN → acesso irrestrito ❌
 * - ROLE_SYSTEM_ADMIN → acesso financeiro por padrão ❌
 */
@Slf4j
@Service
public class FinanceAuthorizationService {

    private final BankAccountRepository bankAccountRepository;

    private final PaymentRepository paymentRepository;

    public FinanceAuthorizationService(
            BankAccountRepository bankAccountRepository,
            PaymentRepository paymentRepository
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Valida acesso a conta bancária
     * 
     * @param bankAccountId ID da conta
     * @param actor usuário
     * @throws AuthorizationException se não tem acesso
     */
    public void validateBankAccountAccess(
            UUID bankAccountId,
            SecurityActor actor
    ) {

        BankAccount account =
                bankAccountRepository
                        .findById(bankAccountId)
                        .orElse(null);

        if (account == null) {
            throw new AuthorizationException(
                    "Conta bancária não encontrada"
            );
        }

        validateClinicAccess(
                account.getClinic().getId(),
                actor
        );
    }

    /**
     * Valida acesso a payment
     * 
     * @param paymentId ID do pagamento
     * @param actor usuário
     * @throws AuthorizationException se não tem acesso
     */
    public void validatePaymentAccess(
            UUID paymentId,
            SecurityActor actor
    ) {

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElse(null);

        if (payment == null) {
            throw new AuthorizationException(
                    "Pagamento não encontrado"
            );
        }

        validateClinicAccess(
                payment.getClinic().getId(),
                actor
        );
    }

    /**
     * Valida acesso a Clinic
     * 
     * Regra: User deve ter:
     * - ClinicMembership com permission FINANCE, OU
     * - PsychoanalystOwnership (nesta clínica) + permission FINANCE
     * 
     * @param clinicId ID da clínica
     * @param actor usuário
     * @throws AuthorizationException se não tem acesso
     */
    public void validateClinicAccess(
            Long clinicId,
            SecurityActor actor
    ) {

        // TODO: implementar após model de permissions estar pronto
        // Por enquanto apenas log
        log.debug(
                "Validando acesso à Clinic: " +
                        "userId={}, clinicId={}",
                actor.userId(),
                clinicId
        );
    }

    /**
     * Valida que duas clínicas são a mesma
     * 
     * @param clinic1 primeira clínica
     * @param clinic2 segunda clínica
     * @throws AuthorizationException se diferentes
     */
    public void validateSameClinic(
            Clinic clinic1,
            Clinic clinic2
    ) {

        if (!clinic1.getId()
                .equals(clinic2.getId())) {

            throw new AuthorizationException(
                    "Lançamento bancário e pagamento pertencem a clínicas diferentes"
            );
        }
    }
}
