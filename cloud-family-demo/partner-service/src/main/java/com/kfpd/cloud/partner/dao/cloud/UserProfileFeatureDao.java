package com.kfpd.cloud.partner.dao.cloud;

import java.util.List;

import com.kfpd.cloud.partner.pojo.vo.AppDocumentVO;
import com.kfpd.cloud.partner.pojo.vo.AppVersionVO;
import com.kfpd.cloud.partner.pojo.vo.FavoriteItemVO;
import com.kfpd.cloud.partner.pojo.vo.MembershipVO;
import com.kfpd.cloud.partner.pojo.vo.NotificationSettingsVO;
import com.kfpd.cloud.partner.pojo.vo.TripOwnerVO;
import com.kfpd.cloud.partner.pojo.vo.TripSlotVO;
import com.kfpd.cloud.partner.pojo.vo.UserHistoryItemVO;

import org.apache.ibatis.annotations.Param;

public interface UserProfileFeatureDao {

    NotificationSettingsVO findNotificationSettings(@Param("username") String username);

    int insertDefaultNotificationSettings(@Param("username") String username);

    int updateNotificationSettings(@Param("username") String username,
                                   @Param("systemEnabled") Boolean systemEnabled,
                                   @Param("activityEnabled") Boolean activityEnabled,
                                   @Param("taskEnabled") Boolean taskEnabled);

    AppDocumentVO findDocument(@Param("documentType") String documentType);

    AppVersionVO findLatestVersion();

    MembershipVO findMembership(@Param("username") String username);

    int insertDefaultMembership(@Param("username") String username);

    long countHistory(@Param("username") String username, @Param("category") String category);

    List<UserHistoryItemVO> findHistoryPage(@Param("username") String username,
                                            @Param("category") String category,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    long countFavorites(@Param("username") String username);

    List<FavoriteItemVO> findFavoritePage(@Param("username") String username,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    List<TripOwnerVO> findBookableTripOwners(@Param("username") String username);

    List<TripSlotVO> findBookableTripSlots(@Param("username") String username,
                                           @Param("ownerUsername") String ownerUsername);

    List<TripSlotVO> findMyTripSlots(@Param("username") String username);

    TripSlotVO findTripSlotById(@Param("id") Long id);

    int insertTripSlot(@Param("ownerUsername") String ownerUsername,
                       @Param("ownerDisplayName") String ownerDisplayName,
                       @Param("title") String title,
                       @Param("place") String place,
                       @Param("tripDate") String tripDate,
                       @Param("startTime") String startTime,
                       @Param("endTime") String endTime);

    int applyTripSlot(@Param("id") Long id,
                      @Param("applicantUsername") String applicantUsername,
                      @Param("applicantDisplayName") String applicantDisplayName,
                      @Param("applyNote") String applyNote);

    int approveTripSlot(@Param("id") Long id, @Param("ownerUsername") String ownerUsername);

    int rejectTripSlot(@Param("id") Long id, @Param("ownerUsername") String ownerUsername);
}
