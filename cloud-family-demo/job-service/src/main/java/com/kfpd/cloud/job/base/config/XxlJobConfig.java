package com.kfpd.cloud.job.base.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobConfig {

    @Bean
    @ConditionalOnProperty(prefix = "demo.xxl-job", name = "enabled", havingValue = "true", matchIfMissing = true)
    XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties properties) {
        XxlJobProperties.Executor executorProperties = properties.getExecutor();
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAccessToken(properties.getAccessToken());
        executor.setAppname(executorProperties.getAppName());
        executor.setAddress(executorProperties.getAddress());
        executor.setIp(executorProperties.getIp());
        executor.setPort(executorProperties.getPort());
        executor.setLogPath(executorProperties.getLogPath());
        executor.setLogRetentionDays(executorProperties.getLogRetentionDays());
        return executor;
    }
}
