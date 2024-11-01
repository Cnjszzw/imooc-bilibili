package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.FastDFSUtil;
import com.imooc.bilibili.service.util.IpUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoService {


    @Autowired
    private VideoDao videoDao;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private UserCoinService userCoinService;

    @Autowired
    private UserService userService;


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
            map.put("hasCoin", coined);
        }
        return map;
    }

    public void addVideoComment(VideoComment videoComment) {
        //判断视频是否存在
        Video video = videoDao.getVideoByVideoId(videoComment.getVideoId());
        if (video == null) {
            throw new ConditionException("视频不存在");
        }
        videoComment.setCreateTime(new Date());
        videoComment.setUpdateTime(new Date());
        videoDao.addVideoComment(videoComment);
    }

    @Transactional
    public PageResult<VideoComment> pageListVideoComments(Integer start, Integer limit, Long videoId) {
        //判断视频是否存在
        Video video = videoDao.getVideoByVideoId(videoId);
        if (video == null) {
            throw new ConditionException("视频不存在");
        }
        Integer videoTotalCommentNum = videoDao.getVideoTotalCommentNum(videoId);
        PageResult<VideoComment> res = new PageResult<>();
        res.setTotal(videoTotalCommentNum);
        if(videoTotalCommentNum == 0){
            return res;
        }
        //查询视频评论接口
        Map<String,Object> params = new HashMap<>();
        params.put("start",start);
        params.put("limit",limit);
        params.put("videoId",videoId);
        //查询出所有的一级评论
        List<VideoComment> videoComments = videoDao.pageListVideoComments(params);
        if(videoComments.size() <= 0){
            return res;
        }
        //查询出所有的二级评论
        List<VideoComment> videoCommentReplies = videoDao.pageListVideoCommentReplies(videoId);
        //将二级评论设置到一级评论中
        for (VideoComment videoComment : videoComments) {
            List<VideoComment> childList = new ArrayList<>();
            for (VideoComment videoCommentReply : videoCommentReplies) {
                //好像这个逻辑不太对啊，应该是判断评论的id和其他评论的rrootId是否相等
//                if(videoComment.getUserId().equals(videoCommentReply.getReplyUserId())){
//                    childList.add(videoCommentReply);
//                }
                if(videoComment.getId().equals(videoCommentReply.getRootId())){
                    childList.add(videoCommentReply);
                }
            }
            videoComment.setChildList(childList);
        }
        //查询出所有用户的用户信息
        Set<Long> userList = videoComments.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
        Set<Long> userReplyList = videoCommentReplies.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
        userList.addAll(userReplyList);
        List<UserInfo> userInfoList = userService.getUserInfoByUserIds(userList);
        Map<Long, UserInfo> userInfoListMap = userInfoList.stream().collect(Collectors.toMap(UserInfo::getUserId, UserInfo -> UserInfo));
        //将用户信息设置到一级评论和二级评论中
        for (VideoComment videoComment : videoComments) {
            List<VideoComment> childList = videoComment.getChildList();
            for (VideoComment comment : childList) {
                comment.setUserInfo(userInfoListMap.get(comment.getUserId()));
                comment.setReplyUserInfo(userInfoListMap.get(comment.getReplyUserId()));
            }
            videoComment.setUserInfo(userInfoListMap.get(videoComment.getUserId()));
        }
        res.setList(videoComments);
        return res;
    }

    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video =  videoDao.getVideoDetails(videoId);
        if(video == null){
            throw new ConditionException("视频不存在");
        }
        List<VideoTag> videoTags = videoDao.getVideoTags(videoId);
        Set<Long> videoTagIds = videoTags.stream().map(VideoTag::getTagId).collect(Collectors.toSet());
        List<Tag> TagLists = videoDao.getVideoTagNamesByIds(videoTagIds);
        List<Map<String, Object>> videoTagList = new ArrayList<>();
        for (Tag tagList : TagLists) {
            Map<String, Object> tagMap = new HashMap<>();
            tagMap.put("tagId", tagList.getId());
            tagMap.put("tagName", tagList.getName());
            videoTagList.add(tagMap);
        }
        Long userId = video.getUserId();
        User user = userService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();
        Map<String, Object> result = new HashMap<>();
        result.put("video", video);
        result.put("userInfo", userInfo);
        result.put("videoTagList", videoTagList);
        return result;
    }

    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();
        //生成clientId
        String agent = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
        String clientId = String.valueOf(userAgent.getId());
        String ip = IpUtil.getIP(request);
        Map<String, Object> params = new HashMap<>();
        if(userId != null){
            params.put("userId", userId);
        }else{
            params.put("ip", ip);
            params.put("clientId", clientId);
        }
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        params.put("today", sdf.format(now));
        params.put("videoId", videoId);
        //添加观看记录
        VideoView dbVideoView = videoDao.getVideoView(params);
        if(dbVideoView == null){
            videoView.setIp(ip);
            videoView.setClientId(clientId);
            videoView.setCreateTime(new Date());
            videoDao.addVideoView(videoView);
        }
    }

    public Integer getVideoViewCounts(Long videoId) {
        return videoDao.getVideoViewCounts(videoId);
    }
}











































