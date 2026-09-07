package com.psicogest.psicogest.security.request;

import com.psicogest.psicogest.infrastructure.security.SecurityHashService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class SecurityRequestContextFactory {

    private final SecurityHashService hashService;

    public SecurityRequestContextFactory(
            SecurityHashService hashService
    ) {
        this.hashService = hashService;
    }

    public SecurityRequestContext from(
            HttpServletRequest request
    ) {
        String userAgent = request.getHeader("User-Agent");

        return new SecurityRequestContext(
                request.getRemoteAddr(),
                userAgent != null
                        ? hashService.sha256(userAgent)
                        : null,
                request.getMethod(),
                request.getRequestURI()
        );
    }
}
