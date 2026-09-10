package com.psicogest.psicogest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 38. Configuração de Clock injetável
 * 
 * Permite testes temporais sem mockar LocalDate.now()
 * 
 * Uso:
 * LocalDate today = LocalDate.now(clock);
 * Instant now = clock.instant();
 */
@Configuration
public class TimeConfiguration {

    /**
     * Clock UTC para operações temporais
     * 
     * Em testes, pode ser substituído por Clock.fixed()
     */
    @Bean
    public Clock clock() {

        return Clock.systemUTC();
    }
}
