package com.kfpd.cloud.job.pojo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户行程预约时段，对应 fa-cloud.user_trip_slot 表。
 */
@Data
@TableName("user_trip_slot")
public class UserTripSlot {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发布人账号。
     */
    @TableField("owner_username")
    private String ownerUsername;

    /**
     * 发布人显示名称。
     */
    @TableField("owner_display_name")
    private String ownerDisplayName;

    /**
     * 行程标题。
     */
    @TableField("title")
    private String title;

    /**
     * 行程地点。
     */
    @TableField("place")
    private String place;

    /**
     * 行程日期。
     */
    @TableField("trip_date")
    private LocalDate tripDate;

    /**
     * 开始时间。
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 结束时间。
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 行程状态：OPEN 可预约、PENDING 待审核、BOOKED 已预约、NO_SHOW 未履约、EXPIRED 已过期。
     */
    @TableField("status")
    private String status;

    /**
     * 预约人账号。
     */
    @TableField("applicant_username")
    private String applicantUsername;

    /**
     * 预约人显示名称。
     */
    @TableField("applicant_display_name")
    private String applicantDisplayName;

    /**
     * 预约申请备注。
     */
    @TableField("apply_note")
    private String applyNote;

    /**
     * 预约申请时间。
     */
    @TableField("applied_at")
    private LocalDateTime appliedAt;

    /**
     * 审核时间。
     */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
