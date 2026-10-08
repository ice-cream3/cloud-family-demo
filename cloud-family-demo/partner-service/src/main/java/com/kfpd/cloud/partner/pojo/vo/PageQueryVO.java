package com.kfpd.cloud.partner.pojo.vo;

public record PageQueryVO(
        Integer pageNum,
        Integer pageSize,
        String category
) {
}
