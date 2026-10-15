package com.kfpd.cloud.auth.pojo.vo;

import java.util.List;

public record AntiAbuseChallengeVO(
        String challengeId,
        String question,
        String questionImage,
        Integer expiresInSeconds,
        String scheme,
        Integer requiredSelectionCount,
        List<ChallengeOptionVO> options
) {
    public record ChallengeOptionVO(
            String value,
            ChallengeShapeVO shape,
            String color,
            String fillMode,
            String image
    ) {
    }

    public record ChallengeShapeVO(
            String value,
            String clipPath
    ) {
    }
}
