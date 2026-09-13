package com.kfpd.cloud.job.service.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.job.dao.cloud.PartnerLoginLogStatsDao;
import com.kfpd.cloud.job.dao.model.PartnerLoginStatsDao;
import com.kfpd.cloud.job.pojo.dto.PartnerLoginStatDTO;
import com.kfpd.cloud.job.pojo.dto.PartnerLoginStatsSummaryDTO;
import com.kfpd.cloud.job.service.PartnerLoginStatsService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerLoginStatsServiceImpl implements PartnerLoginStatsService {

    private final PartnerLoginLogStatsDao partnerLoginLogStatsDao;
    private final PartnerLoginStatsDao partnerLoginStatsDao;

    public PartnerLoginStatsServiceImpl(PartnerLoginLogStatsDao partnerLoginLogStatsDao,
                                        PartnerLoginStatsDao partnerLoginStatsDao) {
        this.partnerLoginLogStatsDao = partnerLoginLogStatsDao;
        this.partnerLoginStatsDao = partnerLoginStatsDao;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
    public PartnerLoginStatsSummaryDTO calculate5mStats(String jobParam) {
        TimeWindow window = resolveWindow(jobParam);
        List<PartnerLoginStatDTO> rows = partnerLoginLogStatsDao.countPartnerLogins(window.start(), window.end());
        int savedCount = saveStats(window, rows);
        return new PartnerLoginStatsSummaryDTO(window.start(), window.end(), rows.size(), savedCount);
    }

    private int saveStats(TimeWindow window, List<PartnerLoginStatDTO> rows) {
        int savedCount = 0;
        Instant calculatedAt = Instant.now();
        for (PartnerLoginStatDTO row : rows) {
            savedCount += partnerLoginStatsDao.upsertPartnerLoginStat(window.start(), window.end(), row, calculatedAt);
        }
        return savedCount;
    }

    private TimeWindow resolveWindow(String jobParam) {
        Instant windowEnd = (jobParam == null || jobParam.isBlank())
                ? previousFiveMinuteBoundary(Instant.now())
                : parseWindowEnd(jobParam.trim());
        return new TimeWindow(windowEnd.minus(5, ChronoUnit.MINUTES), windowEnd);
    }

    private Instant previousFiveMinuteBoundary(Instant now) {
        ZonedDateTime utc = now.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
        int minute = utc.getMinute();
        int boundaryMinute = minute - minute % 5;
        ZonedDateTime currentBoundary = utc.withMinute(boundaryMinute).withSecond(0).withNano(0);
        return currentBoundary.toInstant();
    }

    private Instant parseWindowEnd(String jobParam) {
        try {
            return Instant.parse(jobParam);
        } catch (RuntimeException ignored) {
            return ZonedDateTime.parse(jobParam).toInstant();
        }
    }

    private record TimeWindow(Instant start, Instant end) {
    }
}
