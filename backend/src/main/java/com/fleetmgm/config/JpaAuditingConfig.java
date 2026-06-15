package com.fleetmgm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Separated from FleetMgmApplication so @WebMvcTest slices exclude it automatically
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
class JpaAuditingConfig {}
