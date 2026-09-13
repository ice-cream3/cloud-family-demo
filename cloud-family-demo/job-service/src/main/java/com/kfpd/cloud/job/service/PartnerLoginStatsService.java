package com.kfpd.cloud.job.service;

import com.kfpd.cloud.job.pojo.dto.PartnerLoginStatsSummaryDTO;

public interface PartnerLoginStatsService {

    PartnerLoginStatsSummaryDTO calculate5mStats(String jobParam);
}
