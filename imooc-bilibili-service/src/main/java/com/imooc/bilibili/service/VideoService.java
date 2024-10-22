package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.net.www.http.HttpClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VideoService {


    @Autowired
    private VideoDao videoDao;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private UserCoinService userCoinService;


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
        try {
            fastDFSUtil.viewVideoOnlineBySlices(request, response, url);
        } catch (Exception ignored) {
        }
    }

    public void viewVideoOnlineBySlicesSimple(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        fastDFSUtil.viewVideoOnlineBySlicesSimple(request, response, url);
    }

    public void addLikeVideo(Long videoId, Long userId) {
        //判断视频是否存在
        Video video = videoDao.getVideoByVideoId(videoId);
        if (video == null) {
            throw new ConditionException("视频不存在");
        }
        //判断视频是否已经点赞过
        VideoLike videoLike = videoDao.getVideoLike(userId, videoId);
        if (videoLike != null) {
            throw new ConditionException("已经点赞过");
        }
        VideoLike videoLikeNew = new VideoLike();
        videoLikeNew.setUserId(userId);
        videoLikeNew.setVideoId(videoId);
        videoLikeNew.setCreateTime(new Date());
        //用户的点赞记录入库
        videoDao.addLikeVideo(videoLikeNew);

    }

    public void deleteLikeVideo(Long videoId, Long userId) {
        //判断视频是否存在
        Video video = videoDao.getVideoByVideoId(videoId);
        if (video == null) {
            throw new ConditionException("视频不存在");
        }
        //判断视频是否已经点赞过
        VideoLike videoLike = videoDao.getVideoLike(userId, videoId);
        if (videoLike == null) {
            throw new ConditionException("没有点赞过");
        }
        videoDao.deleteLikeVideo(userId, videoId);
    }

    public Map<String, Object> getLikeVideo(Long videoId, Long userId) {
        Integer count = videoDao.getLikeVideoNum(videoId);
        Map<String, Object> map = new HashMap<>();
        map.put("count", count);
        if (userId != null) {
            Boolean liked = null;
            VideoLike videoLike = videoDao.getVideoLike(userId, videoId);
            if (videoLike != null) {
                liked = true;
            } else {
                liked = false;
            }
            map.put("like", liked);
        }
        return map;
    }

    @Transactional
    public void addVideoCollection(VideoCollection videoCollection) {
        //判断视频是否存在
        Video video = videoDao.getVideoByVideoId(videoCollection.getVideoId());
        if (video == null) {
            throw new ConditionException("视频不存在");
        }
        videoDao.delVideoCollection(videoCollection.getVideoId(),videoCollection.getUserId(),videoCollection.getGroupId());
        videoCollection.setUserId(videoCollection.getUserId());
        videoCollection.setCreateTime(new Date());
        videoDao.addVideoCollection(videoCollection);
    }

    public void deleteVideoCollection(Long videoId, Long userId) {
        videoDao.delVideoCollection(videoId, userId, null);
    }

    public Map<String, Object> getVideoCollectionCounts(Long videoId, Long userId) {
        Integer count = videoDao.queryVideoCollectionCounts(videoId);
        Map<String, Object> map = new HashMap<>();
        map.put("count", count);
        if (userId != null) {
            Boolean collected = null;
            VideoCollection videoCollection = videoDao.queryVideoCollection(videoId, userId);
            if (videoCollection != null) {
                collected = true;
            } else {
                collected = false;
            }
            map.put("collected", collected);
        }
        return map;
    }

    @Transactional
    public void addVideoCoins(VideoCoin videoCoins, Long userId) {
        //判断视频是否存在
        Video video = videoDao.getVideoByVideoId(videoCoins.getVideoId());
        if (video == null) {
            throw new ConditionException("视频不存在");
        }
        if(videoCoins.getAmount() == null ){
            throw new ConditionException("投币数量不能为空");
        }
        //判断投币的数量是否大于零
        if (videoCoins.getAmount() <= 0) {
            throw new ConditionException("投币数量小于等于0！");
        }
        //判断用户硬币数量是否大于等于要投币的数量
        Integer userCoinAmout = userCoinService.getUserCoinAmount(userId);
        if (userCoinAmout < videoCoins.getAmount()) {
            throw new ConditionException("硬币不足");
        }
        //判断用户是否投币过
        VideoCoin dbvideoCoin = userCoinService.queryVideoCoinStatus(videoCoins.getVideoId(), userId);
        if(dbvideoCoin == null){
            //没投过币，执行新增逻辑
            videoCoins.setUserId(userId);
            videoCoins.setCreateTime(new Date());
            videoCoins.setUpdateTime(new Date());
            userCoinService.addVideoCoin(videoCoins);
            userCoinService.updateUserCoinAmount(userId, userCoinAmout - videoCoins.getAmount() ,new Date());
        }else{
            //投币过，执行修改逻辑
            videoCoins.setUpdateTime(new Date());
            videoCoins.setUserId(userId);
            videoCoins.setAmount(dbvideoCoin.getAmount() + videoCoins.getAmount());
            userCoinService.updateVideoCoin(videoCoins);
            userCoinService.updateUserCoinAmount(userId, userCoinAmout - (videoCoins.getAmount() - dbvideoCoin.getAmount()),new Date());
        }
    }

    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        Integer count = videoDao.getVideoCoins(videoId,userId);
        Map<String, Object> map = new HashMap<>();
        map.put("count", count);
        if (userId != null) {
            Boolean coined = null;
            VideoCoin videoCoin = userCoinService.queryVideoCoinStatus(videoId, userId);
            if (videoCoin != null) {
                coined = true;
            } else {
                coined = false;
            }
            map.put("collected", coined);
        }
        return map;
    }
}











































