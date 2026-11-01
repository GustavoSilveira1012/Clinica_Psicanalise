package com.psicogest.psicogest.security;

import com.psicogest.psicogest.security.pilot.ClinicalPilotScopeGuard;
import com.psicogest.psicogest.security.tenant.TenantContextFilter;
import com.psicogest.psicogest.security.tenant.TenantContextResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TenantContextFilterTransactionTest {
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void handledDomainErrorMarksRequestForRollbackAndPreservesResponse() throws Exception {
        var status = new SimpleTransactionStatus();
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(status);
        var filter = new TenantContextFilter(mock(TenantContextResolver.class), manager, mock(ClinicalPilotScopeGuard.class));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));
        var request = new MockHttpServletRequest("POST", "/patients");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            response.setStatus(409);
            response.getWriter().write("{\"message\":\"Conflito\"}");
        });
        assertTrue(status.isRollbackOnly());
        assertEquals(409, response.getStatus());
        assertEquals("{\"message\":\"Conflito\"}", response.getContentAsString());
    }

    @Test void successfulRequestRemainsEligibleForCommit() throws Exception {
        var status = new SimpleTransactionStatus();
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(status);
        var filter = new TenantContextFilter(mock(TenantContextResolver.class), manager, mock(ClinicalPilotScopeGuard.class));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));
        filter.doFilter(new MockHttpServletRequest("POST", "/patients"), new MockHttpServletResponse(), (req, res) -> {});
        assertFalse(status.isRollbackOnly());
    }
}
