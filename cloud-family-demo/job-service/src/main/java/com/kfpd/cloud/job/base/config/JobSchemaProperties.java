package com.kfpd.cloud.job.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.job.schema")
public class JobSchemaProperties {

    private boolean initialize = true;

    public boolean isInitialize() {
        return initialize;
    }

    public void setInitialize(boolean initialize) {
        this.initialize = initialize;
    }
}
