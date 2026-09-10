package com.kfpd.cloud.auth.base.config;

import java.time.Duration;
import java.util.Set;

import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import com.kfpd.cloud.common.security.CommonJwtProperties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Configuration
public class AuthOAuth2JdbcConfig {

    public static final String API_CLIENT_ID = "cloud-api-client";
    public static final String MANAGER_CLIENT_ID = "cloud-manager-client";
    public static final AuthorizationGrantType API_LOGIN_GRANT_TYPE = new AuthorizationGrantType("demo_api_login");
    public static final AuthorizationGrantType MANAGER_LOGIN_GRANT_TYPE = new AuthorizationGrantType("demo_manager_login");

    @Bean
    RegisteredClientRepository registeredClientRepository(
            @Qualifier(MultiDataSourceNames.FA_CLOUD_JDBC_TEMPLATE) JdbcOperations jdbcOperations) {
        return new JdbcRegisteredClientRepository(jdbcOperations);
    }

    @Bean
    OAuth2AuthorizationService oAuth2AuthorizationService(
            @Qualifier(MultiDataSourceNames.FA_CLOUD_JDBC_TEMPLATE) JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    ApplicationRunner registeredClientInitializer(RegisteredClientRepository registeredClientRepository,
                                                  CommonJwtProperties jwtProperties) {
        return args -> {
            saveIfMissing(registeredClientRepository, apiClient(jwtProperties));
            saveIfMissing(registeredClientRepository, managerClient(jwtProperties));
        };
    }

    private void saveIfMissing(RegisteredClientRepository repository, RegisteredClient registeredClient) {
        if (repository.findByClientId(registeredClient.getClientId()) == null) {
            repository.save(registeredClient);
        }
    }

    private RegisteredClient apiClient(CommonJwtProperties jwtProperties) {
        return registeredClient(
                "cloud-api-client-registration",
                API_CLIENT_ID,
                "Cloud API Login Client",
                API_LOGIN_GRANT_TYPE,
                Set.of("user:profile:read"),
                jwtProperties
        );
    }

    private RegisteredClient managerClient(CommonJwtProperties jwtProperties) {
        return registeredClient(
                "cloud-manager-client-registration",
                MANAGER_CLIENT_ID,
                "Cloud Manager Login Client",
                MANAGER_LOGIN_GRANT_TYPE,
                Set.of("manager:login", "manager:dashboard:read", "user:profile:read"),
                jwtProperties
        );
    }

    private RegisteredClient registeredClient(String id,
                                              String clientId,
                                              String clientName,
                                              AuthorizationGrantType grantType,
                                              Set<String> scopes,
                                              CommonJwtProperties jwtProperties) {
        return RegisteredClient.withId(id)
                .clientId(clientId)
                .clientName(clientName)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(grantType)
                .scopes(registeredScopes -> registeredScopes.addAll(scopes))
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(jwtProperties.getTtlSeconds()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(jwtProperties.getRefreshTtlSeconds()))
                        .reuseRefreshTokens(true)
                        .build())
                .build();
    }
}
