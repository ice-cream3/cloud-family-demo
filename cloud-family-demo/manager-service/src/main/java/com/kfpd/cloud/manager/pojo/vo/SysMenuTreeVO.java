package com.kfpd.cloud.manager.pojo.vo;

import java.time.LocalDateTime;
import java.util.List;

public record SysMenuTreeVO(
        Long id,
        Long parentId,
        String menuCode,
        String menuName,
        String path,
        String component,
        String icon,
        Integer sortOrder,
        Boolean visible,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SysMenuTreeVO> children
) {
}
