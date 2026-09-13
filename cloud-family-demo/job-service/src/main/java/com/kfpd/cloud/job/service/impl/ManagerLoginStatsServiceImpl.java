package com.kfpd.cloud.job.service.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.job.dao.cloud.ManagerLoginAuthStatsDao;
import com.kfpd.cloud.job.dao.model.ManagerLoginStatsDao;
import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatDTO;
import com.kfpd.cloud.job.pojo.dto.ManagerLoginStatsSummaryDTO;
import com.kfpd.cloud.job.service.ManagerLoginStatsService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerLoginStatsServiceImpl implements ManagerLoginStatsService {

    private static final String MANAGER_LOGIN_GRANT_TYPE = "demo_manager_login";

    private final ManagerLoginAuthStatsDao managerLoginAuthStatsDao;
    private final ManagerLoginStatsDao managerLoginStatsDao;

    public ManagerLoginStatsServiceImpl(ManagerLoginAuthStatsDao managerLoginAuthStatsDao,
                                        ManagerLoginStatsDao managerLoginStatsDao) {
        this.managerLoginAuthStatsDao = managerLoginAuthStatsDao;
        this.managerLoginStatsDao = managerLoginStatsDao;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_MODEL_TRANSACTION_MANAGER)
    public ManagerLoginStatsSummaryDTO calculate10mStats(String jobParam) {
        TimeWindow window = resolveWindow(jobParam);
        List<ManagerLoginStatDTO> rows = managerLoginAuthStatsDao.countManagerLogins(
                MANAGER_LOGIN_GRANT_TYPE,
                window.start(),
                window.end()
        );
        int savedCount = saveStats(window, rows);
        return new ManagerLoginStatsSummaryDTO(window.start(), window.end(), rows.size(), savedCount);
    }

    private int saveStats(TimeWindow window, List<ManagerLoginStatDTO> rows) {
        int savedCount = 0;
        Instant calculatedAt = Instant.now();
        for (ManagerLoginStatDTO row : rows) {
            savedCount += managerLoginStatsDao.upsertManagerLoginStat(window.start(), window.end(), row, calculatedAt);
        }
        return savedCount;
    }

    private TimeWindow resolveWindow(String jobParam) {
        Instant windowEnd = (jobParam == null || jobParam.isBlank())
                ? previousTenMinuteBoundary(Instant.now())
                : parseWindowEnd(jobParam.trim());
        return new TimeWindow(windowEnd.minus(10, ChronoUnit.MINUTES), windowEnd);
    }

    private Instant previousTenMinuteBoundary(Instant now) {
        ZonedDateTime utc = now.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
        int minute = utc.getMinute();
        int boundaryMinute = minute - minute % 10;
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
