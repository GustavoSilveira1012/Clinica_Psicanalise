package com.psicogest.psicogest.service.finance;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.exception.AuthorizationException;
import com.psicogest.psicogest.model.entity.BankAccount;
import com.psicogest.psicogest.model.entity.Clinic;
import com.psicogest.psicogest.model.entity.Payment;
import com.psicogest.psicogest.model.entity.ClinicUserMembership;
import com.psicogest.psicogest.model.enums.ClinicAccessRole;
import com.psicogest.psicogest.model.enums.ClinicUserMembershipStatus;
import com.psicogest.psicogest.model.enums.UserRole;
import com.psicogest.psicogest.repository.BankAccountRepository;
import com.psicogest.psicogest.repository.ClinicUserMembershipRepository;
import com.psicogest.psicogest.repository.PaymentRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.SecurityActor;
import com.psicogest.psicogest.security.tenant.TenantContextHolder;

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

    private final ClinicUserMembershipRepository membershipRepository;

    private final UserRepository userRepository;

    public FinanceAuthorizationService(
            BankAccountRepository bankAccountRepository,
            PaymentRepository paymentRepository,
            ClinicUserMembershipRepository membershipRepository,
            UserRepository userRepository
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.paymentRepository = paymentRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
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

        if (payment.getClinic() == null) {
            throw new AuthorizationException("Pagamento sem contexto financeiro");
        }
        validateClinicAccess(payment.getClinic().getId(), actor);
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

        if (actor == null || actor.userId() == null || clinicId == null) {
            throw new AuthorizationException("Contexto financeiro ausente");
        }
        var tenant = TenantContextHolder.get();
        if (tenant == null || tenant.organizationId() == null
                || !actor.userId().equals(tenant.userId())) {
            throw new AuthorizationException("Contexto de organização não autorizado");
        }

        UserRole role = userRepository.findById(actor.userId())
                .map(user -> user.getRole())
                .orElseThrow(() -> new AuthorizationException("Usuário não encontrado"));
        if (role == UserRole.SYSTEM_ADMIN) {
            throw new AuthorizationException("Acesso financeiro de administrador de sistema exige delegação explícita");
        }

        boolean allowed = membershipRepository
                .findByUserIdAndStatus(actor.userId(), ClinicUserMembershipStatus.ACTIVE)
                .stream()
                .filter(membership -> membership.getClinic() != null
                        && clinicId.equals(membership.getClinic().getId())
                        && Boolean.TRUE.equals(membership.getClinic().getActive())
                        && tenant.organizationId().equals(membership.getClinic().getOrganizationId()))
                .map(ClinicUserMembership::getAccessRole)
                .anyMatch(accessRole -> accessRole == ClinicAccessRole.ADMIN
                        || accessRole == ClinicAccessRole.FINANCE);
        if (!allowed) {
            throw new AuthorizationException("Usuário não possui permissão financeira nesta clínica");
        }
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

        if (clinic1 == null || clinic2 == null || clinic1.getId() == null || !clinic1.getId()
                .equals(clinic2.getId())) {

            throw new AuthorizationException(
                    "Lançamento bancário e pagamento pertencem a clínicas diferentes"
            );
        }
    }
}
