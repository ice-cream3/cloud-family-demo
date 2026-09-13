package com.kfpd.cloud.job.dao.cloud;

import java.time.Instant;
import java.util.List;

import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatDTO;

import org.apache.ibatis.annotations.Param;

public interface ManagerLoginLogStatsDao {

    List<ManagerLoginStatDTO> countManagerLogins(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );
}
