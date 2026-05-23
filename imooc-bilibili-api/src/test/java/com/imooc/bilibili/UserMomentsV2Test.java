package com.imooc.bilibili;

import com.alibaba.fastjson.JSONObject;
import com.imooc.ImoocBilibiliApp;
import com.imooc.bilibili.domain.Content;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.service.UserMomentsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 动态发布 V2 MQ 异步推送性能测试
 *
 * 测试目标：
 *   1. 生产者耗时——addUserMoments() 只写 MySQL + 发一条 MQ 消息，不应随粉丝数增长
 *   2. 消费者耗时——由 RocketMQConfig.momentsConsumer 内部计时，直接看控制台日志
 *
 * 前置条件：
 *   1. 关闭后端项目，避免 consumer group 抢消息
 *   2. RocketMQ NameServer + Broker 已启动
 *   3. Redis 已清空（redis-cli KEYS "subscribed-*" | xargs redis-cli DEL）
 *   4. MySQL 测试数据已就绪（发布者 10000 + N 个粉丝）
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ImoocBilibiliApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserMomentsV2Test {

    @Autowired
    private UserMomentsService userMomentsService;

    /**
     * V2 生产者耗时测试
     *
     * 发布一条动态，测量 addUserMoments 返回时间。
     * 这个方法只做：写 MySQL + 发一条 MQ 消息，不做 fanout。
     *
     * 消费者 fanout 耗时不在本方法测量——直接看控制台日志：
     *   "动态推送完成：发布者=10000，粉丝数=xxx，fanout耗时=xxx ms"
     */
    @Test
    public void testProducerOnly() throws Exception {
        // 1. 构造测试动态
        UserMoment moment = new UserMoment();
        moment.setUserId(10000L);
        moment.setType("1");

        Content content = new Content();
        JSONObject detail = new JSONObject();
        detail.put("txt", "V2异步推送测试动态");
        detail.put("img", "");
        content.setContentDetail(detail);
        moment.setContent(content);

        // 2. 生产者计时
        long start = System.currentTimeMillis();
        userMomentsService.addUserMoments(moment);
        long cost = System.currentTimeMillis() - start;

        System.out.println("==================== V2 生产者耗时 ====================");
        System.out.println("耗时: " + cost + " ms（仅写 MySQL + 发 MQ 消息）");
        System.out.println("消费者 fanout 耗时请查看后端项目日志中的 '动态推送完成' 行");
        System.out.println("=======================================================");
    }
}
