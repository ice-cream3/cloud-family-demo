package com.kfpd.cloud.job.handler;

import java.util.List;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DemoJobHandler.class);
    private static final List<Long> DEMO_BUSINESS_IDS = List.of(1001L, 1002L, 1003L, 1004L, 1005L, 1006L);

    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("XXL-JOB demo handler started: param={}", jobParam);
        XxlJobHelper.log("XXL-JOB demo handler started, param={0}", jobParam);

        // Put scheduled business logic here.

        XxlJobHelper.log("XXL-JOB demo handler completed");
        log.info("XXL-JOB demo handler completed");
    }

    @XxlJob("paramDemoJobHandler")
    public void paramDemoJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("XXL-JOB param demo started: rawParam={}", jobParam);
        XxlJobHelper.log("XXL-JOB param demo started, rawParam={0}", jobParam);

        if (jobParam == null || jobParam.isBlank()) {
            XxlJobHelper.log("No job param configured");
            log.info("XXL-JOB param demo completed: no param");
            return;
        }

        for (String item : jobParam.split(",")) {
            String value = item.trim();
            if (value.isEmpty()) {
                continue;
            }
            log.info("Processing param item: value={}", value);
            XxlJobHelper.log("Processing param item, value={0}", value);
        }

        XxlJobHelper.log("XXL-JOB param demo completed");
        log.info("XXL-JOB param demo completed");
    }

    @XxlJob("shardingDemoJobHandler")
    public void shardingDemoJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = Math.max(1, XxlJobHelper.getShardTotal());
        String jobParam = XxlJobHelper.getJobParam();
        log.info("XXL-JOB sharding demo started: shardIndex={}, shardTotal={}, param={}",
                shardIndex, shardTotal, jobParam);
        XxlJobHelper.log("XXL-JOB sharding demo started, shardIndex={0}, shardTotal={1}, param={2}",
                shardIndex, shardTotal, jobParam);

        for (Long businessId : DEMO_BUSINESS_IDS) {
            if (businessId % shardTotal != shardIndex) {
                continue;
            }
            processShardBusiness(businessId);
        }

        XxlJobHelper.log("XXL-JOB sharding demo completed, shardIndex={0}, shardTotal={1}", shardIndex, shardTotal);
        log.info("XXL-JOB sharding demo completed: shardIndex={}, shardTotal={}", shardIndex, shardTotal);
    }

    private void processShardBusiness(Long businessId) {
        log.info("Processing sharding demo business: businessId={}", businessId);
        XxlJobHelper.log("Processing sharding demo business, businessId={0}", businessId);
    }
}
