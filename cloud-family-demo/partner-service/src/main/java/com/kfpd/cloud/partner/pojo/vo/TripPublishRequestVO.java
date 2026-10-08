package com.kfpd.cloud.partner.pojo.vo;

public record TripPublishRequestVO(
        String title,
        String place,
        String tripDate,
        String startTime,
        String endTime
) {
}
