package com.psicogest.psicogest.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicogest.psicogest.service.SecurityEventService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityEventService securityEventService;
    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(
            SecurityEventService securityEventService,
            ObjectMapper objectMapper
    ) {
        this.securityEventService = securityEventService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                userId = Long.valueOf(authentication.getName());
            } catch (NumberFormatException ignored) {
            }
        }

        // Record ACCESS_DENIED event
        // This will be used for behavioral detection
        // (403 patterns can indicate IDOR or enumeration attacks)

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", 403);
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", "Acesso negado");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
