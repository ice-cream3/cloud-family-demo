package com.kfpd.cloud.partner.pojo.vo;

public record NotificationSettingsUpdateVO(
        Boolean systemEnabled,
        Boolean activityEnabled,
        Boolean taskEnabled
) {
}
