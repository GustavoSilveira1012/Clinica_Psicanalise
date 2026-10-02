package com.psicogest.psicogest.security;

import com.psicogest.psicogest.integration.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import com.psicogest.psicogest.model.entity.Availability;
import com.psicogest.psicogest.model.entity.AvailabilityException;
import com.psicogest.psicogest.security.tenant.TenantDatabaseContext;
import java.time.LocalTime;
import java.util.TimeZone;
import com.jayway.jsonpath.JsonPath;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import com.psicogest.psicogest.security.audit.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class AuthProfileRlsIntegrationTest extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired TenantDatabaseContext databaseContext;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AuditService auditService;
    @Autowired AuditChainVerificationService auditVerifier;
    @MockitoBean Clock clock;
    private String restrictedRole;
    private Long userId;
    private Long professionalId;
    private UUID organizationId;

    @BeforeEach
    void seedAndUseRestrictedRole() {
        when(clock.instant()).thenReturn(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusNanos(123456789));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        userId = jdbc.queryForObject("INSERT INTO users(name,email,password_hash,role) VALUES ('Synthetic professional',?,'not-a-usable-hash','PSYCHOANALYST') RETURNING id",
                Long.class, "rls-" + UUID.randomUUID() + "@example.invalid");
        organizationId = UUID.randomUUID();
        jdbc.update("INSERT INTO organizations(id,name,slug,type,owner_user_id,created_at,updated_at) VALUES (?,'Synthetic RLS organization',?,'CLINIC',?,now(),now())",
                organizationId, "rls-" + UUID.randomUUID(), userId);
        jdbc.update("INSERT INTO organization_memberships(id,organization_id,user_id,role,status,created_at,updated_at) VALUES (?,?,?,'OWNER','ACTIVE',now(),now())",
                UUID.randomUUID(), organizationId, userId);
        professionalId = jdbc.queryForObject("INSERT INTO psychoanalysts(user_id,organization_id) VALUES (?,?) RETURNING id", Long.class, userId, organizationId);
        String role = "rls_test_" + UUID.randomUUID().toString().replace("-", "");
        restrictedRole = role;
        jdbc.execute("CREATE ROLE " + role + " NOSUPERUSER NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA public, app TO " + role);
        jdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + role);
        jdbc.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + role);
        if (TestTransaction.isActive()) jdbc.execute("SET LOCAL ROLE " + role);
    }

    @Test
    void auditSessionIsPersistedWithTheSignedEntry() {
        var audit = appendSyntheticAudit(true);
        entityManager.flush();
        assertEquals(audit.getSession().getId(), jdbc.queryForObject("SELECT session_id FROM audit_logs WHERE id=?", UUID.class, audit.getId()));
    }

    @Test
    void auditSignatureSurvivesPostgresTimestampPrecisionAndReload() {
        var audit = appendSyntheticAudit(false);
        entityManager.flush();
        entityManager.clear();
        var verification = auditVerifier.verify(audit.getSequence(), audit.getSequence());
        assertTrue(verification.isSuccess(), verification.getReason());
        assertEquals(1, verification.getVerifiedCount());
    }

    private com.psicogest.psicogest.model.entity.AuditLog appendSyntheticAudit(boolean withSession) {
        databaseContext.applyUser(userId);
        databaseContext.applyOrganization(organizationId);
        UUID sessionId = withSession ? UUID.randomUUID() : null;
        if (sessionId != null) jdbc.update("INSERT INTO user_sessions(id,user_id,created_at,last_seen_at,expires_at) VALUES (?,?,now(),now(),now()+interval '1 day')", sessionId, userId);
        return auditService.recordCriticalWrite(new AuditCommand(userId, sessionId, AuditAction.MEDICAL_RECORD_CREATED,
                "Synthetic regression", UUID.randomUUID().toString(), null, null, AuditOutcome.SUCCESS,
                UUID.randomUUID().toString(), "127.0.0.1", null, Map.of("synthetic", true)));
    }

    private ResultActions performRestricted(MockHttpServletRequestBuilder request) {
        // Match HTTP transaction boundaries. Sensitive-read audit uses REQUIRES_NEW;
        // it must not wait on a prior write held by a test-wide transaction.
        return new TransactionTemplate(transactionManager).execute(status -> {
            jdbc.execute("SET LOCAL ROLE " + restrictedRole);
            try {
                ResultActions result = mvc.perform(request);
                // This outer transaction only supplies the runtime database role.
                // Mirror the tenant filter's rollback on handled HTTP errors.
                if (result.andReturn().getResponse().getStatus() >= 400) status.setRollbackOnly();
                return result;
            }
            catch (Exception exception) { throw new RuntimeException(exception); }
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void clinicalRecordCreationAndFinalizationPersistUnderRestrictedRlsRole() throws Exception {
        var auth = jwt().jwt(token -> token.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_PSYCHOANALYST"));
        String patientResponse = performRestricted(post("/patients").with(auth).header("X-Organization-Id", organizationId)
                        .contentType("application/json").content("{\"name\":\"Synthetic clinical patient\",\"email\":\"clinical-"+UUID.randomUUID()+"@example.invalid\",\"password\":\"Synthetic-E2E-2026!\",\"linkToCurrentProfessional\":true}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number patientId = JsonPath.read(patientResponse, "$.id");
        String recordResponse = performRestricted(post("/patients/"+patientId+"/medical-records").with(auth).header("X-Organization-Id", organizationId)
                        .contentType("application/json").content("{\"content\":\"Synthetic encrypted clinical regression\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String recordId = JsonPath.read(recordResponse, "$.id");
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM medical_records WHERE id=?", Long.class, UUID.fromString(recordId)));
        assertEquals(12, jdbc.queryForObject("SELECT octet_length(content_iv) FROM medical_records WHERE id=?", Integer.class, UUID.fromString(recordId)));
        performRestricted(get("/medical-records/"+recordId).with(auth).header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Synthetic encrypted clinical regression"));
        performRestricted(patch("/medical-records/"+recordId+"/finalize").with(auth).header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINALIZED"));
        performRestricted(put("/medical-records/"+recordId).with(auth).header("X-Organization-Id", organizationId)
                        .contentType("application/json").content("{\"content\":\"Forbidden synthetic overwrite\"}"))
                .andExpect(status().isForbidden());
        performRestricted(get("/medical-records/"+recordId).with(auth).header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Synthetic encrypted clinical regression"));
        performRestricted(post("/api/v1/medical-records/"+recordId+"/addendums").with(auth).header("X-Organization-Id", organizationId)
                        .contentType("application/json").content("{\"content\":\"Synthetic complementary note\",\"reason\":\"COMPLEMENT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void financialCreationAndCollectionPersistAndReplayUnderRestrictedRlsRole() throws Exception {
        jdbc.update("UPDATE users SET role='CLINIC_ADMIN' WHERE id=?", userId);
        Long clinicId = jdbc.queryForObject("INSERT INTO clinics(name,organization_id) VALUES ('Synthetic finance clinic',?) RETURNING id", Long.class, organizationId);
        jdbc.update("INSERT INTO clinic_user_memberships(clinic_id,user_id,access_role,status,started_at,organization_id) VALUES (?,?,'ADMIN','ACTIVE',now()-interval '1 day',?)", clinicId, userId, organizationId);
        jdbc.update("INSERT INTO financial_entities(id,entity_type,clinic_id,active,created_at,updated_at,organization_id) VALUES (?,'CLINIC',?,true,now(),now(),?)", UUID.randomUUID(), clinicId, organizationId);
        var auth = jwt().jwt(token -> token.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN"));
        String patient = performRestricted(post("/patients").with(auth).header("X-Organization-Id", organizationId)
                        .contentType("application/json").content("{\"name\":\"Synthetic finance patient\",\"email\":\"finance-"+UUID.randomUUID()+"@example.invalid\",\"password\":\"Synthetic-E2E-2026!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number patientId = JsonPath.read(patient, "$.id");
        String receivable = performRestricted(post("/api/v1/receivables").with(auth).header("X-Organization-Id", organizationId)
                        .contentType("application/json").content("{\"patientId\":"+patientId+",\"description\":\"Synthetic charge\",\"grossAmount\":125.50,\"discountAmount\":0,\"dueDate\":\""+java.time.LocalDate.now().plusDays(1)+"\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String receivableId = JsonPath.read(receivable, "$.id");
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM receivables WHERE id=?", Long.class, UUID.fromString(receivableId)));
        String key = UUID.randomUUID().toString();
        String body = "{\"patientId\":"+patientId+",\"receivableId\":\""+receivableId+"\",\"amount\":125.50,\"paymentMethod\":\"PIX\"}";
        String collected = performRestricted(post("/api/v1/payments/collect").with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", key)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.payment.allocatedAmount").value(125.50))
                .andExpect(jsonPath("$.payment.availableAmount").value(0)).andReturn().getResponse().getContentAsString();
        String allocationId = JsonPath.read(collected, "$.allocation.id");
        performRestricted(post("/api/v1/payments/collect").with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", key)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.allocation.id").value(allocationId));
        performRestricted(get("/api/v1/receivables/"+receivableId).with(auth).header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outstandingAmount").value(0));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM payments WHERE idempotency_key=?", Integer.class, key));
        performRestricted(post("/api/v1/payments/collect").with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", key)
                        .contentType("application/json").content(body.replace(receivableId, UUID.randomUUID().toString())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        String paymentId = JsonPath.read(collected, "$.payment.id");
        String refundKey = UUID.randomUUID().toString();
        String refundBody = "{\"amount\":25.50,\"reason\":\"CUSTOMER_REQUEST\",\"allocations\":[{\"paymentAllocationId\":\""+allocationId+"\",\"amount\":25.50}]}";
        String refundPath = "/api/v1/payments/"+paymentId+"/refunds";
        String refund = performRestricted(post(refundPath).with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", refundKey)
                        .contentType("application/json").content(refundBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String refundId = JsonPath.read(refund, "$.id");
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM refunds WHERE id=?", Long.class, UUID.fromString(refundId)));
        performRestricted(post(refundPath).with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", refundKey)
                        .contentType("application/json").content(refundBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(refundId));
        performRestricted(post(refundPath).with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", refundKey)
                        .contentType("application/json").content(refundBody.replace("CUSTOMER_REQUEST","PAYMENT_ERROR")))
                .andExpect(status().isConflict());
        performRestricted(post(refundPath+"/"+refundId+"/confirm").with(auth).header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        performRestricted(post(refundPath).with(auth).header("X-Organization-Id", organizationId).header("Idempotency-Key", refundKey)
                        .contentType("application/json").content(refundBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(refundId));
        performRestricted(get("/api/v1/receivables/"+receivableId).with(auth).header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outstandingAmount").value(25.50));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM refunds WHERE idempotency_key=?", Integer.class, refundKey));
    }

    @Test
    void localScheduleTimesPreserveWallClockAcrossJdbcAndJvmTimezones() {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
            databaseContext.applyUser(userId);
            databaseContext.applyOrganization(organizationId);
            Long availabilityId = jdbc.queryForObject("INSERT INTO availability(psychoanalyst_id,day_of_week,start_time,end_time) VALUES (?,1,'09:17','18:23') RETURNING id", Long.class, professionalId);
            Availability availability = entityManager.find(Availability.class, availabilityId);
            assertEquals(LocalTime.of(9,17), availability.getStartTime());
            assertEquals(LocalTime.of(18,23), availability.getEndTime());
            availability.setStartTime(LocalTime.of(10,19));
            entityManager.flush();
            assertEquals("10:19:00", jdbc.queryForObject("SELECT start_time::text FROM availability WHERE id=?", String.class, availabilityId));
            Long exceptionId = jdbc.queryForObject("INSERT INTO availability_exceptions(psychoanalyst_id,exception_date,type,start_time,end_time,created_at,updated_at) VALUES (?,current_date,'BLOCKED','12:31','13:37',now(),now()) RETURNING id", Long.class, professionalId);
            AvailabilityException exception = entityManager.find(AvailabilityException.class, exceptionId);
            assertEquals(LocalTime.of(12,31), exception.getStartTime());
            assertEquals(LocalTime.of(13,37), exception.getEndTime());
            exception.setEndTime(LocalTime.of(14,41));
            entityManager.flush();
            assertEquals("14:41:00", jdbc.queryForObject("SELECT end_time::text FROM availability_exceptions WHERE id=?", String.class, exceptionId));
        } finally { TimeZone.setDefault(previous); }
    }

    @Test
    void profileDiscoversOwnMembershipAndProfessionalWithRlsEnabled() throws Exception {
        mvc.perform(get("/auth/me").with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizations[0].id").value(organizationId.toString()))
                .andExpect(jsonPath("$.psychoanalystId").value(professionalId));
    }

    @Test
    void explicitSelfRelationshipMakesNewPatientAccessible() throws Exception {
        String email = "new-" + UUID.randomUUID() + "@example.invalid";
        mvc.perform(post("/patients")
                        .with(jwt().jwt(token -> token.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_PSYCHOANALYST")))
                        .header("X-Organization-Id", organizationId)
                        .contentType("application/json")
                        .content("{\"name\":\"Synthetic new patient\",\"email\":\"" + email + "\",\"password\":\"Synthetic-E2E-2026!\",\"linkToCurrentProfessional\":true}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/patients").with(jwt().jwt(token -> token.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_PSYCHOANALYST")))
                        .header("X-Organization-Id", organizationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(email));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM therapeutic_relationships WHERE psychoanalyst_id = ?", Integer.class, professionalId));
        mvc.perform(get("/patients/search").param("query", email)
                        .with(jwt().jwt(token -> token.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_PSYCHOANALYST")))
                        .header("X-Organization-Id", organizationId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].email").value(email));
    }

    @Test
    void administratorCannotAcquireClinicalRelationshipThroughPatientCreation() throws Exception {
        jdbc.update("UPDATE users SET role='CLINIC_ADMIN' WHERE id=?", userId);
        String email = "denied-" + UUID.randomUUID() + "@example.invalid";
        mvc.perform(post("/patients")
                        .with(jwt().jwt(token -> token.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")))
                        .header("X-Organization-Id", organizationId)
                        .contentType("application/json")
                        .content("{\"name\":\"Synthetic denied patient\",\"email\":\"" + email + "\",\"password\":\"Synthetic-E2E-2026!\",\"linkToCurrentProfessional\":true}"))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email=?", Integer.class, email));
    }
}
