package com.kfpd.cloud.manager.pojo.vo;

public record SysMenuRequestVO(
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
