package com.kfpd.cloud.manager.dao.cloud;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

public interface UserFeatureAdminDao {

    long countNotifications(@Param("keyword") String keyword);

    List<Map<String, Object>> findNotificationPage(@Param("keyword") String keyword,
                                                   @Param("offset") int offset,
                                                   @Param("pageSize") int pageSize);

    int insertNotification(Map<String, Object> record);

    int updateNotification(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteNotification(@Param("id") Long id);

    long countDocuments(@Param("keyword") String keyword);

    List<Map<String, Object>> findDocumentPage(@Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("pageSize") int pageSize);

    int insertDocument(Map<String, Object> record);

    int updateDocument(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteDocument(@Param("id") Long id);

    long countVersions(@Param("keyword") String keyword);

    List<Map<String, Object>> findVersionPage(@Param("keyword") String keyword,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    int insertVersion(Map<String, Object> record);

    int updateVersion(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteVersion(@Param("id") Long id);

    long countMemberships(@Param("keyword") String keyword, @Param("status") String status);

    List<Map<String, Object>> findMembershipPage(@Param("keyword") String keyword,
                                                 @Param("status") String status,
                                                 @Param("offset") int offset,
                                                 @Param("pageSize") int pageSize);

    int insertMembership(Map<String, Object> record);

    int updateMembership(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteMembership(@Param("id") Long id);

    long countHistories(@Param("keyword") String keyword, @Param("category") String category);

    List<Map<String, Object>> findHistoryPage(@Param("keyword") String keyword,
                                              @Param("category") String category,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    int insertHistory(Map<String, Object> record);

    int updateHistory(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteHistory(@Param("id") Long id);

    long countFavorites(@Param("keyword") String keyword);

    List<Map<String, Object>> findFavoritePage(@Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("pageSize") int pageSize);

    int insertFavorite(Map<String, Object> record);

    int updateFavorite(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteFavorite(@Param("id") Long id);

    long countTripOwners(@Param("keyword") String keyword);

    List<Map<String, Object>> findTripOwnerPage(@Param("keyword") String keyword,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    long countTrips(@Param("keyword") String keyword, @Param("status") String status, @Param("reviewOnly") boolean reviewOnly);

    List<Map<String, Object>> findTripPage(@Param("keyword") String keyword,
                                           @Param("status") String status,
                                           @Param("reviewOnly") boolean reviewOnly,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);

    int insertTrip(Map<String, Object> record);

    int updateTrip(@Param("id") Long id, @Param("record") Map<String, Object> record);

    int deleteTrip(@Param("id") Long id);
}
