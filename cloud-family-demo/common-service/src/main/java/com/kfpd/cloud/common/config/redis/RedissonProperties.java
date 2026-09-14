package com.kfpd.cloud.common.config.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.redisson.lock")
public class RedissonProperties {

    private String keyPrefix = "cloud-family-demo:lock";
    private long waitTimeSeconds = 5;
    private long leaseTimeSeconds = 30;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    public void setWaitTimeSeconds(long waitTimeSeconds) {
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public long getLeaseTimeSeconds() {
        return leaseTimeSeconds;
    }

    public void setLeaseTimeSeconds(long leaseTimeSeconds) {
        this.leaseTimeSeconds = leaseTimeSeconds;
    }
}
