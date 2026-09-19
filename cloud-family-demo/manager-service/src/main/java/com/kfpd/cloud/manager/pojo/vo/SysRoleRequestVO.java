package com.kfpd.cloud.manager.pojo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SysRoleRequestVO(
        @NotBlank(message = "角色编码不能为空")
        @Size(max = 64, message = "角色编码长度不能超过64个字符")
        String roleCode,
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 100, message = "角色名称长度不能超过100个字符")
        String roleName,
        @Size(max = 500, message = "描述长度不能超过500个字符")
        String description,
        @Size(max = 32, message = "状态长度不能超过32个字符")
        String status
) {
}
