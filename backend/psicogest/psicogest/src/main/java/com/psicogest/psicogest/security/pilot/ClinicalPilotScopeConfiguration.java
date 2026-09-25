package com.psicogest.psicogest.security.pilot;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ClinicalPilotScopeConfiguration implements WebMvcConfigurer {

    private final ClinicalPilotScopeGuard guard;

    public ClinicalPilotScopeConfiguration(ClinicalPilotScopeGuard guard) {
        this.guard = guard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guard);
    }
}
