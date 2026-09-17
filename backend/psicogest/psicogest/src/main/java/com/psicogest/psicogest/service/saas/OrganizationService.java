package com.psicogest.psicogest.service.saas;

import com.psicogest.psicogest.dto.saas.CreateOrganizationInviteRequest;
import com.psicogest.psicogest.dto.saas.CreateOrganizationRequest;
import com.psicogest.psicogest.dto.saas.OnboardingResponse;
import com.psicogest.psicogest.dto.saas.OrganizationInviteResponse;
import com.psicogest.psicogest.dto.saas.OrganizationMembershipResponse;
import com.psicogest.psicogest.dto.saas.OrganizationResponse;
import com.psicogest.psicogest.config.SaasCommercialProperties;
import com.psicogest.psicogest.exception.AccessDeniedException;
import com.psicogest.psicogest.exception.LastOrganizationOwnerException;
import com.psicogest.psicogest.exception.OrganizationInviteException;
import com.psicogest.psicogest.exception.ResourceNotFoundException;
import com.psicogest.psicogest.model.entity.User;
import com.psicogest.psicogest.model.entity.saas.Organization;
import com.psicogest.psicogest.model.entity.saas.OrganizationInvite;
import com.psicogest.psicogest.model.entity.saas.OrganizationMembership;
import com.psicogest.psicogest.model.entity.saas.OrganizationOnboarding;
import com.psicogest.psicogest.model.entity.saas.SaasPlanVersion;
import com.psicogest.psicogest.model.entity.saas.SaasSubscription;
import com.psicogest.psicogest.model.enums.OnboardingStatus;
import com.psicogest.psicogest.model.enums.OnboardingStep;
import com.psicogest.psicogest.model.enums.OrganizationInviteStatus;
import com.psicogest.psicogest.model.enums.OrganizationMembershipStatus;
import com.psicogest.psicogest.model.enums.OrganizationRole;
import com.psicogest.psicogest.model.enums.SaasPlanVersionStatus;
import com.psicogest.psicogest.model.enums.SaasSubscriptionStatus;
import com.psicogest.psicogest.repository.OrganizationInviteRepository;
import com.psicogest.psicogest.repository.OrganizationMembershipRepository;
import com.psicogest.psicogest.repository.OrganizationOnboardingRepository;
import com.psicogest.psicogest.repository.OrganizationRepository;
import com.psicogest.psicogest.repository.SaasPlanVersionRepository;
import com.psicogest.psicogest.repository.SaasSubscriptionRepository;
import com.psicogest.psicogest.repository.UserRepository;
import com.psicogest.psicogest.security.SecurityHashService;
import com.psicogest.psicogest.security.authorization.AuthenticatedUserContext;
import com.psicogest.psicogest.security.crypto.ApplicationEncryptionService;
import com.psicogest.psicogest.security.crypto.EncryptedEnvelope;
import com.psicogest.psicogest.security.crypto.EncryptionContext;
import com.psicogest.psicogest.security.tenant.TenantContext;
import com.psicogest.psicogest.security.tenant.TenantContextResolver;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@EnableConfigurationProperties(SaasCommercialProperties.class)
public class OrganizationService {

    private static final Duration INVITE_TTL = Duration.ofHours(72);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationInviteRepository inviteRepository;
    private final OrganizationOnboardingRepository onboardingRepository;
    private final SaasSubscriptionRepository subscriptionRepository;
    private final SaasPlanVersionRepository planVersionRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final TenantContextResolver tenantContextResolver;
    private final TenantDatabaseContext databaseContext;
    private final SecurityHashService hashService;
    private final ApplicationEncryptionService encryptionService;
    private final SaasCommercialProperties commercialProperties;
    private final SecureRandom random = new SecureRandom();

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            OrganizationInviteRepository inviteRepository,
            OrganizationOnboardingRepository onboardingRepository,
            SaasSubscriptionRepository subscriptionRepository,
            SaasPlanVersionRepository planVersionRepository,
            UserRepository userRepository,
            AuthenticatedUserContext authenticatedUserContext,
            TenantContextResolver tenantContextResolver,
            TenantDatabaseContext databaseContext,
            SecurityHashService hashService,
            ApplicationEncryptionService encryptionService,
            SaasCommercialProperties commercialProperties
    ) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.inviteRepository = inviteRepository;
        this.onboardingRepository = onboardingRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planVersionRepository = planVersionRepository;
        this.userRepository = userRepository;
        this.authenticatedUserContext = authenticatedUserContext;
        this.tenantContextResolver = tenantContextResolver;
        this.databaseContext = databaseContext;
        this.hashService = hashService;
        this.encryptionService = encryptionService;
        this.commercialProperties = commercialProperties;
    }

    @Transactional
    public OrganizationResponse create(@Valid CreateOrganizationRequest request, Authentication authentication) {
        Long userId = currentUserId(authentication);
        databaseContext.applyUser(userId);
        if (organizationRepository.existsBySlugIgnoreCase(request.slug())) {
            throw new OrganizationInviteException("Já existe uma organização com este slug");
        }

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        Organization organization = Organization.builder()
                .name(request.name().trim())
                .slug(request.slug().toLowerCase(Locale.ROOT))
                .type(request.type())
                .timezone(request.timezone() == null || request.timezone().isBlank()
                        ? "America/Sao_Paulo" : request.timezone())
                .owner(owner)
                .build();
        organization = organizationRepository.save(organization);

        databaseContext.applyOrganization(organization.getId());
        membershipRepository.save(OrganizationMembership.builder()
                .organization(organization)
                .user(owner)
                .role(OrganizationRole.OWNER)
                .status(OrganizationMembershipStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());
        onboardingRepository.save(OrganizationOnboarding.builder()
                .organization(organization)
                .currentStep(OnboardingStep.PROFILE)
                .status(OnboardingStatus.IN_PROGRESS)
                .completedSteps(List.of())
                .build());
        createTrialSubscription(organization);

        return toResponse(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembershipResponse> mine(Authentication authentication) {
        Long userId = currentUserId(authentication);
        databaseContext.applyUser(userId);
        return membershipRepository.findAllByUserIdAndStatusOrderByCreatedAtAsc(
                        userId, OrganizationMembershipStatus.ACTIVE)
                .stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembershipResponse> members(UUID organizationId, Authentication authentication) {
        requireMembership(organizationId, authentication);
        return membershipRepository.findMembers(organizationId, OrganizationMembershipStatus.ACTIVE)
                .stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional
    public OrganizationInviteResponse invite(
            UUID organizationId,
            CreateOrganizationInviteRequest request,
            Authentication authentication
    ) {
        OrganizationMembership actor = requireMembership(organizationId, authentication);
        if (actor.getRole() != OrganizationRole.OWNER && actor.getRole() != OrganizationRole.ADMIN) {
            throw new AccessDeniedException("Somente owner ou admin pode convidar membros");
        }
        if (request.role() == OrganizationRole.OWNER) {
            throw new OrganizationInviteException("A transferência de ownership ocorre em uma operação própria");
        }
        String email = normalizeEmail(request.email());
        String emailHash = hashService.sha256(email);
        if (inviteRepository.existsByOrganizationIdAndEmailHashAndStatus(
                organizationId, emailHash, OrganizationInviteStatus.PENDING)) {
            throw new OrganizationInviteException("Já existe um convite pendente para este e-mail");
        }

        UUID inviteId = UUID.randomUUID();
        String token = randomToken();
        Instant expiresAt = Instant.now().plus(INVITE_TTL);
        Organization organization = actor.getOrganization();
        OrganizationInvite invite = OrganizationInvite.builder()
                .id(inviteId)
                .organization(organization)
                .invitedBy(actor.getUser())
                .emailHash(emailHash)
                .tokenHash(hashService.sha256(token))
                .role(request.role())
                .expiresAt(expiresAt)
                .build();
        EncryptedEnvelope encrypted = encryptionService.encrypt(
                email,
                new EncryptionContext("ORGANIZATION_INVITE", inviteId.toString(), "email",
                        java.util.Map.of("organizationId", organizationId.toString())));
        invite.setEmailCiphertext(encrypted.ciphertext());
        invite.setEmailIv(encrypted.iv());
        invite.setEmailEncryptedDek(encrypted.wrappedDataKey());
        invite.setEmailKeyId(encrypted.keyId());
        invite.setEmailCryptoVersion(encrypted.cryptoVersion());
        invite.setEmailCryptoAlgorithm(encrypted.algorithm());
        inviteRepository.save(invite);

        return new OrganizationInviteResponse(inviteId, maskEmail(email), token, expiresAt);
    }

    @Transactional
    public OrganizationMembershipResponse acceptInvite(String rawToken, Authentication authentication) {
        if (rawToken == null || rawToken.length() < 32) {
            throw new OrganizationInviteException("Convite inválido ou expirado");
        }
        String tokenHash = hashService.sha256(rawToken);
        databaseContext.applyInviteToken(tokenHash);
        OrganizationInvite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new OrganizationInviteException("Convite inválido ou expirado"));
        if (invite.getStatus() != OrganizationInviteStatus.PENDING ||
                invite.getExpiresAt().isBefore(Instant.now())) {
            throw new OrganizationInviteException("Convite inválido ou expirado");
        }

        Long userId = currentUserId(authentication);
        databaseContext.applyUser(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (!hashService.sha256(normalizeEmail(user.getEmail())).equals(invite.getEmailHash())) {
            throw new OrganizationInviteException("O convite foi emitido para outro e-mail");
        }

        UUID organizationId = invite.getOrganization().getId();
        databaseContext.applyOrganization(organizationId);
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(
                        organizationId, userId, OrganizationMembershipStatus.ACTIVE)
                .orElseGet(() -> membershipRepository.save(OrganizationMembership.builder()
                        .organization(invite.getOrganization())
                        .user(user)
                        .role(invite.getRole())
                        .status(OrganizationMembershipStatus.ACTIVE)
                        .invitedAt(invite.getCreatedAt())
                        .joinedAt(Instant.now())
                        .build()));
        invite.setStatus(OrganizationInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);
        return toMembershipResponse(membership);
    }

    @Transactional
    public OnboardingResponse updateOnboarding(
            UUID organizationId,
            OnboardingStep step,
            Authentication authentication
    ) {
        requireMembership(organizationId, authentication);
        OrganizationOnboarding onboarding = onboardingRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding não encontrado"));
        List<String> completed = new ArrayList<>(onboarding.getCompletedSteps() == null
                ? List.of() : onboarding.getCompletedSteps());
        if (step != OnboardingStep.COMPLETE && !completed.contains(step.name())) {
            completed.add(step.name());
        }
        onboarding.setCompletedSteps(completed);
        onboarding.setCurrentStep(step);
        if (step == OnboardingStep.COMPLETE) {
            onboarding.setStatus(OnboardingStatus.COMPLETED);
            onboarding.setCompletedAt(Instant.now());
        }
        return toOnboardingResponse(onboardingRepository.save(onboarding));
    }

    @Transactional(readOnly = true)
    public OnboardingResponse onboarding(
            UUID organizationId,
            Authentication authentication
    ) {
        requireMembership(organizationId, authentication);
        OrganizationOnboarding onboarding = onboardingRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding não encontrado"));
        return toOnboardingResponse(onboarding);
    }

    @Transactional
    public OrganizationMembershipResponse transferOwnership(
            UUID organizationId,
            UUID targetMembershipId,
            Authentication authentication
    ) {
        OrganizationMembership actor = requireMembership(organizationId, authentication);
        if (actor.getRole() != OrganizationRole.OWNER) {
            throw new AccessDeniedException("Somente o owner pode transferir ownership");
        }
        OrganizationMembership target = membershipRepository.findById(targetMembershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        if (!target.getOrganization().getId().equals(organizationId) ||
                target.getStatus() != OrganizationMembershipStatus.ACTIVE) {
            throw new AccessDeniedException("Membro não pertence à organização");
        }
        if (target.getId().equals(actor.getId())) {
            return toMembershipResponse(actor);
        }
        actor.setRole(OrganizationRole.ADMIN);
        target.setRole(OrganizationRole.OWNER);
        Organization organization = target.getOrganization();
        organization.setOwner(target.getUser());
        membershipRepository.save(actor);
        membershipRepository.save(target);
        organizationRepository.save(organization);
        return toMembershipResponse(target);
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID membershipId, Authentication authentication) {
        OrganizationMembership actor = requireMembership(organizationId, authentication);
        if (actor.getRole() != OrganizationRole.OWNER && actor.getRole() != OrganizationRole.ADMIN) {
            throw new AccessDeniedException("Sem permissão para remover membros");
        }
        OrganizationMembership member = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new AccessDeniedException("Membro não pertence à organização");
        }
        if (member.getRole() == OrganizationRole.OWNER &&
                membershipRepository.countByOrganizationIdAndRoleAndStatus(
                        organizationId, OrganizationRole.OWNER, OrganizationMembershipStatus.ACTIVE) <= 1) {
            throw new LastOrganizationOwnerException("A organização precisa manter pelo menos um owner ativo");
        }
        member.setStatus(OrganizationMembershipStatus.REMOVED);
        membershipRepository.save(member);
    }

    public OrganizationMembership requireMembership(UUID organizationId, Authentication authentication) {
        TenantContext context = tenantContextResolver.resolve(authentication, organizationId);
        return membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                        context.organizationId(), context.userId(), OrganizationMembershipStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("Membership ativa não encontrada"));
    }

    private void createTrialSubscription(Organization organization) {
        SaasPlanVersion plan = planVersionRepository.findPublishedByPlanCode(
                        "START", SaasPlanVersionStatus.PUBLISHED)
                .orElse(null);
        if (plan == null) return;
        Instant now = Instant.now();
        subscriptionRepository.save(SaasSubscription.builder()
                .organization(organization)
                .planVersion(plan)
                .status(SaasSubscriptionStatus.TRIALING)
                .trialStartedAt(now)
                .trialEndsAt(now.plus(Duration.ofDays(commercialProperties.trialDays())))
                .build());
    }

    private Long currentUserId(Authentication authentication) {
        return authenticatedUserContext.userId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Usuário autenticado não identificado"));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "•••@" + parts[1];
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(), organization.getName(), organization.getSlug(),
                organization.getType(), organization.getStatus(), organization.getTimezone(),
                organization.getCreatedAt());
    }

    private OrganizationMembershipResponse toMembershipResponse(OrganizationMembership membership) {
        return new OrganizationMembershipResponse(
                membership.getId(), membership.getOrganization().getId(), membership.getUser().getId(),
                membership.getUser().getName(), membership.getRole(), membership.getStatus());
    }

    private OnboardingResponse toOnboardingResponse(OrganizationOnboarding onboarding) {
        return new OnboardingResponse(
                onboarding.getOrganization().getId(), onboarding.getCurrentStep(), onboarding.getStatus(),
                onboarding.getCompletedSteps(), onboarding.getCompletedAt());
    }
}
