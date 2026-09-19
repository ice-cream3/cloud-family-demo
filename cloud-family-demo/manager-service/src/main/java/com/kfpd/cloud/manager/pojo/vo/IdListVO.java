package com.kfpd.cloud.manager.pojo.vo;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record IdListVO(
        @NotNull(message = "ID列表不能为空")
        List<Long> ids
) {
}
