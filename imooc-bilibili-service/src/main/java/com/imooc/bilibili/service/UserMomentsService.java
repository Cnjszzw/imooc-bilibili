package com.imooc.bilibili.service;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserMomentsDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.service.util.RocketMQUtil;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserFollowingService userFollowingService;

    public void addUserMoments(UserMoment userMoment) throws Exception {
        userMoment.setCreateTime(new Date());
        userMomentsDao.addUserMoments(userMoment);
        DefaultMQProducer producer = (DefaultMQProducer) applicationContext.getBean("momentsProducer");
        Message msg = new Message(UserMomentsConstant.TOPIC_MOMENTS, JSONObject.toJSONString(userMoment).getBytes(StandardCharsets.UTF_8));
        RocketMQUtil.syncSendMsg(producer, msg);
    }

    /**
     * V1 同步推送版本：发布动态后直接遍历粉丝写 Redis，不经过 MQ。
     * 用于性能对比测试——测量同步推送的完整耗时，拆解 DB 和 Redis 分别用时。
     *
     * @return long[3] {粉丝数, DB耗时ms, Redis fanout耗时ms}
     */
    public long[] addUserMomentsV1(UserMoment userMoment) {
        userMoment.setCreateTime(new Date());
        userMomentsDao.addUserMoments(userMoment);

        long dbStart = System.currentTimeMillis();
        List<UserFollowing> fanList = userFollowingService.getUserFansSimple(userMoment.getUserId());
        long dbCost = System.currentTimeMillis() - dbStart;

        long fanoutStart = System.currentTimeMillis();
        for (UserFollowing fan : fanList) {
            String key = "subscribed-" + fan.getUserId();
            String listStr = redisTemplate.opsForValue().get(key);
            List<UserMoment> list;
            if (org.springframework.util.StringUtils.isEmpty(listStr)) {
                list = new ArrayList<>();
            } else {
                list = JSONArray.parseArray(listStr, UserMoment.class);
            }
            list.add(userMoment);
            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list));
        }
        long fanoutCost = System.currentTimeMillis() - fanoutStart;

        return new long[]{fanList.size(), dbCost, fanoutCost};
    }

    public List<UserMoment> getUserSubscribedMoments(Long userId) {
        List<UserMoment> allMoments = new ArrayList<>();

        // ① 推模式收件箱：普通用户的动态
        String inboxKey = "subscribed-" + userId;
        String inboxStr = redisTemplate.opsForValue().get(inboxKey);
        List<UserMoment> pushList = JSONArray.parseArray(inboxStr, UserMoment.class);
        if (pushList != null) {
            allMoments.addAll(pushList);
        }

        // ② 拉模式：遍历关注的大 V，读取发件箱
        Set<Long> followingIds = userFollowingService.getUserFollowingIds(userId);
        for (Long followingId : followingIds) {
            String outboxKey = "outbox-" + followingId;
            List<String> outboxItems = redisTemplate.opsForList().range(outboxKey, 0, -1);
            if (outboxItems != null && !outboxItems.isEmpty()) {
                for (String item : outboxItems) {
                    UserMoment moment = JSONObject.parseObject(item, UserMoment.class);
                    allMoments.add(moment);
                }
            }
        }

        // ③ 按创建时间倒序排列
        allMoments.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return allMoments;
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
            contentDetail.setImg(contentDetail.getImg());
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
        list.forEach(moment -> newVideoList.forEach(video -> {
            if (video.getId().equals(moment.getContent().getContentDetail().getLong("id"))) {
                JSONObject contentDetail = JSONObject.parseObject(JSONObject.toJSONString(video));
                moment.getContent().setContentDetail(contentDetail);
            }
        }));
    }
}
