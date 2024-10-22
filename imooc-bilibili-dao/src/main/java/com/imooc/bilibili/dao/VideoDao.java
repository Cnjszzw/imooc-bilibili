package com.imooc.bilibili.dao;


import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.domain.VideoTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoDao {
    Integer addVideos(Video video);

    Integer batchAddVideoTags(@Param("tagList")List<VideoTag> tagList);

    List<Video> pageListVideos(@Param("start") Integer start,@Param("limit") Integer limit,@Param("area") String area);

    Integer queryVideosTotalNum(String area);

    List<VideoTag> queryVideoTagsByVideoId(Long id);
}
