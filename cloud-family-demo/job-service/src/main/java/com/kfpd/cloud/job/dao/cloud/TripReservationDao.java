package com.kfpd.cloud.job.dao.cloud;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TripReservationDao {

    private final JdbcTemplate jdbcTemplate;

    public TripReservationDao(@Qualifier(MultiDataSourceNames.FA_CLOUD_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int expireTripSlots() {
        // Use the database clock, as the other trip queries do. Leave review and applicant data intact.
        return jdbcTemplate.update("""
                UPDATE user_trip_slot
                SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP
                WHERE status IN ('OPEN', 'PENDING', 'BOOKED')
                  AND (trip_date < CURRENT_DATE
                       OR (trip_date = CURRENT_DATE AND end_time <= CURRENT_TIME))
                """);
    }
}
