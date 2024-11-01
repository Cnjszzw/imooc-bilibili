package com.imooc.bilibili.dao;


import com.imooc.bilibili.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VideoDao {
    Integer addVideos(Video video);

    Integer batchAddVideoTags(@Param("tagList") List<VideoTag> tagList);

    List<Video> pageListVideos(@Param("start") Integer start, @Param("limit") Integer limit, @Param("area") String area);

    Integer queryVideosTotalNum(String area);

    List<VideoTag> queryVideoTagsByVideoId(Long id);

    Video getVideoByVideoId(Long videoId);

    VideoLike getVideoLike(Long userId, Long videoId);

    Integer addLikeVideo(VideoLike videoLikeNew);

    Integer deleteLikeVideo(@Param("userId") Long userId, @Param("videoId") Long videoId);

    Integer getLikeVideoNum(Long videoId);

    Integer addVideoCollection(VideoCollection videoCollection);

    Integer delVideoCollection(@Param("videoId") Long videoId, @Param("userId") Long userId, @Param("groupId") Long groupId);

    Integer queryVideoCollectionCounts(Long videoId);

    VideoCollection queryVideoCollection(@Param("videoId") Long videoId, @Param("userId") Long userId);

    Integer getVideoCoins(@Param("videoId") Long videoId,@Param("userId") Long userId);

    Integer addVideoComment(VideoComment videoComment);

    Integer getVideoTotalCommentNum(Long videoId);

    List<VideoComment> pageListVideoComments(Map<String, Object> params);

    List<VideoComment> pageListVideoCommentReplies(Long videoId);

    Video getVideoDetails(Long videoId);

    VideoView getVideoView(Map<String, Object> params);

    Integer addVideoView(VideoView videoView);

    Integer getVideoViewCounts(Long videoId);
}
