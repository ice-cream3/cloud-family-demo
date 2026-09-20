package com.kfpd.cloud.manager.pojo.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SysMenuRequestVO(
        Long parentId,
        @NotBlank(message = "菜单编码不能为空")
        @Size(max = 64, message = "菜单编码长度不能超过64个字符")
        String menuCode,
        @NotBlank(message = "菜单名称不能为空")
        @Size(max = 100, message = "菜单名称长度不能超过100个字符")
        String menuName,
        @Size(max = 255, message = "路径长度不能超过255个字符")
        String path,
        @Size(max = 255, message = "组件长度不能超过255个字符")
        String component,
        @Size(max = 100, message = "图标长度不能超过100个字符")
        String icon,
        Boolean buttonFlag,
        @Min(value = 0, message = "排序不能小于0")
        Integer sortOrder,
        Boolean visible,
        @Size(max = 32, message = "状态长度不能超过32个字符")
        String status
) {
}
