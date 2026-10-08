package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDateTime;

public record MembershipVO(
        String username,
        String planName,
        String status,
        LocalDateTime expireAt,
        String benefits
) {
}
