package com.kfpd.cloud.job.base.config;

import com.kfpd.cloud.job.service.ManagerLoginStatsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JobSchemaProperties.class)
public class JobSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(JobSchemaInitializer.class);

    @Bean
    ApplicationRunner faModelJobSchemaInitializer(JobSchemaProperties properties,
                                                 ManagerLoginStatsService managerLoginStatsService) {
        return args -> {
            if (!properties.isInitialize()) {
                log.info("Job schema initialization skipped");
                return;
            }
            managerLoginStatsService.initializeSchema();
            log.info("Job schema initialized: table=manager_login_10m_stats");
        };
    }
}
