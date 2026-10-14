package com.kfpd.cloud.partner.pojo.vo;

public record TripCreditVO(
        Integer creditScore,
        String creditLevel,
        Integer minReservableScore,
        Integer noShowPenalty,
        Boolean reservable
) {
}
