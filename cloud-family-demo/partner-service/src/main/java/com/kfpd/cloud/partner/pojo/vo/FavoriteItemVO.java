package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDateTime;

public record FavoriteItemVO(
        Long id,
        String itemType,
        String title,
        String description,
        String iconTone,
        LocalDateTime createdAt
) {
}
