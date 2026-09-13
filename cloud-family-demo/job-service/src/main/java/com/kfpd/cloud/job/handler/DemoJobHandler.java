package com.kfpd.cloud.job.handler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DemoJobHandler.class);

    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("XXL-JOB demo handler started: param={}", jobParam);
        XxlJobHelper.log("XXL-JOB demo handler started, param={0}", jobParam);

        // Put scheduled business logic here.

        XxlJobHelper.log("XXL-JOB demo handler completed");
        log.info("XXL-JOB demo handler completed");
    }
}
