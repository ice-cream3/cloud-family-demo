package com.kfpd.cloud.partner.pojo.vo;

public record TripOwnerVO(
        String ownerUsername,
        String ownerDisplayName,
        Long slotCount,
        String nextDate,
        String nextTime
) {
}
