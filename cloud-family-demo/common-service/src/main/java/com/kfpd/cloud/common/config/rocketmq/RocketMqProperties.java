package com.kfpd.cloud.common.config.rocketmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.rocketmq")
public class RocketMqProperties {

    /**
     * Disabled by default so services can start when local RocketMQ is not running.
     */
    private boolean enabled = false;

    private String namesrvAddr = "localhost:9876";

    private String producerGroup = "cloud-family-demo-producer";

    private int sendMessageTimeoutMillis = 3000;

    private int retryTimesWhenSendFailed = 2;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public String getProducerGroup() {
        return producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public int getSendMessageTimeoutMillis() {
        return sendMessageTimeoutMillis;
    }

    public void setSendMessageTimeoutMillis(int sendMessageTimeoutMillis) {
        this.sendMessageTimeoutMillis = sendMessageTimeoutMillis;
    }

    public int getRetryTimesWhenSendFailed() {
        return retryTimesWhenSendFailed;
    }

    public void setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed) {
        this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
    }
}
