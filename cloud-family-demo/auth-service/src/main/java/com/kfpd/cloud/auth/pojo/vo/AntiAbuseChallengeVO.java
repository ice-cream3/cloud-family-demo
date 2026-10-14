package com.kfpd.cloud.auth.pojo.vo;

import java.util.List;

public record AntiAbuseChallengeVO(
        String challengeId,
        String question,
        String questionImage,
        Integer expiresInSeconds,
        List<ChallengeOptionVO> options
) {
    public record ChallengeOptionVO(
            String value,
            ChallengeShapeVO shape,
            String color,
            String fillMode
    ) {
    }

    public record ChallengeShapeVO(
            String value,
            String clipPath
    ) {
    }
}
