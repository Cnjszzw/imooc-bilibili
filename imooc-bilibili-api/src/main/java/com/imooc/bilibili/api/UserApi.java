package com.imooc.bilibili.api;


import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.service.UserService;
import com.imooc.bilibili.service.util.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class UserApi {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSupport userSupport;

    //获取RSA公钥的接口
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

    //修改用户核心信息接口(主要是修改手机号、密码、绑定邮箱)
    @PutMapping("/users")
    public JsonResponse<String> updateUserInfo(@RequestBody User user) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        user.setId(userId);
        userService.updateUser(user);
        return JsonResponse.success();
    }

    //更新用户信息接口
    @PutMapping("/user-infos")
    public JsonResponse<String> updateUserInfo(@RequestBody UserInfo userInfo) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        userInfo.setUserId(userId);
        userService.updateUserInfo(userInfo);
        return JsonResponse.success();
    }


    //根据姓名(可选)查询所有用户（1.显示是否关注过 2.要进行分页查询 3.同时返回一共有多少搜索出来的数量）
    @GetMapping("/user-infos")
    public JsonResponse<PageResult<UserInfo>> pageListUserInfos(@RequestParam Integer no, @RequestParam Integer size , @RequestParam(required = false) String nick){
        Long userId = userSupport.getCurrentUserId();
        JSONObject params = new JSONObject();
        params.put("no",no);
        params.put("size",size);
        params.put("nick",nick);
        params.put("userId",userId);
        PageResult<UserInfo> result = userService.pageListUserInfos(params);
        return new JsonResponse<>(result);
    }

    //利用双token进行登录
    @PostMapping("/user-dts")
    public JsonResponse<Map<String,Object>> loginForDts(@RequestBody User user) throws Exception {
        Map<String,Object> res = userService.loginForDts(user);
        return new JsonResponse<>(res);
    }

    //登出接口（需要删除refreshToken）
    @DeleteMapping("/refresh-tokens")
    public JsonResponse<String> logout(HttpServletRequest request) throws Exception {
        String refreshToken = request.getHeader("refreshToken");
        Long userId = userSupport.getCurrentUserId();
        userService.logout(refreshToken,userId);
        return JsonResponse.success();
    }

    //刷新token(accessToken接口)
    @PostMapping("/access-tokens")
    public JsonResponse<String> refreshToken(HttpServletRequest request) throws Exception {
        String refreshToken = request.getHeader("refreshToken");
        String accessToken = userService.getrefreshedAccessToken(refreshToken);
        return JsonResponse.success(accessToken);
    }

}















































