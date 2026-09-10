package com.kfpd.cloud.manager.pojo.dto;

public record SysMenuRequestDTO(
        Long parentId,
        String menuCode,
        String menuName,
        String path,
        String component,
        String icon,
        Integer sortOrder,
        Boolean visible,
        String status
) {
}
