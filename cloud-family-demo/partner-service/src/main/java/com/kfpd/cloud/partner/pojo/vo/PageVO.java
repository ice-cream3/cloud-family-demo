package com.kfpd.cloud.partner.pojo.vo;

import java.util.List;

public record PageVO<T>(
        long total,
        int pageNum,
        int pageSize,
        List<T> records
) {
}
