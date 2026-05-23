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
 * 动态发布 V1 同步推送性能测试
 *
 * 测试目标：不使用 MQ，发布动态后直接同步遍历所有粉丝写入 Redis，
 * 测量完整耗时并拆解 DB 和 Redis 分别用时。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ImoocBilibiliApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserMomentsV1Test {

    @Autowired
    private UserMomentsService userMomentsService;

    @Test
    public void testSyncPushV1() {
        UserMoment moment = new UserMoment();
        moment.setUserId(10000L);
        moment.setType("1");

        Content content = new Content();
        JSONObject detail = new JSONObject();
        detail.put("txt", "V1同步推送测试动态");
        detail.put("img", "");
        content.setContentDetail(detail);
        moment.setContent(content);

        long totalStart = System.currentTimeMillis();
        long[] result = userMomentsService.addUserMomentsV1(moment);
        long totalCost = System.currentTimeMillis() - totalStart;

        long fanCount = result[0];
        long dbCost = result[1];
        long fanoutCost = result[2];

        System.out.println("==================== V1 同步推送测试结果 ====================");
        System.out.println("粉丝数量: " + fanCount);
        System.out.println("DB 查粉丝耗时: " + dbCost + " ms");
        System.out.println("Redis fanout 耗时: " + fanoutCost + " ms");
        System.out.println("总耗时: " + totalCost + " ms");
        System.out.println("============================================================");
    }
}
