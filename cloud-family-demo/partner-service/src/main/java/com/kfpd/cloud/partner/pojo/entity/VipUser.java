package com.kfpd.cloud.partner.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("vip_user")
public class VipUser {

    @TableId(type = IdType.AUTO)
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
