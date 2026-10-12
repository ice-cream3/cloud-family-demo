package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.manager.dao.cloud.VipUserDao;
import com.kfpd.cloud.manager.pojo.entity.VipUser;
import com.kfpd.cloud.manager.pojo.vo.AuthSessionStatusVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.manager.pojo.vo.VipUserVO;
import com.kfpd.cloud.manager.service.AuthSessionAdminService;
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
    private final AuthSessionAdminService authSessionAdminService;

    public VipUserServiceImpl(VipUserDao vipUserDao,
                              OperationLogService operationLogService,
                              AuthSessionAdminService authSessionAdminService) {
        this.vipUserDao = vipUserDao;
        this.operationLogService = operationLogService;
        this.authSessionAdminService = authSessionAdminService;
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
    public PageVO<VipUserVO> findPartnerInfoUsers(VipUserPageQueryVO query, String authorization) {
        PageVO<VipUserVO> page = findVipUsers(query);
        List<String> usernames = page.records().stream()
                .map(VipUserVO::username)
                .filter(username -> username != null && !username.isBlank())
                .distinct()
                .toList();
        Map<String, AuthSessionStatusVO> statuses = authSessionAdminService.apiSessionStatuses(usernames, authorization)
                .stream()
                .collect(Collectors.toMap(AuthSessionStatusVO::username, status -> status, (left, right) -> left));
        List<VipUserVO> records = page.records().stream()
                .map(record -> withSessionStatus(record, statuses.get(record.username())))
                .toList();
        return new PageVO<>(page.total(), page.pageNum(), page.pageSize(), records);
    }

    @Override
    public VipUserVO findVipUserById(Long id) {
        return toVO(findVipUserEntityById(id));
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO createVipUser(VipUserRequestVO request) {
        requireRequest(request);
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        validateEmailUnique(request.email(), null);
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
        requireRequest(request);
        SystemManagementSupport.requireText(request.username(), "username is required");
        SystemManagementSupport.requireText(request.displayName(), "displayName is required");
        VipUser before = findVipUserEntityById(id);
        validateEmailUnique(request.email(), id);
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
    public VipUserVO resetVipUserPassword(Long id, PasswordResetRequestVO request) {
        if (request == null) {
            throw SystemManagementSupport.badRequest("request body is required");
        }
        SystemManagementSupport.requireText(request.resolvedPasswordHash(), "password is required");
        VipUser before = findVipUserEntityById(id);
        VipUser vipUser = new VipUser();
        vipUser.setId(id);
        vipUser.setUsername(before.getUsername());
        vipUser.setPasswordHash(request.resolvedPasswordHash());
        vipUser.setDisplayName(before.getDisplayName());
        vipUser.setEmail(before.getEmail());
        vipUser.setPhone(before.getPhone());
        vipUser.setVipLevel(before.getVipLevel());
        vipUser.setStatus(before.getStatus());
        if (vipUserDao.update(vipUser) == 0) {
            log.warn("Vip user service exception: action=resetVipUserPassword, id={}, message=Vip user not found", id);
            throw SystemManagementSupport.notFound("Vip user not found");
        }
        VipUserVO updated = findVipUserById(id);
        operationLogService.recordUpdate(MODULE_PARTNER, BUSINESS_VIP_USER, id, updated.username(), "PASSWORD_RESET", updated);
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

    @Override
    public int kickOutVipUser(Long id, String authorization) {
        VipUser vipUser = findVipUserEntityById(id);
        int revokedCount = authSessionAdminService.kickOutApiUser(vipUser.getUsername(), authorization);
        operationLogService.recordUpdate(MODULE_PARTNER, BUSINESS_VIP_USER, id, vipUser.getUsername(), "KICK_OUT", Map.of(
                "username", vipUser.getUsername(),
                "revokedCount", revokedCount
        ));
        return revokedCount;
    }

    private VipUser findVipUserEntityById(Long id) {
        return Optional.ofNullable(vipUserDao.findById(id)).orElseThrow(() -> {
            log.warn("Vip user service exception: action=findVipUserById, id={}, message=Vip user not found", id);
            return SystemManagementSupport.notFound("Vip user not found");
        });
    }

    private void apply(VipUser vipUser, VipUserRequestVO request) {
        vipUser.setUsername(request.username().trim());
        vipUser.setPasswordHash(request.passwordHash());
        vipUser.setDisplayName(request.displayName().trim());
        vipUser.setEmail(trimToNull(request.email()));
        vipUser.setPhone(trimToNull(request.phone()));
        vipUser.setVipLevel(defaultValue(request.vipLevel(), "NORMAL"));
        vipUser.setStatus(SystemManagementSupport.defaultStatus(request.status()));
    }

    private void requireRequest(VipUserRequestVO request) {
        if (request == null) {
            throw SystemManagementSupport.badRequest("request body is required");
        }
    }

    private void validateEmailUnique(String rawEmail, Long excludeId) {
        String email = trimToNull(rawEmail);
        if (email == null) {
            return;
        }
        if (!isValidEmail(email)) {
            throw SystemManagementSupport.badRequest("email format is invalid");
        }
        long count = excludeId == null
                ? vipUserDao.countByEmail(email)
                : vipUserDao.countByEmailExcludingId(email, excludeId);
        if (count > 0) {
            throw SystemManagementSupport.badRequest("email already exists");
        }
    }

    private boolean isValidEmail(String value) {
        return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
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
                vipUser.getUpdatedAt(),
                null,
                null,
                null,
                null
        );
    }

    private VipUserVO withSessionStatus(VipUserVO vipUser, AuthSessionStatusVO status) {
        if (status == null) {
            return new VipUserVO(
                    vipUser.id(),
                    vipUser.username(),
                    vipUser.displayName(),
                    vipUser.email(),
                    vipUser.phone(),
                    vipUser.vipLevel(),
                    vipUser.status(),
                    vipUser.createdAt(),
                    vipUser.updatedAt(),
                    false,
                    0,
                    null,
                    null
            );
        }
        return new VipUserVO(
                vipUser.id(),
                vipUser.username(),
                vipUser.displayName(),
                vipUser.email(),
                vipUser.phone(),
                vipUser.vipLevel(),
                vipUser.status(),
                vipUser.createdAt(),
                vipUser.updatedAt(),
                status.online(),
                status.sessionCount(),
                status.latestAccessTokenExpiresAt() == null ? null : status.latestAccessTokenExpiresAt().toString(),
                status.latestRefreshTokenExpiresAt() == null ? null : status.latestRefreshTokenExpiresAt().toString()
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
