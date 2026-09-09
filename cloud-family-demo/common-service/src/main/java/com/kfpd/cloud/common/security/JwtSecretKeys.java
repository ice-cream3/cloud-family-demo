package com.kfpd.cloud.common.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class JwtSecretKeys {

    private JwtSecretKeys() {
    }

    public static SecretKey hmacSha256Key(String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }
}
