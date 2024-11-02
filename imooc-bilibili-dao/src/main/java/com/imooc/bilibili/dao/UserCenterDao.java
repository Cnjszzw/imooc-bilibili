package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface UserCenterDao {

    List<VideoArea> getUserCenterVideoAreas(Long userId);

    Integer pageCountUserCenterVideos(Map<String, Object> params);

    List<Video> pageListUserCenterVideos(Map<String, Object> params);

    List<FollowingGroup> getUserFollowingGroups(Long userId);

    List<UserFollowing> pageListUserCenterFollowings(Map<String, Object> params);

    List<UserInfo> getUserInfoByIds(@Param("userIds") Set<Long> userIds);

    Integer pageCountUserFans(Map<String, Object> params);

    List<UserFollowing> pageListUserFans(Map<String, Object> params);

    List<UserFollowing> getUserFollowings(Long userId);

    List<CollectionGroup> countUserCenterCollectionGroups(Long userId);

    Integer pageCountUserCollections(Map<String, Object> params);

    List<VideoCollection> pageListUserCollections(Map<String, Object> params);

    List<Video> getVideoInfoByIds(Set<Long> videoIds);

    int addUserCollectionGroups(VideoCollectionGroup videoCollectionGroup);

    List<FollowingGroup> countUserCenterFollowingGroups(Long userId);

    Integer pageCountUserCenterFollowings(Map<String, Object> params);

    Long countUserFans(Long userId);
}
