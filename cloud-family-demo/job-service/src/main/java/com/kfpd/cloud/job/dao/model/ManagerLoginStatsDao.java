package com.kfpd.cloud.job.dao.model;

import java.time.Instant;

import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatDTO;

import org.apache.ibatis.annotations.Param;

public interface ManagerLoginStatsDao {

    int upsertManagerLoginStat(@Param("windowStart") Instant windowStart,
                               @Param("windowEnd") Instant windowEnd,
                               @Param("stat") ManagerLoginStatDTO stat,
                               @Param("calculatedAt") Instant calculatedAt);
}
