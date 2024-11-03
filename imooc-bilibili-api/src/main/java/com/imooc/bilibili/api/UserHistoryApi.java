package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.UserVideoHistory;
import com.imooc.bilibili.service.UserHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserHistoryApi {


    @Autowired
    private UserHistoryService userHistoryService;

    @Autowired
    private UserSupport userSupport;

    @GetMapping("/user-video-histories")
    public JsonResponse<PageResult<UserVideoHistory>> pagListUserVideoHistory(@RequestParam Integer size,
                                                                              @RequestParam Integer no){
        Long userId = userSupport.getCurrentUserId();
        PageResult<UserVideoHistory> result = userHistoryService.pagListUserVideoHistory(size,no,userId);
        return new JsonResponse<>(result);
    }

}
