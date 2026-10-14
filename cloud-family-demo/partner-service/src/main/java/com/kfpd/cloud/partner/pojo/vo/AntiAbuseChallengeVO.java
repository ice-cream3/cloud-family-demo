package com.kfpd.cloud.partner.pojo.vo;

public record AntiAbuseChallengeVO(
        String challengeId,
        String question,
        Integer expiresInSeconds
) {
}
