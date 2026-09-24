package com.kfpd.cloud.manager.base.config;

import com.kfpd.cloud.manager.base.security.GatewayAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableMethodSecurity
public class ManagerSecurityConfig {

    @Bean
    SecurityFilterChain managerSecurityFilterChain(HttpSecurity http,
                                                   GatewayAuthenticationFilter gatewayAuthenticationFilter) throws Exception {
        return http
                .securityMatcher("/api/manager/**", "/actuator/health")
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(gatewayAuthenticationFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/api/manager/health").permitAll()
                        .anyRequest().permitAll()
                )
                .build();
    }
}
