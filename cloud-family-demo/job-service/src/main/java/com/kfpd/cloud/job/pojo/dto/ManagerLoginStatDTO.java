package com.kfpd.cloud.job.pojo.dto;

import java.time.Instant;

public class ManagerLoginStatDTO {

    private String username;
    private long loginCount;
    private Instant firstLoginAt;
    private Instant lastLoginAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(long loginCount) {
        this.loginCount = loginCount;
    }

    public Instant getFirstLoginAt() {
        return firstLoginAt;
    }

    public void setFirstLoginAt(Instant firstLoginAt) {
        this.firstLoginAt = firstLoginAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
