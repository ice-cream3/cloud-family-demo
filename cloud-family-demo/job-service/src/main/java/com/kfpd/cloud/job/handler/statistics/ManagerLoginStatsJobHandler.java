package com.kfpd.cloud.job.handler.statistics;

import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatsSummaryDTO;
import com.kfpd.cloud.job.service.ManagerLoginStatsService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ManagerLoginStatsJobHandler {

    private static final Logger log = LoggerFactory.getLogger(ManagerLoginStatsJobHandler.class);

    private final ManagerLoginStatsService managerLoginStatsService;

    public ManagerLoginStatsJobHandler(ManagerLoginStatsService managerLoginStatsService) {
        this.managerLoginStatsService = managerLoginStatsService;
    }

    @XxlJob("managerLoginStats10mJobHandler")
    public void managerLoginStats10mJobHandler() {
        ManagerLoginStatsSummaryDTO summary;
        try {
            summary = managerLoginStatsService.calculate10mStats(XxlJobHelper.getJobParam());
        } catch (Exception e) {
            log.error("统计管理端用户失败,e:{}", e.getMessage(), e);
            XxlJobHelper.handleFail("统计管理端用户失败.");
            return;
        }

        log.info("Manager login stats completed: windowStart={}, windowEnd={}, rowCount={}, savedCount={}",
                summary.windowStart(), summary.windowEnd(), summary.rowCount(), summary.savedCount());
        XxlJobHelper.log("Manager login stats completed, windowStart={0}, windowEnd={1}, rowCount={2}, savedCount={3}",
                summary.windowStart(), summary.windowEnd(), summary.rowCount(), summary.savedCount());
    }
}
