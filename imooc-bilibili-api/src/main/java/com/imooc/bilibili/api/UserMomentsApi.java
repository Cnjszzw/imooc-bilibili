package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.domain.annotation.ApiLimitedRole;
import com.imooc.bilibili.domain.annotation.DataLimited;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import com.imooc.bilibili.service.UserMomentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserMomentsApi {


    @Autowired
    private UserMomentsService userMomentsService;

    @Autowired
    private UserSupport userSupport;


    //发布动态接口
    @DataLimited//数据库权限控制 等级为1和等级为0的用户只能发布type1或者0的动态
    @ApiLimitedRole(limitedRoleCodeList = {AuthRoleConstant.ROLE_LV0})//备注：Lv0的用户无法发布动态
    @PostMapping("/user-moments")
    public JsonResponse<String> addUserMoments(@RequestBody UserMoment userMoment) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        userMoment.setUserId(userId);
        userMomentsService.addUserMoments(userMoment);
        return JsonResponse.success();
    }

    //获取发布动态的接口
    @GetMapping("/user-subscribed-moments")
    public JsonResponse<List<UserMoment>> getUserSubscribedMoments() {
        Long userId = userSupport.getCurrentUserId();
        List<UserMoment> list = userMomentsService.getUserSubscribedMoments(userId);
        return new JsonResponse<>(list);
    }

    @GetMapping("/moments")
    public JsonResponse<PageResult<UserMoment>> pageListMoments(@RequestParam("size") Integer size,
                                                                @RequestParam("no") Integer no,
                                                                String type) {
        try {
            Long userId = userSupport.getCurrentUserId();
            PageResult<UserMoment> list = userMomentsService.pageListMoments(size, no, userId, type);
            return new JsonResponse<>(list);
        } catch (Exception e) {
            // 处理其他可能的异常
            return new JsonResponse<>(new PageResult<UserMoment>());
        }
    }
}
