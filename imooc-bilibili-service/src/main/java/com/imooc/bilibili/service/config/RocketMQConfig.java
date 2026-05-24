package com.imooc.bilibili.service.config;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserMomentsDao;
import com.imooc.bilibili.domain.UserFollowing;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.service.UserFollowingService;
import com.imooc.bilibili.service.websocket.WebSocketService;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class RocketMQConfig {

    private final Logger logger =  LoggerFactory.getLogger(this.getClass());


    @Value("${rocketmq.name.server.address}")
    private String nameServerAddr;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserFollowingService userFollowingService;

    @Autowired
    private UserMomentsDao userMomentsDao;


    @Bean("momentsProducer")
    public DefaultMQProducer momentsProducer() throws Exception{
        DefaultMQProducer producer = new DefaultMQProducer(UserMomentsConstant.GROUP_MOMENTS);
        producer.setNamesrvAddr(nameServerAddr);
        producer.start();
        return producer;
    }

    @Bean("momentsConsumer")
    public DefaultMQPushConsumer momentsConsumer() throws Exception{
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(UserMomentsConstant.GROUP_MOMENTS);
        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(UserMomentsConstant.TOPIC_MOMENTS, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context){
                MessageExt msg = msgs.get(0);
                if(msg == null){
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                long consumeStart = System.currentTimeMillis();
                System.out.println("[动态推送] 开始消费 " + new Date(consumeStart));

                String bodyStr = new String(msg.getBody());
                UserMoment userMoment = JSONObject.toJavaObject(JSONObject.parseObject(bodyStr), UserMoment.class);
                Long userId = userMoment.getUserId();

                // 查询粉丝列表（仅查 ID，不做用户信息和互关匹配）
                long dbStart = System.currentTimeMillis();
                List<UserFollowing>fanList = userFollowingService.getUserFansSimple(userId);
                long dbCost = System.currentTimeMillis() - dbStart;

                // 推拉结合：临界点 10w 粉丝
                // push 成本 = 粉丝数 × Redis SET（10w 约 7s），pull = 1 次 LPUSH（< 1ms）
                // 10w 以上大 V 占比 < 1%，走拉模式消除写扩散
                int PUSH_PULL_THRESHOLD = 100000;
                long fanoutCost;
                String mode;

                long fanoutStart = System.currentTimeMillis();
                if (fanList.size() < PUSH_PULL_THRESHOLD) {
                    // 推模式(push)：遍历粉丝写入各自收件箱 subscribed-{fanId}
                    for(UserFollowing fan : fanList){
                        String key = "subscribed-" + fan.getUserId();
                        String subscribedListStr = redisTemplate.opsForValue().get(key);
                        List<UserMoment> subscribedList;
                        if(StringUtil.isNullOrEmpty(subscribedListStr)){
                            subscribedList = new ArrayList<>();
                        }else{
                            subscribedList = JSONArray.parseArray(subscribedListStr, UserMoment.class);
                        }
                        subscribedList.add(userMoment);
                        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(subscribedList));
                    }
                    mode = "push";
                } else {
                    // 拉模式(pull)：大 V 只写自己的发件箱 outbox-{userId}，粉丝读时主动拉取
                    String outboxKey = "outbox-" + userId;
                    redisTemplate.opsForList().leftPush(outboxKey, JSONObject.toJSONString(userMoment));
                    redisTemplate.opsForList().trim(outboxKey, 0, 999);
                    mode = "pull";
                }
                fanoutCost = System.currentTimeMillis() - fanoutStart;
                long totalCost = System.currentTimeMillis() - consumeStart;

                System.out.println("[V2消费者] 消费完成 " + new Date() + " | 发布者=" + userId + " | 查粉丝=" + dbCost + "ms | 粉丝数=" + fanList.size() + " | " + mode + " | fanout=" + fanoutCost + "ms | 总耗时=" + totalCost + "ms");

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        return consumer;
    }

    /**
     * 动态持久化消费者（Phase 2 新增）
     * 接收 content-service 的 RocketMQ 事务消息，将动态写入 DB
     * 与 momentsConsumer 分属不同消费组，各自独立消费同一条消息
     */
    @Bean("momentPersistConsumer")
    public DefaultMQPushConsumer momentPersistConsumer() throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("MomentsPersistGroup");
        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(UserMomentsConstant.TOPIC_MOMENTS, "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            MessageExt msg = msgs.get(0);
            if (msg == null) {
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
            String bodyStr = new String(msg.getBody());
            UserMoment userMoment = JSONObject.toJavaObject(JSONObject.parseObject(bodyStr), UserMoment.class);
            userMoment.setCreateTime(new Date());
            userMomentsDao.addUserMoments(userMoment);
            logger.info("动态持久化完成: userId={}, type={}", userMoment.getUserId(), userMoment.getType());
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        return consumer;
    }

    @Bean("danmusProducer")
    public DefaultMQProducer danmusProducer() throws Exception{
        // 实例化消息生产者Producer
        DefaultMQProducer producer = new DefaultMQProducer(UserMomentsConstant.GROUP_DANMUS);
        // 设置NameServer的地址
        producer.setNamesrvAddr(nameServerAddr);
        // 启动Producer实例
        producer.start();
        return producer;
    }

    @Bean("danmusConsumer")
    public DefaultMQPushConsumer danmusConsumer() throws Exception{
        // 实例化消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(UserMomentsConstant.GROUP_DANMUS);
        // 设置NameServer的地址
        consumer.setNamesrvAddr(nameServerAddr);
        // 订阅一个或者多个Topic，以及Tag来过滤需要消费的消息
        consumer.subscribe(UserMomentsConstant.TOPIC_DANMUS, "*");
        // 注册回调实现类来处理从broker拉取回来的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt msg = msgs.get(0);
                byte[] msgByte = msg.getBody();
                String bodyStr = new String(msgByte);
                JSONObject jsonObject = JSONObject.parseObject(bodyStr);
                String sessionId = jsonObject.getString("sessionId");
                String message = jsonObject.getString("message");
                WebSocketService webSocketService = WebSocketService.WEBSOCKET_MAP.get(sessionId);
                if(webSocketService.getSession().isOpen()){
                    try {
                        logger.info("DM弹幕：向("+webSocketService.getUserId().toString()+")发送弹幕");
                        webSocketService.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 标记该消息已经被成功消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 启动消费者实例
        consumer.start();
        return consumer;
    }
}
