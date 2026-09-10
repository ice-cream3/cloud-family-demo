package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class VipUser {

    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String email;
    private String phone;
    private String vipLevel;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
