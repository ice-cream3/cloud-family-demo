package com.kfpd.cloud.manager.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.kfpd.cloud.manager.dao.model.LoginTrendDao;
import com.kfpd.cloud.manager.pojo.dto.LoginTrendCountDTO;
import com.kfpd.cloud.manager.pojo.vo.LoginTrendPointVO;
import com.kfpd.cloud.manager.pojo.vo.ManagerDashboardVO;

import org.springframework.stereotype.Service;

@Service
public class ManagerService {

    private static final int LOGIN_TREND_DAYS = 14;

    private final LoginTrendDao loginTrendDao;

    public ManagerService(LoginTrendDao loginTrendDao) {
        this.loginTrendDao = loginTrendDao;
    }

    public ManagerDashboardVO dashboard(String username, String roles, String permissions) {
        return new ManagerDashboardVO(
                username,
                "manager-user",
                splitHeader(roles),
                splitHeader(permissions),
                loginTrend(),
                LocalDateTime.now()
        );
    }

    private List<LoginTrendPointVO> loginTrend() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(LOGIN_TREND_DAYS - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        Map<String, Long> managerCounts = countByDate(loginTrendDao.findManagerLoginTrend(startAt));
        Map<String, Long> partnerCounts = countByDate(loginTrendDao.findPartnerLoginTrend(startAt));
        return IntStream.range(0, LOGIN_TREND_DAYS)
                .mapToObj(startDate::plusDays)
                .map(LocalDate::toString)
                .map(date -> new LoginTrendPointVO(
                        date,
                        managerCounts.getOrDefault(date, 0L),
                        partnerCounts.getOrDefault(date, 0L)
                ))
                .toList();
    }

    private Map<String, Long> countByDate(List<LoginTrendCountDTO> counts) {
        return counts.stream()
                .collect(Collectors.toMap(
                        LoginTrendCountDTO::getLoginDate,
                        item -> item.getLoginCount() == null ? 0L : item.getLoginCount(),
                        Long::sum
                ));
    }

    private List<String> splitHeader(String value) {
        // Gateway serializes roles and permissions as comma-separated headers for this demo.
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
