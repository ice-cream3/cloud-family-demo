package com.kfpd.cloud.partner.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.kfpd.cloud.common.datasource.MultiDataSourceNames;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.partner.dao.VipUserDao;
import com.kfpd.cloud.partner.pojo.entity.VipUser;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.UserProfileVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserPageQueryVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserRequestVO;
import com.kfpd.cloud.partner.pojo.vo.VipUserVO;
import com.kfpd.cloud.partner.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final VipUserDao vipUserDao;

    public UserServiceImpl(VipUserDao vipUserDao) {
        this.vipUserDao = vipUserDao;
    }

    @Override
    public PageVO<VipUserVO> findVipUsers(VipUserPageQueryVO query) {
        int pageNum = normalizePageNum(query == null ? null : query.pageNum());
        int pageSize = normalizePageSize(query == null ? null : query.pageSize());
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
    public UserProfileVO currentUser(String username, String roles) {
        // Username comes from the gateway after token validation.
        VipUser vipUser = Optional.ofNullable(vipUserDao.findByUsername(username)).orElseGet(() -> fallbackUser(username));
        return new UserProfileVO(vipUser.getUsername(), vipUser.getDisplayName(), splitHeader(roles), LocalDateTime.now());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO createVipUser(VipUserRequestVO request) {
        requireText(request.username(), "username is required");
        requireText(request.displayName(), "displayName is required");
        VipUser vipUser = new VipUser();
        apply(vipUser, request);
        vipUserDao.insert(vipUser);
        return findVipUserById(vipUser.getId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO updateVipUser(Long id, VipUserRequestVO request) {
        requireText(request.username(), "username is required");
        requireText(request.displayName(), "displayName is required");
        VipUser vipUser = new VipUser();
        vipUser.setId(id);
        apply(vipUser, request);
        if (vipUserDao.update(vipUser) == 0) {
            log.warn("Partner service exception: action=updateVipUser, id={}, message=Vip user not found", id);
            throw notFound("Vip user not found");
        }
        return findVipUserById(id);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void deleteVipUser(Long id) {
        if (vipUserDao.deleteById(id) == 0) {
            log.warn("Partner service exception: action=deleteVipUser, id={}, message=Vip user not found", id);
            throw notFound("Vip user not found");
        }
    }

    private VipUser fallbackUser(String username) {
        VipUser vipUser = new VipUser();
        vipUser.setUsername(username);
        vipUser.setDisplayName("demo-user");
        return vipUser;
    }

    private VipUser findVipUserEntityById(Long id) {
        return Optional.ofNullable(vipUserDao.findById(id)).orElseThrow(() -> {
            log.warn("Partner service exception: action=findVipUserById, id={}, message=Vip user not found", id);
            return notFound("Vip user not found");
        });
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

    private List<String> splitHeader(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private void apply(VipUser vipUser, VipUserRequestVO request) {
        vipUser.setUsername(request.username());
        vipUser.setPasswordHash(request.passwordHash());
        vipUser.setDisplayName(request.displayName());
        vipUser.setEmail(request.email());
        vipUser.setPhone(request.phone());
        vipUser.setVipLevel(defaultValue(request.vipLevel(), "NORMAL"));
        vipUser.setStatus(defaultValue(request.status(), "ENABLED"));
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            log.warn("Partner service exception: code={}, message={}", ErrorCode.COMMON_BAD_REQUEST.getCode(), message);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
        }
    }

    private BusinessException notFound(String message) {
        log.warn("Partner service exception: code={}, message={}", ErrorCode.PARTNER_VIP_USER_NOT_FOUND.getCode(), message);
        return new BusinessException(ErrorCode.PARTNER_VIP_USER_NOT_FOUND, message);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
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
