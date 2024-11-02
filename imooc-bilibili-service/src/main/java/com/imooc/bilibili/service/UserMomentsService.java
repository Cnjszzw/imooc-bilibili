package com.imooc.bilibili.service;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserMomentsDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.service.util.RocketMQUtil;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserMomentsService {

    @Autowired
    UserMomentsDao userMomentsDao;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Value("${fdfs.http.storage-addr}")
    private String fastdfsUrl;

    public void addUserMoments(UserMoment userMoment) throws Exception {
        userMoment.setCreateTime(new Date());
        userMomentsDao.addUserMoments(userMoment);
        DefaultMQProducer producer = (DefaultMQProducer) applicationContext.getBean("momentsProducer");
        Message msg = new Message(UserMomentsConstant.TOPIC_MOMENTS, JSONObject.toJSONString(userMoment).getBytes(StandardCharsets.UTF_8));
        RocketMQUtil.syncSendMsg(producer, msg);
    }

    public List<UserMoment> getUserSubscribedMoments(Long userId) {
        String key = "subscribed-" + userId;
        String listStr = redisTemplate.opsForValue().get(key);
        return JSONArray.parseArray(listStr, UserMoment.class);
    }

    public PageResult<UserMoment> pageListMoments(Integer size, Integer no,
                                                  Long userId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no - 1) * size);
        params.put("limit", size);
        params.put("userId", userId);
        params.put("type", type);
        Integer total = userMomentsDao.pageCountMoments(params);
        List<UserMoment> list = new ArrayList<>();
        if (total > 0) {
            list = userMomentsDao.pageListMoments(params);
            if (!list.isEmpty()) {
                //处理不同类型的动态
                this.processVideoMoment(list.stream()
                        .filter(item -> UserMomentsConstant.TYPE_VIDEO
                                .equals(item.getType())).collect(Collectors.toList()));
                this.processImgMoment(list.stream()
                        .filter(item -> UserMomentsConstant.TYPE_IMG
                                .equals(item.getType())).collect(Collectors.toList()));
                //匹配对应用户信息
                Set<Long> userIdSet = list.stream()
                        .map(UserMoment::getUserId).collect(Collectors.toSet());
                List<UserInfo> userInfoList = userService.getUserInfoByUserIds(userIdSet);
                list.forEach(moment -> userInfoList.forEach(userInfo -> {
                    if (moment.getUserId().equals(userInfo.getUserId())) {
                        moment.setUserInfo(userInfo);
                    }
                }));
            }
        }
        return new PageResult<>(total, list);
    }

    private void processImgMoment(List<UserMoment> list) {
        list.forEach(moment -> {
            Content content = moment.getContent();
            ImgContent contentDetail = content.getContentDetail().toJavaObject(ImgContent.class);
            contentDetail.setImg(fastdfsUrl + contentDetail.getImg());
            content.setContentDetail(JSONObject.parseObject(JSONObject.toJSONString(contentDetail)));
            moment.setContent(content);
        });
    }

    private void processVideoMoment(List<UserMoment> list) {
        List<Video> videoList = list.stream()
                .map(UserMoment::getContent)
                .map(content -> content.getContentDetail().toJavaObject(Video.class))
                .collect(Collectors.toList());
        List<Video> newVideoList = videoService.getVideoCount(videoList);
        newVideoList.forEach(video -> video.setThumbnail(fastdfsUrl + video.getThumbnail()));
        list.forEach(moment -> newVideoList.forEach(video -> {
            if (video.getId().equals(moment.getContent().getContentDetail().getLong("id"))) {
                JSONObject contentDetail = JSONObject.parseObject(JSONObject.toJSONString(video));
                moment.getContent().setContentDetail(contentDetail);
            }
        }));
    }
}
