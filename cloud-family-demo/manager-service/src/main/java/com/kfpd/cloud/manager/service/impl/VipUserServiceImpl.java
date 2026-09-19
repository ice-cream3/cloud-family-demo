package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Optional;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.cloud.VipUserDao;
import com.kfpd.cloud.manager.pojo.entity.VipUser;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;
import com.kfpd.cloud.manager.service.OperationLogService;
import com.kfpd.cloud.manager.service.VipUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VipUserServiceImpl implements VipUserService {

    private static final Logger log = LoggerFactory.getLogger(VipUserServiceImpl.class);
    private static final String MODULE_PARTNER = "PARTNER";
    private static final String BUSINESS_VIP_USER = "VIP_USER";

    private final VipUserDao vipUserDao;
    private final OperationLogService operationLogService;

    public VipUserServiceImpl(VipUserDao vipUserDao, OperationLogService operationLogService) {
        this.vipUserDao = vipUserDao;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageVO<VipUserVO> findVipUsers(VipUserPageQueryVO query) {
        int pageNum = SystemManagementSupport.pageNum(query == null ? null : query.pageNum());
        int pageSize = SystemManagementSupport.pageSize(query == null ? null : query.pageSize());
        int offset = (pageNum - 1) * pageSize;
        String username = trimToNull(query == null ? null : query.username());
        String displayName = trimToNull(query == null ? null : query.displayName());
        String vipLevel = trimToNull(query == null ? null : query.vipLevel());
        String status = trimToNull(query == null ? null : query.status());
        long total = vipUserDao.count(username, displayName, vipLevel, status);
        List<VipUserVO> records = vipUserDao.findPage(username, displayName, vipLevel, status, offset, pageSize)
                .stream()
                .map(this::toVO)
                .toList();
        return new PageVO<>(total, pageNum, pageSize, records);
    }

    @Override
    public VipUserVO findVipUserById(Long id) {
        return toVO(findVipUserEntityById(id));
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO createVipUser(VipUserRequestVO request) {
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        VipUser vipUser = new VipUser();
        apply(vipUser, request);
        vipUserDao.insert(vipUser);
        VipUserVO created = findVipUserById(vipUser.getId());
        operationLogService.recordCreate(MODULE_PARTNER, BUSINESS_VIP_USER, created.id(), created.username(), created);
        return created;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO updateVipUser(Long id, VipUserRequestVO request) {
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        VipUser before = findVipUserEntityById(id);
        VipUser vipUser = new VipUser();
        vipUser.setId(id);
        apply(vipUser, request);
        if (vipUserDao.update(vipUser) == 0) {
            log.warn("Vip user service exception: action=updateVipUser, id={}, message=Vip user not found", id);
            throw SystemManagementSupport.notFound("Vip user not found");
        }
        VipUserVO updated = findVipUserById(id);
        operationLogService.recordUpdate(MODULE_PARTNER, BUSINESS_VIP_USER, id, updated.username(), toVO(before), updated);
        return updated;
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteVipUser(Long id) {
        VipUser before = findVipUserEntityById(id);
        if (vipUserDao.deleteById(id) == 0) {
            log.warn("Vip user service exception: action=deleteVipUser, id={}, message=Vip user not found", id);
            throw SystemManagementSupport.notFound("Vip user not found");
        }
        operationLogService.recordDelete(MODULE_PARTNER, BUSINESS_VIP_USER, id, before.getUsername(), toVO(before));
    }

    private VipUser findVipUserEntityById(Long id) {
        return Optional.ofNullable(vipUserDao.findById(id)).orElseThrow(() -> {
            log.warn("Vip user service exception: action=findVipUserById, id={}, message=Vip user not found", id);
            return SystemManagementSupport.notFound("Vip user not found");
        });
    }

    private void apply(VipUser vipUser, VipUserRequestVO request) {
        vipUser.setUsername(request.username());
        vipUser.setPasswordHash(request.passwordHash());
        vipUser.setDisplayName(request.displayName());
        vipUser.setEmail(request.email());
        vipUser.setPhone(request.phone());
        vipUser.setVipLevel(defaultValue(request.vipLevel(), "NORMAL"));
        vipUser.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }

    private VipUserVO toVO(VipUser vipUser) {
        return new VipUserVO(
                vipUser.getId(),
                vipUser.getUsername(),
                vipUser.getDisplayName(),
                vipUser.getEmail(),
                vipUser.getPhone(),
                vipUser.getVipLevel(),
                vipUser.getStatus(),
                vipUser.getCreatedAt(),
                vipUser.getUpdatedAt()
        );
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
