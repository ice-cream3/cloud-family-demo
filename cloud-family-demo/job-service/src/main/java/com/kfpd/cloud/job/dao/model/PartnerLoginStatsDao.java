package com.kfpd.cloud.job.dao.model;

import java.time.Instant;

import com.kfpd.cloud.job.pojo.dto.PartnerLoginStatDTO;

import org.apache.ibatis.annotations.Param;

public interface PartnerLoginStatsDao {

    int upsertPartnerLoginStat(@Param("windowStart") Instant windowStart,
                               @Param("windowEnd") Instant windowEnd,
                               @Param("stat") PartnerLoginStatDTO stat,
                               @Param("calculatedAt") Instant calculatedAt);
}
