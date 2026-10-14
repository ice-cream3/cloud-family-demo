package com.kfpd.cloud.partner.pojo.vo;

public record EmailVerificationCodeVO(
        String email,
        Integer expiresInSeconds
) {
}
