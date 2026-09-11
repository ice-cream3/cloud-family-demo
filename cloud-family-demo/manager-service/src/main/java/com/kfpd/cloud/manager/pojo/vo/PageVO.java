package com.kfpd.cloud.manager.pojo.vo;

import java.util.List;

public record PageVO<T>(
        long total,
        int pageNum,
        int pageSize,
        List<T> records
) {
}
