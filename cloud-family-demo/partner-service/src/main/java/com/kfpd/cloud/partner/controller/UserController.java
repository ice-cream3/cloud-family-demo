package com.kfpd.cloud.partner.controller;

import java.util.Map;
import java.util.List;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.common.web.GatewayHeaders;
import com.kfpd.cloud.partner.pojo.vo.AccountSecurityUpdateVO;
import com.kfpd.cloud.partner.pojo.vo.AccountSecurityVO;
import com.kfpd.cloud.partner.pojo.vo.AppDocumentVO;
import com.kfpd.cloud.partner.pojo.vo.AppVersionVO;
import com.kfpd.cloud.partner.pojo.vo.FavoriteItemVO;
import com.kfpd.cloud.partner.pojo.vo.MembershipVO;
import com.kfpd.cloud.partner.pojo.vo.NotificationSettingsUpdateVO;
import com.kfpd.cloud.partner.pojo.vo.NotificationSettingsVO;
import com.kfpd.cloud.partner.pojo.vo.PageQueryVO;
import com.kfpd.cloud.partner.pojo.vo.PageVO;
import com.kfpd.cloud.partner.pojo.vo.PasswordChangeVO;
import com.kfpd.cloud.partner.pojo.vo.ProfileDynamicVO;
import com.kfpd.cloud.partner.pojo.vo.TripApplyRequestVO;
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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping({"/me", "/my"})
    public ApiResponse<UserProfileVO> currentUser(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles
    ) {
        return ApiResponse.success(userService.currentUser(username, roles));
    }

    @PostMapping("/security")
    public ApiResponse<AccountSecurityVO> accountSecurity(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.accountSecurity(username));
    }

    @PostMapping("/security/update")
    public ApiResponse<AccountSecurityVO> updateAccountSecurity(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody AccountSecurityUpdateVO request
    ) {
        return ApiResponse.success(userService.updateAccountSecurity(username, request));
    }

    @PostMapping("/security/password")
    public ApiResponse<Void> changePassword(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody PasswordChangeVO request
    ) {
        userService.changePassword(username, request);
        return ApiResponse.success();
    }

    @PostMapping("/settings/notifications")
    public ApiResponse<NotificationSettingsVO> notificationSettings(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.notificationSettings(username));
    }

    @PostMapping("/settings/notifications/update")
    public ApiResponse<NotificationSettingsVO> updateNotificationSettings(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody NotificationSettingsUpdateVO request
    ) {
        return ApiResponse.success(userService.updateNotificationSettings(username, request));
    }

    @PostMapping("/settings/document/{documentType}")
    public ApiResponse<AppDocumentVO> document(@PathVariable String documentType) {
        return ApiResponse.success(userService.document(documentType));
    }

    @PostMapping("/settings/version")
    public ApiResponse<AppVersionVO> latestVersion() {
        return ApiResponse.success(userService.latestVersion());
    }

    @PostMapping("/membership")
    public ApiResponse<MembershipVO> membership(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.membership(username));
    }

    @PostMapping("/history/page")
    public ApiResponse<PageVO<UserHistoryItemVO>> history(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody(required = false) PageQueryVO query
    ) {
        return ApiResponse.success(userService.history(username, query));
    }

    @PostMapping("/favorites/page")
    public ApiResponse<PageVO<FavoriteItemVO>> favorites(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody(required = false) PageQueryVO query
    ) {
        return ApiResponse.success(userService.favorites(username, query));
    }

    @PostMapping("/dynamic/refresh")
    public ApiResponse<ProfileDynamicVO> refreshDynamic(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.refreshDynamic(username));
    }

    @PostMapping("/trips/bookable/owners")
    public ApiResponse<List<TripOwnerVO>> tripOwners(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.tripOwners(username));
    }

    @PostMapping("/trips/bookable/slots/{ownerUsername}")
    public ApiResponse<List<TripSlotVO>> bookableTripSlots(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @PathVariable String ownerUsername
    ) {
        return ApiResponse.success(userService.bookableTripSlots(username, ownerUsername));
    }

    @PostMapping("/trips/my")
    public ApiResponse<List<TripSlotVO>> myTrips(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.myTrips(username));
    }

    @PostMapping("/trips/my-reservations")
    public ApiResponse<List<TripSlotVO>> myReservedTrips(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username
    ) {
        return ApiResponse.success(userService.myReservedTrips(username));
    }

    @PostMapping("/trips/publish")
    public ApiResponse<TripSlotVO> publishTrip(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody TripPublishRequestVO request
    ) {
        return ApiResponse.success(userService.publishTrip(username, request));
    }

    @PostMapping("/trips/apply")
    public ApiResponse<TripSlotVO> applyTrip(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody TripApplyRequestVO request
    ) {
        return ApiResponse.success(userService.applyTrip(username, request));
    }

    @PostMapping("/trips/review")
    public ApiResponse<TripSlotVO> reviewTrip(
            @RequestHeader(value = GatewayHeaders.USER_NAME, defaultValue = "anonymous") String username,
            @RequestBody TripReviewRequestVO request
    ) {
        return ApiResponse.success(userService.reviewTrip(username, request));
    }

    @PostMapping("/vip-users/page")
    public ApiResponse<PageVO<VipUserVO>> vipUsers(@ModelAttribute VipUserPageQueryVO query) {
        return ApiResponse.success(userService.findVipUsers(query));
    }

    @PostMapping("/vip-users/detail/{id}")
    public ApiResponse<VipUserVO> vipUser(@PathVariable Long id) {
        return ApiResponse.success(userService.findVipUserById(id));
    }

    @PostMapping("/vip-users")
    public ResponseEntity<ApiResponse<VipUserVO>> createVipUser(@RequestBody VipUserRequestVO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.createVipUser(request)));
    }

    @PostMapping("/vip-users/update/{id}")
    public ApiResponse<VipUserVO> updateVipUser(@PathVariable Long id, @RequestBody VipUserRequestVO request) {
        return ApiResponse.success(userService.updateVipUser(id, request));
    }

    @PostMapping("/vip-users/delete/{id}")
    public ApiResponse<Void> deleteVipUser(@PathVariable Long id) {
        userService.deleteVipUser(id);
        return ApiResponse.success();
    }

    @PostMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP", "service", "partner-service"));
    }
}
