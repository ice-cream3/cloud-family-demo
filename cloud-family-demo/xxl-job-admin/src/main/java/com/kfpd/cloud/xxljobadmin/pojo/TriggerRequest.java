package com.kfpd.cloud.xxljobadmin.pojo;

public record TriggerRequest(
        String appName,
        String handler,
        String param,
        boolean shardingBroadcast
) {
}
