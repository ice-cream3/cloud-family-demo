package com.kfpd.cloud.job.handler.statistics;

import com.kfpd.cloud.job.pojo.dto.PartnerLoginStatsSummaryDTO;
import com.kfpd.cloud.job.service.PartnerLoginStatsService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PartnerLoginStatsJobHandler {

    private static final Logger log = LoggerFactory.getLogger(PartnerLoginStatsJobHandler.class);

    private final PartnerLoginStatsService partnerLoginStatsService;

    public PartnerLoginStatsJobHandler(PartnerLoginStatsService partnerLoginStatsService) {
        this.partnerLoginStatsService = partnerLoginStatsService;
    }

    @XxlJob("partnerLoginStats5mJobHandler")
    public void partnerLoginStats5mJobHandler() {
        PartnerLoginStatsSummaryDTO summary;
        try {
            summary = partnerLoginStatsService.calculate5mStats(XxlJobHelper.getJobParam());
        } catch (Exception e) {
            log.error("统计 partner 用户登录失败,e:{}", e.getMessage(), e);
            XxlJobHelper.handleFail("统计 partner 用户登录失败.");
            return;
        }

        log.info("Partner login stats completed: windowStart={}, windowEnd={}, rowCount={}, savedCount={}",
                summary.windowStart(), summary.windowEnd(), summary.rowCount(), summary.savedCount());
        XxlJobHelper.log("Partner login stats completed, windowStart={0}, windowEnd={1}, rowCount={2}, savedCount={3}",
                summary.windowStart(), summary.windowEnd(), summary.rowCount(), summary.savedCount());
    }
}
