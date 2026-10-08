package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDateTime;

public record AppDocumentVO(
        String documentType,
        String title,
        String content,
        LocalDateTime updatedAt
) {
}
