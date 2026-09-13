package com.kfpd.cloud.xxljobadmin.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.xxl-job-admin")
public class XxlJobAdminProperties {

    private String accessToken = "default_token";
    private int executorTimeoutSeconds = 10;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getExecutorTimeoutSeconds() {
        return executorTimeoutSeconds;
    }

    public void setExecutorTimeoutSeconds(int executorTimeoutSeconds) {
        this.executorTimeoutSeconds = executorTimeoutSeconds;
    }
}
