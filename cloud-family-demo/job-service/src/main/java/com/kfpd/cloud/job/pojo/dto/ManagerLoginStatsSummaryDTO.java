package com.kfpd.cloud.job.pojo.dto;

import java.time.Instant;

public record ManagerLoginStatsSummaryDTO(
        Instant windowStart,
        Instant windowEnd,
        int rowCount,
        int savedCount
) {
}
