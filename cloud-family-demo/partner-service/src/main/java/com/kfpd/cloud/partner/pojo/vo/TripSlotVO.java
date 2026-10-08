package com.kfpd.cloud.partner.pojo.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TripSlotVO(
        Long id,
        String ownerUsername,
        String ownerDisplayName,
        String title,
        String place,
        LocalDate tripDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String applicantUsername,
        String applicantDisplayName,
        String applyNote,
        LocalDateTime appliedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
