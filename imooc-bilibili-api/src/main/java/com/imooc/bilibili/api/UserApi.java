package com.imooc.bilibili.api;


import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.service.UserService;
import com.imooc.bilibili.service.util.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserApi {

    @Autowired
    private UserService userService;


    @GetMapping("/rsa-pks")
    public JsonResponse<String> getRsaPublicKey() throws Exception {
        return JsonResponse.success(RSAUtil.getPublicKeyStr());
    }
}
