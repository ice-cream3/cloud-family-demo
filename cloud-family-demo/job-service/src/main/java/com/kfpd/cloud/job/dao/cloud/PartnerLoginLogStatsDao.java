package com.kfpd.cloud.job.dao.cloud;

import java.time.Instant;
import java.util.List;

import com.kfpd.cloud.job.pojo.dto.PartnerLoginStatDTO;

import org.apache.ibatis.annotations.Param;

public interface PartnerLoginLogStatsDao {

    List<PartnerLoginStatDTO> countPartnerLogins(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );
}
