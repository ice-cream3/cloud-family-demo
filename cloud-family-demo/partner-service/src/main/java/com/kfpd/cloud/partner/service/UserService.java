package com.kfpd.cloud.partner.service;

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

public interface UserService {

    PageVO<VipUserVO> findVipUsers(VipUserPageQueryVO query);

    VipUserVO findVipUserById(Long id);

    UserProfileVO currentUser(String username, String roles);

    AccountSecurityVO accountSecurity(String username);

    AccountSecurityVO updateAccountSecurity(String username, AccountSecurityUpdateVO request);

    void changePassword(String username, PasswordChangeVO request);

    NotificationSettingsVO notificationSettings(String username);

    NotificationSettingsVO updateNotificationSettings(String username, NotificationSettingsUpdateVO request);

    AppDocumentVO document(String documentType);

    AppVersionVO latestVersion();

    MembershipVO membership(String username);

    PageVO<UserHistoryItemVO> history(String username, PageQueryVO query);

    PageVO<FavoriteItemVO> favorites(String username, PageQueryVO query);

    ProfileDynamicVO refreshDynamic(String username);

    java.util.List<TripOwnerVO> tripOwners(String username);

    java.util.List<TripSlotVO> bookableTripSlots(String username, String ownerUsername);

    java.util.List<TripSlotVO> myTrips(String username);

    java.util.List<TripSlotVO> myReservedTrips(String username);

    TripSlotVO publishTrip(String username, TripPublishRequestVO request);

    TripSlotVO applyTrip(String username, TripApplyRequestVO request);

    TripSlotVO reviewTrip(String username, TripReviewRequestVO request);

    VipUserVO createVipUser(VipUserRequestVO request);

    VipUserVO updateVipUser(Long id, VipUserRequestVO request);

    void deleteVipUser(Long id);
}
