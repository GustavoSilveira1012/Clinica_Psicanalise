package com.psicogest.psicogest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Registers scheduling support. Each job has its own fail-closed activation
 * property so enabling clinical-export retention cannot activate unrelated
 * payment or subscription jobs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
