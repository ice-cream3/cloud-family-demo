package com.kfpd.cloud.job.handler.trip;

import com.kfpd.cloud.job.service.TripExpirationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TripExpirationJobHandler {

    private static final Logger log = LoggerFactory.getLogger(TripExpirationJobHandler.class);

    private final TripExpirationService tripExpirationService;

    public TripExpirationJobHandler(TripExpirationService tripExpirationService) {
        this.tripExpirationService = tripExpirationService;
    }

    @XxlJob("tripExpirationJobHandler")
    public void tripExpirationJobHandler() {
        try {
            int updatedCount = tripExpirationService.expireTrips();
            log.info("Trip expiration completed: updatedCount={}", updatedCount);
            XxlJobHelper.log("Trip expiration completed, updatedCount={0}", updatedCount);
        } catch (Exception ex) {
            log.error("Trip expiration failed", ex);
            XxlJobHelper.handleFail("Trip expiration failed; see executor logs");
        }
    }
}
