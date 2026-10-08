package com.kfpd.cloud.partner.pojo.vo;

public record TripApplyRequestVO(
        Long slotId,
        String applyNote,
        Boolean confirmConflict
) {
}
