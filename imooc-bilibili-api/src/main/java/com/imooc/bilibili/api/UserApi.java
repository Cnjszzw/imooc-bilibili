package com.imooc.bilibili.api;


import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.service.UserService;
import com.imooc.bilibili.service.util.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserApi {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSupport userSupport;

    @GetMapping("/rsa-pks")
    public JsonResponse<String> getRsaPublicKey() throws Exception {
        return JsonResponse.success(RSAUtil.getPublicKeyStr());
    }

    //注册的接口
    @PostMapping("/users")
    public JsonResponse<String> addUser(@RequestBody User user){
        return userService.addUser(user);
    }

    //登录的接口
    @PostMapping("/user-tokens")
    public JsonResponse<String> login(@RequestBody User user) throws Exception {
        String token = userService.login(user);
        return JsonResponse.success(token);
    }

    //获取用户信息接口
    @GetMapping("/users")
    public JsonResponse<User> getUserInfo(){
        Long userId = userSupport.getCurrentUserId();
        User user = userService.getUserInfo(userId);
        return new JsonResponse(user);
    }

}















































