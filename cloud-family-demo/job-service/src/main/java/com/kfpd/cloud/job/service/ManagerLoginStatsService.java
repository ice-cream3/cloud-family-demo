package com.kfpd.cloud.job.service;

import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatsSummaryDTO;

public interface ManagerLoginStatsService {

    void initializeSchema();

    ManagerLoginStatsSummaryDTO calculate10mStats(String jobParam);
}
