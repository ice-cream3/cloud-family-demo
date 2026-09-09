package com.kfpd.cloud.auth.base.config;

import java.nio.charset.StandardCharsets;

import com.kfpd.cloud.common.security.CommonJwtProperties;
import com.kfpd.cloud.common.security.JwtSecretKeys;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(CommonJwtProperties.class)
public class AuthJwtConfig {

    @Bean
    JwtEncoder jwtEncoder(CommonJwtProperties properties) {
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        return new NimbusJwtEncoder(new ImmutableSecret<>(secret));
    }

    @Bean
    JwtDecoder jwtDecoder(CommonJwtProperties properties) {
        return NimbusJwtDecoder.withSecretKey(JwtSecretKeys.hmacSha256Key(properties.getSecret()))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
