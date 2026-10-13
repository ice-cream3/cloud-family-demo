package com.kfpd.cloud.partner.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.kfpd.cloud.common.config.datasource.MultiDataSourceNames;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.partner.dao.cloud.UserProfileFeatureDao;
import com.kfpd.cloud.partner.dao.cloud.VipUserDao;
import com.kfpd.cloud.partner.pojo.entity.VipUser;
import com.kfpd.cloud.partner.pojo.vo.AccountSecurityUpdateVO;
import com.kfpd.cloud.partner.pojo.vo.AccountSecurityVO;
import com.kfpd.cloud.partner.pojo.vo.AppDocumentVO;
import com.kfpd.cloud.partner.pojo.vo.AppVersionVO;
import com.kfpd.cloud.partner.pojo.vo.EmailVerificationCodeVO;
import com.kfpd.cloud.partner.pojo.vo.EmailVerificationSendVO;
import com.kfpd.cloud.partner.pojo.vo.FavoriteItemVO;
import com.kfpd.cloud.partner.pojo.vo.MembershipVO;
import com.kfpd.cloud.partner.pojo.vo.NotificationSettingsUpdateVO;
import com.kfpd.cloud.partner.pojo.vo.NotificationSettingsVO;
import com.kfpd.cloud.partner.pojo.vo.PageQueryVO;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.PasswordChangeVO;
import com.kfpd.cloud.partner.pojo.vo.PhoneVerificationCodeVO;
import com.kfpd.cloud.partner.pojo.vo.PhoneVerificationSendVO;
import com.kfpd.cloud.partner.pojo.vo.ProfileDynamicVO;
import com.kfpd.cloud.partner.pojo.vo.TripApplyRequestVO;
import com.kfpd.cloud.partner.pojo.vo.TripCreditVO;
import com.kfpd.cloud.partner.pojo.vo.TripOwnerVO;
import com.kfpd.cloud.partner.pojo.vo.TripPublishRequestVO;
import com.kfpd.cloud.partner.pojo.vo.TripReviewRequestVO;
import com.kfpd.cloud.partner.pojo.vo.TripSlotVO;
import com.kfpd.cloud.partner.pojo.vo.UserHistoryItemVO;
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
    private static final int DEFAULT_CREDIT_SCORE = 100;
    private static final int MIN_RESERVABLE_CREDIT_SCORE = 60;
    private static final int TRIP_NO_SHOW_PENALTY = 5;
    private static final Duration PHONE_CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom PHONE_CODE_RANDOM = new SecureRandom();
    private static final Map<String, PhoneVerificationCode> PHONE_CODES = new ConcurrentHashMap<>();
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom EMAIL_CODE_RANDOM = new SecureRandom();
    private static final Map<String, EmailVerificationCode> EMAIL_CODES = new ConcurrentHashMap<>();

    private final VipUserDao vipUserDao;
    private final UserProfileFeatureDao userProfileFeatureDao;

    public UserServiceImpl(VipUserDao vipUserDao, UserProfileFeatureDao userProfileFeatureDao) {
        this.vipUserDao = vipUserDao;
        this.userProfileFeatureDao = userProfileFeatureDao;
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
    public AccountSecurityVO accountSecurity(String username) {
        return toAccountSecurityVO(findCurrentVipUser(username));
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public AccountSecurityVO updateAccountSecurity(String username, AccountSecurityUpdateVO request) {
        VipUser vipUser = findCurrentVipUser(username);
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
        requireText(request.displayName(), "displayName is required");
        String email = blankToNull(request.email());
        if (email != null && !isValidEmail(email)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email format is invalid");
        }
        String currentEmail = blankToNull(vipUser.getEmail());
        boolean emailChanged = !sameNullable(email, currentEmail);
        if (email != null && vipUserDao.countByEmailExcludingUsername(email, username) > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email already exists");
        }
        String phone = normalizePhone(request.phone());
        String phoneCountryCode = normalizePhoneCountryCode(request.phoneCountryCode());
        String currentPhone = normalizePhone(vipUser.getPhone());
        String currentPhoneCountryCode = defaultPhoneCountryCode(vipUser.getPhoneCountryCode());
        boolean phoneChanged = !sameNullable(phone, currentPhone) || (phone != null && !phoneCountryCode.equals(currentPhoneCountryCode));
        if (phone != null) {
            validatePhone(phoneCountryCode, phone);
            validatePhoneAvailable(phoneCountryCode, phone, username);
        }
        if (emailChanged) {
            if (currentEmail != null) {
                verifyEmailCode(username, currentEmail, request.currentEmailVerificationCode());
            }
            if (email != null) {
                verifyEmailCode(username, email, request.newEmailVerificationCode());
            }
        }
        if (phoneChanged) {
            if (currentPhone != null) {
                verifyPhoneCode(username, currentPhoneCountryCode, currentPhone, request.currentPhoneVerificationCode());
            }
            if (phone != null) {
                verifyPhoneCode(username, phoneCountryCode, phone, firstNonBlank(request.newPhoneVerificationCode(), request.verificationCode()));
            }
        }
        vipUser.setDisplayName(request.displayName().trim());
        vipUser.setEmail(email);
        vipUser.setPhoneCountryCode(phone == null ? null : phoneCountryCode);
        vipUser.setPhone(phone);
        if (vipUserDao.update(vipUser) == 0) {
            log.warn("Partner service exception: action=updateAccountSecurity, username={}, message=Vip user not found", username);
            throw notFound("Vip user not found");
        }
        return toAccountSecurityVO(findCurrentVipUser(username));
    }

    @Override
    public PhoneVerificationCodeVO sendPhoneVerificationCode(String username, PhoneVerificationSendVO request) {
        findCurrentVipUser(username);
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
        String phone = normalizePhone(request.phone());
        String phoneCountryCode = normalizePhoneCountryCode(request.phoneCountryCode());
        validatePhone(phoneCountryCode, phone);
        validatePhoneAvailable(phoneCountryCode, phone, username);
        String code = String.format("%06d", PHONE_CODE_RANDOM.nextInt(1_000_000));
        PHONE_CODES.put(phoneCodeKey(username, phoneCountryCode, phone), new PhoneVerificationCode(code, Instant.now().plus(PHONE_CODE_TTL)));
        log.info("Phone verification code generated: username={}, phone={}, code={}", username, maskPhone(phoneCountryCode, phone), code);
        return new PhoneVerificationCodeVO(phoneCountryCode, phone, (int) PHONE_CODE_TTL.toSeconds());
    }

    @Override
    public EmailVerificationCodeVO sendEmailVerificationCode(String username, EmailVerificationSendVO request) {
        findCurrentVipUser(username);
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
        String email = blankToNull(request.email());
        if (!isValidEmail(email)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email format is invalid");
        }
        if (vipUserDao.countByEmailExcludingUsername(email, username) > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email already exists");
        }
        String code = String.format("%06d", EMAIL_CODE_RANDOM.nextInt(1_000_000));
        EMAIL_CODES.put(emailCodeKey(username, email), new EmailVerificationCode(code, Instant.now().plus(EMAIL_CODE_TTL)));
        log.info("Email verification code generated: username={}, email={}, code={}", username, maskEmail(email), code);
        return new EmailVerificationCodeVO(email, (int) EMAIL_CODE_TTL.toSeconds());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public void changePassword(String username, PasswordChangeVO request) {
        VipUser vipUser = findCurrentVipUser(username);
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
        requireText(request.currentPassword(), "currentPassword is required");
        requireText(request.newPassword(), "newPassword is required");
        if (vipUser.getPasswordHash() == null || !vipUser.getPasswordHash().equals(request.currentPassword())) {
            log.warn("Partner service exception: action=changePassword, username={}, message=Current password is incorrect", username);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "Current password is incorrect");
        }
        if (!isStrongPassword(request.newPassword())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "newPassword is not strong enough");
        }
        vipUser.setPasswordHash(request.newPassword());
        if (vipUserDao.update(vipUser) == 0) {
            log.warn("Partner service exception: action=changePassword, username={}, message=Vip user not found", username);
            throw notFound("Vip user not found");
        }
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public NotificationSettingsVO notificationSettings(String username) {
        findCurrentVipUser(username);
        userProfileFeatureDao.insertDefaultNotificationSettings(username);
        return userProfileFeatureDao.findNotificationSettings(username);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public NotificationSettingsVO updateNotificationSettings(String username, NotificationSettingsUpdateVO request) {
        findCurrentVipUser(username);
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
        userProfileFeatureDao.updateNotificationSettings(
                username,
                defaultBoolean(request.systemEnabled(), true),
                defaultBoolean(request.activityEnabled(), true),
                defaultBoolean(request.taskEnabled(), false)
        );
        return userProfileFeatureDao.findNotificationSettings(username);
    }

    @Override
    public AppDocumentVO document(String documentType) {
        String normalizedType = trimToNull(documentType);
        if (normalizedType == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "documentType is required");
        }
        return Optional.ofNullable(userProfileFeatureDao.findDocument(normalizedType)).orElseThrow(() ->
                new BusinessException(ErrorCode.COMMON_NOT_FOUND, "document not found"));
    }

    @Override
    public AppVersionVO latestVersion() {
        return Optional.ofNullable(userProfileFeatureDao.findLatestVersion())
                .orElse(new AppVersionVO("v1.0.0", true, "当前已是最新版本"));
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public MembershipVO membership(String username) {
        findCurrentVipUser(username);
        userProfileFeatureDao.insertDefaultMembership(username);
        return userProfileFeatureDao.findMembership(username);
    }

    @Override
    public PageVO<UserHistoryItemVO> history(String username, PageQueryVO query) {
        findCurrentVipUser(username);
        int pageNum = normalizePageNum(query == null ? null : query.pageNum());
        int pageSize = normalizePageSize(query == null ? null : query.pageSize());
        int offset = (pageNum - 1) * pageSize;
        String category = trimToNull(query == null ? null : query.category());
        long total = userProfileFeatureDao.countHistory(username, category);
        List<UserHistoryItemVO> records = userProfileFeatureDao.findHistoryPage(username, category, offset, pageSize);
        return new PageVO<>(total, pageNum, pageSize, records);
    }

    @Override
    public PageVO<FavoriteItemVO> favorites(String username, PageQueryVO query) {
        findCurrentVipUser(username);
        int pageNum = normalizePageNum(query == null ? null : query.pageNum());
        int pageSize = normalizePageSize(query == null ? null : query.pageSize());
        int offset = (pageNum - 1) * pageSize;
        long total = userProfileFeatureDao.countFavorites(username);
        List<FavoriteItemVO> records = userProfileFeatureDao.findFavoritePage(username, offset, pageSize);
        return new PageVO<>(total, pageNum, pageSize, records);
    }

    @Override
    public ProfileDynamicVO refreshDynamic(String username) {
        findCurrentVipUser(username);
        long favoriteCount = userProfileFeatureDao.countFavorites(username);
        long historyCount = userProfileFeatureDao.countHistory(username, null);
        return new ProfileDynamicVO(
                favoriteCount,
                historyCount,
                3,
                0,
                "UP",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @Override
    public List<TripOwnerVO> tripOwners(String username) {
        findCurrentVipUser(username);
        return userProfileFeatureDao.findBookableTripOwners(username);
    }

    @Override
    public List<TripSlotVO> bookableTripSlots(String username, String ownerUsername) {
        findCurrentVipUser(username);
        String owner = trimToNull(ownerUsername);
        if (owner == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "ownerUsername is required");
        }
        if (owner.equalsIgnoreCase(username)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "cannot book your own trip");
        }
        return userProfileFeatureDao.findBookableTripSlots(username, owner);
    }

    @Override
    public List<TripSlotVO> myTrips(String username) {
        findCurrentVipUser(username);
        return userProfileFeatureDao.findMyTripSlots(username);
    }

    @Override
    public List<TripSlotVO> myReservedTrips(String username) {
        findCurrentVipUser(username);
        return userProfileFeatureDao.findMyReservedTripSlots(username);
    }

    @Override
    public TripCreditVO tripCredit(String username) {
        return toTripCredit(findCurrentVipUser(username));
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public TripSlotVO publishTrip(String username, TripPublishRequestVO request) {
        VipUser vipUser = findCurrentVipUser(username);
        requireVerifiedPhone(vipUser);
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
        requireText(request.title(), "title is required");
        requireText(request.place(), "place is required");
        LocalDate tripDate = parseTripDate(request.tripDate());
        LocalTime startTime = parseTripTime(request.startTime(), "startTime is required");
        LocalTime endTime = parseTripTime(request.endTime(), "endTime is required");
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "endTime must be after startTime");
        }
        LocalDate today = LocalDate.now();
        if (tripDate.isBefore(today) || tripDate.isAfter(today.plusDays(6))) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "tripDate must be within the next 7 days");
        }
        if (userProfileFeatureDao.countOwnedTripSlotConflicts(username, tripDate.toString(), startTime.toString(), endTime.toString()) > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip slot time conflicts");
        }
        userProfileFeatureDao.insertTripSlot(
                username,
                Optional.ofNullable(vipUser.getDisplayName()).orElse(username),
                request.title().trim(),
                request.place().trim(),
                tripDate.toString(),
                startTime.toString(),
                endTime.toString()
        );
        return userProfileFeatureDao.findMyTripSlots(username).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "publish failed"));
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public TripSlotVO applyTrip(String username, TripApplyRequestVO request) {
        VipUser vipUser = findCurrentVipUser(username);
        requireVerifiedPhone(vipUser);
        if (currentCreditScore(vipUser) < MIN_RESERVABLE_CREDIT_SCORE) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "credit score is too low to book trips");
        }
        if (request == null || request.slotId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "slotId is required");
        }
        TripSlotVO slot = Optional.ofNullable(userProfileFeatureDao.findTripSlotById(request.slotId()))
                .orElseThrow(() -> notFound("Trip slot not found"));
        if (username.equalsIgnoreCase(slot.ownerUsername())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "cannot book your own trip");
        }
        if (!"OPEN".equals(slot.status())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip slot is not open");
        }
        List<TripSlotVO> conflicts = userProfileFeatureDao.findConflictingOwnedTripSlots(username, request.slotId());
        if (!conflicts.isEmpty() && !Boolean.TRUE.equals(request.confirmConflict())) {
            throw new BusinessException(ErrorCode.PARTNER_TRIP_CONFLICT, "预约时间与您发布的行程冲突，继续预约将删除冲突行程");
        }
        if (!conflicts.isEmpty()) {
            userProfileFeatureDao.deleteConflictingOwnedTripSlots(username, request.slotId());
        }
        int affected = userProfileFeatureDao.applyTripSlot(
                request.slotId(),
                username,
                Optional.ofNullable(vipUser.getDisplayName()).orElse(username),
                Optional.ofNullable(trimToNull(request.applyNote())).orElse("希望预约这个时段")
        );
        if (affected == 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip slot is not available");
        }
        return userProfileFeatureDao.findTripSlotById(request.slotId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public TripSlotVO markTripNoShow(String username, Long slotId) {
        findCurrentVipUser(username);
        if (slotId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "slotId is required");
        }
        TripSlotVO slot = Optional.ofNullable(userProfileFeatureDao.findTripSlotById(slotId))
                .orElseThrow(() -> notFound("Trip slot not found"));
        if (slot.applicantUsername() == null || slot.applicantUsername().isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip has no applicant");
        }
        LocalDateTime tripEndAt = LocalDateTime.of(slot.tripDate(), slot.endTime());
        if (tripEndAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip is not finished yet");
        }
        int affected = userProfileFeatureDao.markTripNoShow(slotId, username);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip no-show is not available");
        }
        vipUserDao.deductCreditScore(slot.applicantUsername(), TRIP_NO_SHOW_PENALTY);
        return userProfileFeatureDao.findTripSlotById(slotId);
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public TripSlotVO reviewTrip(String username, TripReviewRequestVO request) {
        findCurrentVipUser(username);
        if (request == null || request.slotId() == null || request.approved() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "slotId and approved are required");
        }
        int affected = request.approved()
                ? userProfileFeatureDao.approveTripSlot(request.slotId(), username)
                : userProfileFeatureDao.rejectTripSlot(request.slotId(), username);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip review is not available");
        }
        return userProfileFeatureDao.findTripSlotById(request.slotId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO createVipUser(VipUserRequestVO request) {
        requireRequest(request);
        requireText(request.username(), "username is required");
        requireText(request.displayName(), "displayName is required");
        validateEmailUnique(request.email(), null);
        validateOptionalPhone(request.phoneCountryCode(), request.phone());
        VipUser vipUser = new VipUser();
        apply(vipUser, request);
        vipUserDao.insert(vipUser);
        return findVipUserById(vipUser.getId());
    }

    @Override
    @Transactional(transactionManager = MultiDataSourceNames.FA_CLOUD_TRANSACTION_MANAGER)
    public VipUserVO updateVipUser(Long id, VipUserRequestVO request) {
        requireRequest(request);
        requireText(request.username(), "username is required");
        requireText(request.displayName(), "displayName is required");
        validateEmailUnique(request.email(), id);
        validateOptionalPhone(request.phoneCountryCode(), request.phone());
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

    private VipUser findCurrentVipUser(String username) {
        requireText(username, "username is required");
        return Optional.ofNullable(vipUserDao.findByUsername(username)).orElseThrow(() -> {
            log.warn("Partner service exception: action=findCurrentVipUser, username={}, message=Vip user not found", username);
            return notFound("Vip user not found");
        });
    }

    private VipUserVO toVO(VipUser vipUser) {
        return new VipUserVO(
                vipUser.getId(),
                vipUser.getUsername(),
                vipUser.getDisplayName(),
                vipUser.getEmail(),
                vipUser.getPhoneCountryCode(),
                vipUser.getPhone(),
                vipUser.getVipLevel(),
                currentCreditScore(vipUser),
                vipUser.getStatus(),
                vipUser.getCreatedAt(),
                vipUser.getUpdatedAt()
        );
    }

    private AccountSecurityVO toAccountSecurityVO(VipUser vipUser) {
        return new AccountSecurityVO(
                vipUser.getUsername(),
                vipUser.getDisplayName(),
                vipUser.getEmail(),
                vipUser.getPhoneCountryCode(),
                vipUser.getPhone(),
                vipUser.getVipLevel(),
                currentCreditScore(vipUser),
                vipUser.getStatus(),
                vipUser.getUpdatedAt()
        );
    }

    private TripCreditVO toTripCredit(VipUser vipUser) {
        int creditScore = currentCreditScore(vipUser);
        return new TripCreditVO(
                creditScore,
                creditLevel(creditScore),
                MIN_RESERVABLE_CREDIT_SCORE,
                TRIP_NO_SHOW_PENALTY,
                creditScore >= MIN_RESERVABLE_CREDIT_SCORE
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
        vipUser.setUsername(request.username().trim());
        vipUser.setPasswordHash(request.passwordHash());
        vipUser.setDisplayName(request.displayName().trim());
        vipUser.setEmail(blankToNull(request.email()));
        String phone = normalizePhone(request.phone());
        vipUser.setPhoneCountryCode(phone == null ? null : normalizePhoneCountryCode(request.phoneCountryCode()));
        vipUser.setPhone(phone);
        vipUser.setVipLevel(defaultValue(request.vipLevel(), "NORMAL"));
        vipUser.setCreditScore(normalizeCreditScore(request.creditScore()));
        vipUser.setStatus(defaultValue(request.status(), "ENABLED"));
    }

    private int currentCreditScore(VipUser vipUser) {
        return normalizeCreditScore(vipUser.getCreditScore());
    }

    private int normalizeCreditScore(Integer score) {
        if (score == null) {
            return DEFAULT_CREDIT_SCORE;
        }
        return Math.max(0, Math.min(DEFAULT_CREDIT_SCORE, score));
    }

    private String creditLevel(int score) {
        if (score >= 95) {
            return "S";
        }
        if (score >= 85) {
            return "A";
        }
        if (score >= 75) {
            return "B";
        }
        if (score >= MIN_RESERVABLE_CREDIT_SCORE) {
            return "C";
        }
        return "D";
    }

    private void requireRequest(VipUserRequestVO request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "request is required");
        }
    }

    private void requireVerifiedPhone(VipUser vipUser) {
        if (vipUser.getPhone() == null || vipUser.getPhone().isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone verification is required");
        }
    }

    private void validatePhone(String phoneCountryCode, String phone) {
        if (!isValidPhoneCountryCode(phoneCountryCode) || phone == null || !phone.matches("^\\d{4,14}$")) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone format is invalid");
        }
        String fullDigits = phoneCountryCode.substring(1) + phone;
        if (!fullDigits.matches("^[1-9]\\d{7,14}$")) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone format is invalid");
        }
    }

    private void validateOptionalPhone(String rawPhoneCountryCode, String rawPhone) {
        String phone = normalizePhone(rawPhone);
        if (phone != null) {
            validatePhone(normalizePhoneCountryCode(rawPhoneCountryCode), phone);
        }
    }

    private void validatePhoneAvailable(String phoneCountryCode, String phone, String username) {
        if (vipUserDao.countByPhoneExcludingUsername(phoneCountryCode, phone, username) > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone already exists");
        }
    }

    private void verifyPhoneCode(String username, String phoneCountryCode, String phone, String rawCode) {
        String code = blankToNull(rawCode);
        if (code == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone verification code is required");
        }
        String key = phoneCodeKey(username, phoneCountryCode, phone);
        PhoneVerificationCode savedCode = PHONE_CODES.get(key);
        if (savedCode == null || savedCode.expiresAt().isBefore(Instant.now())) {
            PHONE_CODES.remove(key);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone verification code expired");
        }
        if (!savedCode.code().equals(code)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "phone verification code is invalid");
        }
        PHONE_CODES.remove(key);
    }

    private void verifyEmailCode(String username, String email, String rawCode) {
        String code = blankToNull(rawCode);
        if (code == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email verification code is required");
        }
        String key = emailCodeKey(username, email);
        EmailVerificationCode savedCode = EMAIL_CODES.get(key);
        if (savedCode == null || savedCode.expiresAt().isBefore(Instant.now())) {
            EMAIL_CODES.remove(key);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email verification code expired");
        }
        if (!savedCode.code().equals(code)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email verification code is invalid");
        }
        EMAIL_CODES.remove(key);
    }

    private String phoneCodeKey(String username, String phoneCountryCode, String phone) {
        return username + ":" + phoneCountryCode + phone;
    }

    private String emailCodeKey(String username, String email) {
        return username + ":" + email;
    }

    private String maskPhone(String phoneCountryCode, String phone) {
        String normalized = phoneCountryCode + phone;
        if (phone == null || phone.length() < 4) {
            return normalized;
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(atIndex, 0));
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? null : phone.trim().replaceAll("[\\s\\-()]", "");
        if (normalized != null && normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String normalizePhoneCountryCode(String phoneCountryCode) {
        String normalized = blankToNull(phoneCountryCode);
        if (normalized == null) {
            return "+86";
        }
        normalized = normalized.replaceAll("[\\s\\-()]", "");
        return normalized.startsWith("+") ? normalized : "+" + normalized;
    }

    private String defaultPhoneCountryCode(String phoneCountryCode) {
        return normalizePhoneCountryCode(phoneCountryCode);
    }

    private boolean isValidPhoneCountryCode(String phoneCountryCode) {
        return phoneCountryCode != null && phoneCountryCode.matches("^\\+[1-9]\\d{0,3}$");
    }

    private void validateEmailUnique(String rawEmail, Long excludeId) {
        String email = blankToNull(rawEmail);
        if (email == null) {
            return;
        }
        if (!isValidEmail(email)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email format is invalid");
        }
        long count = excludeId == null
                ? vipUserDao.countByEmail(email)
                : vipUserDao.countByEmailExcludingId(email, excludeId);
        if (count > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "email already exists");
        }
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

    private boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        String normalized = blankToNull(first);
        return normalized == null ? blankToNull(second) : normalized;
    }

    private boolean sameNullable(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private boolean isStrongPassword(String value) {
        return value != null && value.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{6,20}$");
    }

    private boolean isValidEmail(String value) {
        return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    }

    private LocalDate parseTripDate(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "tripDate is required");
        }
        try {
            return LocalDate.parse(normalized);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "tripDate format is invalid");
        }
    }

    private LocalTime parseTripTime(String value, String emptyMessage) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, emptyMessage);
        }
        try {
            return LocalTime.parse(normalized);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "trip time format is invalid");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record PhoneVerificationCode(String code, Instant expiresAt) {
    }

    private record EmailVerificationCode(String code, Instant expiresAt) {
    }
}
