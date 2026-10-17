package com.kfpd.cloud.job.service.impl;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.job.dao.cloud.TripReservationDao;
import com.kfpd.cloud.job.service.TripExpirationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripExpirationServiceImpl implements TripExpirationService {

    private final TripReservationDao tripReservationDao;

    public TripExpirationServiceImpl(TripReservationDao tripReservationDao) {
        this.tripReservationDao = tripReservationDao;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public int expireTrips() {
        return tripReservationDao.expireTripSlots();
    }
}
