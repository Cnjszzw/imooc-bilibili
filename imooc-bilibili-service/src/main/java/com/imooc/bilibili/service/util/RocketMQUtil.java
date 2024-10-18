package com.imooc.bilibili.service.util;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.CountDownLatch2;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RocketMQUtil {

    public static void syncSendMsg(DefaultMQProducer producer, Message msg) throws Exception{
        producer.setVipChannelEnabled(false);
        SendResult result = producer.send(msg);
        System.out.println(result);
    }

    public static void asyncSendMsg(DefaultMQProducer producer, Message msg) throws Exception{

        // 初始化消息计数器为2
        int messageCount = 2;

        // 创建一个CountDownLatch对象，用于等待消息发送完成
        CountDownLatch2 countDownLatch = new CountDownLatch2(messageCount);
        for (int i = 0; i < messageCount; i++) {
            // 发送消息，并注册回调函数
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    // 如果消息发送成功，计数器减一
                    countDownLatch.countDown();
                    // 打印消息ID
                    System.out.println(sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    // 如果消息发送出现异常，计数器也减一
                    countDownLatch.countDown();
                    // 打印异常信息
                    System.out.println("发送消息的时候发生了异常!" + e);
                    // 打印堆栈跟踪信息
                    e.printStackTrace();
                }
            });
        }
        // 等待所有消息发送完成
        countDownLatch.await(5, TimeUnit.SECONDS);


    }
}
