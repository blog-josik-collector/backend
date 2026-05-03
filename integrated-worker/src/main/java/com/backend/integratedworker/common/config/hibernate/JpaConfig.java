package com.backend.integratedworker.common.config.hibernate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider") // JPA Auditing 활성화
@EnableJpaRepositories(basePackages = {
        "com.backend.integratedworker.collectingjob.repository"
})
@EntityScan(basePackages = "com.backend.commondataaccess.persistence")
public class JpaConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
