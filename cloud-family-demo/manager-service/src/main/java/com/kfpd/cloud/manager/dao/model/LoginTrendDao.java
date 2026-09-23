package com.kfpd.cloud.manager.dao.model;

import java.time.LocalDateTime;
import java.util.List;

import com.kfpd.cloud.manager.pojo.dto.LoginTrendCountDTO;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LoginTrendDao {

    @Select("""
            SELECT DATE_FORMAT(login_at, '%Y-%m-%d') AS loginDate,
                   COUNT(*) AS loginCount
            FROM manager_login_log
            WHERE login_result = 'SUCCESS'
              AND login_at >= #{startAt}
            GROUP BY DATE_FORMAT(login_at, '%Y-%m-%d')
            ORDER BY loginDate ASC
            """)
    List<LoginTrendCountDTO> findManagerLoginTrend(@Param("startAt") LocalDateTime startAt);

    @Select("""
            SELECT DATE(login_at) AS loginDate,
                   COUNT(*) AS loginCount
            FROM partner_login_log
            WHERE login_result = 'SUCCESS'
              AND login_at >= #{startAt}
            GROUP BY DATE(login_at)
            ORDER BY loginDate ASC
            """)
    List<LoginTrendCountDTO> findPartnerLoginTrend(@Param("startAt") LocalDateTime startAt);
}
