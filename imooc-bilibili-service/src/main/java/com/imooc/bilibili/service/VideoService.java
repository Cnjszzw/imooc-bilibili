package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.domain.VideoTag;
import com.imooc.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.net.www.http.HttpClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class VideoService {


    @Autowired
    private VideoDao videoDao;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Transactional
    public void addVideos(Video video) {
        video.setCreateTime(new Date());
        video.setUpdateTime(new Date());
        videoDao.addVideos(video);
        List<VideoTag> tagList = video.getVideoTagList();
        for (VideoTag tag : tagList) {
            tag.setVideoId(video.getId());
            tag.setCreateTime(new Date());
        }
        videoDao.batchAddVideoTags(tagList);
    }

    public PageResult<Video> pageListVideos(Integer start, Integer limit, String area) {
        List<Video> videoList = videoDao.pageListVideos(start, limit, area);
        for (Video video : videoList) {
            List<VideoTag> tagList = videoDao.queryVideoTagsByVideoId(video.getId());
            video.setVideoTagList(tagList);
        }
        PageResult<Video> pageResult = new PageResult<>();
        pageResult.setList(videoList);
        Integer totalNUm = videoDao.queryVideosTotalNum(area);
        pageResult.setTotal(totalNUm);
        return pageResult;
    }

    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) {
        try{
            fastDFSUtil.viewVideoOnlineBySlices(request, response, url);
        }catch (Exception ignored){}
    }

    public void viewVideoOnlineBySlicesSimple(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        fastDFSUtil.viewVideoOnlineBySlicesSimple(request, response, url);
    }
}
