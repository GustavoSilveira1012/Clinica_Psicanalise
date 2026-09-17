package com.psicogest.psicogest.security.tenant;

import com.psicogest.psicogest.exception.AccessDeniedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Captures a requested tenant for downstream authorization. Services still
 * resolve/apply the context inside their own transaction before touching RLS.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantContextResolver resolver;
    private final TransactionTemplate transactionTemplate;

    public TenantContextFilter(TenantContextResolver resolver,
                               PlatformTransactionManager transactionManager) {
        this.resolver = resolver;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/auth/") || path.startsWith("/health/")
                || path.startsWith("/actuator/health/") || path.startsWith("/webhooks/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requested = request.getHeader("X-Organization-Id");
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                UUID organizationId = requested == null || requested.isBlank()
                        ? null : UUID.fromString(requested);
                transactionTemplate.executeWithoutResult(status -> {
                    TenantContextHolder.set(resolver.resolve(authentication, organizationId));
                    try {
                        filterChain.doFilter(request, response);
                    } catch (IOException | ServletException exception) {
                        throw new FilterInvocationException(exception);
                    }
                });
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Organization-Id inválido");
        } catch (AccessDeniedException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Organização não autorizada");
        } catch (FilterInvocationException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) throw ioException;
            if (cause instanceof ServletException servletException) throw servletException;
            throw exception;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static final class FilterInvocationException extends RuntimeException {
        private FilterInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
