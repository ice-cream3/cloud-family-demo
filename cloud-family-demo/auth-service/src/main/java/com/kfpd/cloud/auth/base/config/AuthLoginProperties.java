package com.kfpd.cloud.auth.base.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "demo.auth.login")
public class AuthLoginProperties {

    // Bound from auth-service application.yml: demo.auth.login.api.*
    private Api api = new Api();
    // Bound from auth-service application.yml: demo.auth.login.manager.*
    private Manager manager = new Manager();
    public enum ChallengeType {
        GRAPHIC_SET,
        GRAPHIC_SEQUENCE,
        GRAPHIC_PAIR,
        IMAGE_REASSEMBLE,
        IMAGE_SLIDER
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public static class Api {

        // Empty enables all schemes for API login and registration.
        private Set<ChallengeType> enabledChallengeSchemes = Set.of();

        public Set<ChallengeType> getEnabledChallengeSchemes() {
            return enabledChallengeSchemes;
        }

        public void setEnabledChallengeSchemes(Set<ChallengeType> enabledChallengeSchemes) {
            this.enabledChallengeSchemes = enabledChallengeSchemes == null ? Set.of() : Set.copyOf(enabledChallengeSchemes);
        }

        // Number of failed API login attempts allowed before temporary lock.
        private int maxFailedAttempts = 5;
        // Lock duration in seconds after the failed attempt threshold is reached.
        private long lockSeconds = 300;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public long getLockSeconds() {
            return lockSeconds;
        }

        public void setLockSeconds(long lockSeconds) {
            this.lockSeconds = lockSeconds;
        }
    }

    public static class Manager {

        // Empty enables all schemes for manager login, independently of the API configuration.
        private Set<ChallengeType> enabledChallengeSchemes = Set.of();

        public Set<ChallengeType> getEnabledChallengeSchemes() {
            return enabledChallengeSchemes;
        }

        public void setEnabledChallengeSchemes(Set<ChallengeType> enabledChallengeSchemes) {
            this.enabledChallengeSchemes = enabledChallengeSchemes == null ? Set.of() : Set.copyOf(enabledChallengeSchemes);
        }

        // Manager login is allowed only from these source IPs unless "*" is configured.
        private Set<String> allowedIps = new HashSet<>(Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));

        public Set<String> getAllowedIps() {
            return allowedIps;
        }

        public void setAllowedIps(Set<String> allowedIps) {
            this.allowedIps = allowedIps;
        }
    }
}
