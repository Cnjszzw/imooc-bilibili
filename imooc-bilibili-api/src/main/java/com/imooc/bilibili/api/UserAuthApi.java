package com.imooc.bilibili.api;


import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.auth.UserAuthorities;
import com.imooc.bilibili.service.UserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAuthApi {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserAuthService userAuthService;

    //获取用户相关权限接口（主要是前端来控制，接口和数据的权限控制主要是通过AOP切面编程来实现）
    @GetMapping("/user-authorities")
    public JsonResponse<UserAuthorities> getUserAuthorities() {
        Long userId = userSupport.getCurrentUserId();
        UserAuthorities userAuthorities = userAuthService.getUserAuthorities(userId);
        return new JsonResponse<>(userAuthorities);
    }
}
