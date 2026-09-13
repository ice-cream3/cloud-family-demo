package com.kfpd.cloud.auth.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "demo.auth.session")
public class AuthSessionProperties {

    private Store store = Store.JDBC;
    private String redisKeyPrefix = "cloud-family-demo:auth";

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public boolean isRedis() {
        return store == Store.REDIS;
    }

    public enum Store {
        JDBC,
        REDIS
    }
}
