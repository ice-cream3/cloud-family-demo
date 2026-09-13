package com.kfpd.cloud.job.dao.cloud;

import java.time.Instant;
import java.util.List;

import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatDTO;

import org.apache.ibatis.annotations.Param;

public interface ManagerLoginAuthStatsDao {

    List<ManagerLoginStatDTO> countManagerLogins(
            @Param("grantType") String grantType,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd
    );
}
