package com.kfpd.cloud.partner.pojo.vo;

public record AppVersionVO(
        String versionName,
        Boolean latest,
        String releaseNote
) {
}
