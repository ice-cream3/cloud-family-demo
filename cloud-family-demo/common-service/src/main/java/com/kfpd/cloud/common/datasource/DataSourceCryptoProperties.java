package com.kfpd.cloud.common.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.datasource.crypto")
public class DataSourceCryptoProperties {

    private String secret = "cloud-family-demo-datasource-crypto-secret";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
