package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.UserDao;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.MD5Util;
import com.imooc.bilibili.service.util.RSAUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    public JsonResponse<String> addUser(User user) {
        //1.对传入的user进行校验，主要判断手机号的格式，并且判断没有注册过
        String phone = user.getPhone();
        if(StringUtils.isNullOrEmpty(phone)){
            throw new ConditionException("手机号不能为空");
        }
        if(userDao.getUserByPhone(phone) != null){
            throw new ConditionException("该手机号已注册");
        }
        //2.经过校验，可以进行进一步的注册
        String rawPassword = user.getPassword();
        try {
            String password = RSAUtil.decrypt(rawPassword);
            String salt = String.valueOf(new Date().getTime());
            String md5EncodedPassword = MD5Util.sign(password, salt, "UTF-8");
            user.setSalt(salt);
            user.setPassword(md5EncodedPassword);
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
        } catch (Exception e) {
            throw new ConditionException("密码解密失败");
        }
        //入库t_user表
        userDao.addUser(user);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setNick(UserConstant.DEFAULT_NICK);
        userInfo.setGender(UserConstant.GENDER_UNKNOW);
        userInfo.setBirth(UserConstant.DEFAULT_BIRTH);
        userInfo.setCreateTime(new Date());
        userInfo.setUpdateTime(new Date());
        //入库t_user_info表
        userDao.addUserInfo(userInfo);

        return JsonResponse.success();


    }

    public String login(User user) throws Exception {
        //1.对用户校验：校验手机号码，校验是否存在用户
        String phone = user.getPhone();
        if(StringUtils.isNullOrEmpty(phone)){
            throw new ConditionException("手机号不能为空");
        }
        User dbUser = userDao.getUserByPhone(phone);
        if (dbUser == null) {
            throw new ConditionException("该手机号未注册");
        }
        String password = user.getPassword();
        try {
            String rawPassword = RSAUtil.decrypt(password);
            String md5PassWord = MD5Util.sign(rawPassword, dbUser.getSalt(), "UTF-8");
            if(!md5PassWord.equals(dbUser.getPassword())){
                throw new ConditionException("密码错误");
            }
        } catch (Exception e) {
            throw new ConditionException("解密错误");
        }
        return TokenUtil.generateToken(dbUser.getId());

    }

    public User getUserInfo(Long userId) {
        User user = userDao.getUserById(userId);
        UserInfo userInfo = userDao.getUserInfoByUserId(userId);
        user.setUserInfo(userInfo);
        return user;
    }
}








































