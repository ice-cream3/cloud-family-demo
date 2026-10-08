package com.kfpd.cloud.partner.pojo.vo;

public record NotificationSettingsVO(
        Boolean systemEnabled,
        Boolean activityEnabled,
        Boolean taskEnabled
) {
}
