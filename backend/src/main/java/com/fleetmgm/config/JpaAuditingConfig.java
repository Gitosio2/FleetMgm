package com.fleetmgm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Separated from FleetMgmApplication so @WebMvcTest slices exclude it automatically.
// Public (not package-private) so @DataJpaTest classes in other packages can @Import it —
// @DataJpaTest's minimal slice does not component-scan arbitrary @Configuration classes.
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaAuditingConfig {}
