package com.imooc.bilibili.service;


import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.strategy.UserGranter;
import com.imooc.bilibili.service.strategy.UserLoginFactory;
import com.imooc.bilibili.service.util.MD5Util;
import com.imooc.bilibili.service.util.RSAUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserFollowingService userFollowingService;


    @Autowired
    private  UserAuthService userAuthService;

    @Autowired
    private UserLoginFactory userLoginFactory;

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
        //添加用户默认权限角色
        userAuthService.addUserDefaultRole(user.getId());
        return JsonResponse.success();


    }

//    public String login(User user) throws Exception {
//        //1.对用户校验：校验手机号码，校验是否存在用户
//        String phone = (user.getPhone() == null ? "" : user.getPhone());
//        String email = (user.getEmail() == null ? "" : user.getEmail());
//        if(StringUtils.isNullOrEmpty(phone) && StringUtils.isNullOrEmpty(email)){
//            throw new ConditionException("参数异常，手机号和邮箱参数都错误");
//        }
//        User dbUser = userDao.getUserByPhoneOrEmail(phone,email);
//        if (dbUser == null) {
//            throw new ConditionException("该手机号/邮箱未注册");
//        }
//        String password = user.getPassword();
//        try {
//            String rawPassword = RSAUtil.decrypt(password);
//            String md5PassWord = MD5Util.sign(rawPassword, dbUser.getSalt(), "UTF-8");
//            if(!md5PassWord.equals(dbUser.getPassword())){
//                throw new ConditionException("密码错误");
//            }
//        } catch (Exception e) {
//            throw new ConditionException("解密错误");
//        }
//        return TokenUtil.generateToken(dbUser.getId());
//
//    }

    //利用了策略工厂模式的登录接口
    public String login(User user) throws Exception {
        UserGranter granter = userLoginFactory.getGranter(user.getAccountType());
        if(granter == null){
            throw new ConditionException("不支持该登录方式");
        }
        String token = granter.login(user);
        return token;
    }

    public User getUserInfo(Long userId) {
        User user = userDao.getUserById(userId);
        UserInfo userInfo = userDao.getUserInfoByUserId(userId);
        user.setUserInfo(userInfo);
        return user;
    }

    public void updateUser(User user) throws Exception {
        Long id = user.getId();
        User dbUser = userDao.getUserById(id);
        if(dbUser == null){
            throw new ConditionException("用户不存在");
        }
        String rawPassword = user.getPassword();
        if(!StringUtils.isNullOrEmpty(rawPassword)){
            String password = RSAUtil.decrypt(rawPassword);
            String salt = dbUser.getSalt();
            String md5Password = MD5Util.sign(password, salt, "UTF-8");
            user.setPassword(md5Password);
        }
        user.setUpdateTime(new Date());
        userDao.updateUser(user);
    }

    public void updateUserInfo(UserInfo userInfo) {
        userInfo.setUpdateTime(new Date());
        userDao.updateUserInfo(userInfo);
    }

    public User getUserById(Long id) {
        return userDao.getUserById(id);
    }


    public List<UserInfo> getUserInfoByIds(Set<Long> userFollowingsIds) {
        return userDao.getUserInfoByIds(userFollowingsIds);
    }

    public PageResult<UserInfo> pageListUserInfos(JSONObject params) {
        Integer total = this.pageCountUserInfos(params);
        PageResult<UserInfo> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        if (total > 0) {
            List<UserInfo> userInfos = userDao.pageListUserInfos(params);
            Set<Long> UserFollowingIds = userFollowingService.getUserFollowingIds(Long.valueOf(params.getString("userId")));
            for (UserInfo userInfo : userInfos) {
                userInfo.setFollowed(false);
            }
            for (UserInfo userInfo : userInfos) {
                for (Long userFollowingId : UserFollowingIds) {
                    if (userInfo.getUserId().equals(userFollowingId))
                        userInfo.setFollowed(true);
                }
            }
            pageResult.setList(userInfos);
        }
        return pageResult;
    }


    public Integer pageCountUserInfos(JSONObject params) {
        return userDao.pageCountUserInfos(params);
    }

    @Transactional
    public Map<String, Object> loginForDts(User user) throws Exception {
        //1.对用户校验：校验手机号码，校验是否存在用户
        String phone = (user.getPhone() == null ? "" : user.getPhone());
        String email = (user.getEmail() == null ? "" : user.getEmail());
        if(StringUtils.isNullOrEmpty(phone) && StringUtils.isNullOrEmpty(email)){
            throw new ConditionException("参数异常，手机号和邮箱参数都错误");
        }
        User dbUser = userDao.getUserByPhoneOrEmail(phone,email);
        if (dbUser == null) {
            throw new ConditionException("该手机号/邮箱未注册");
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
        Long userId = dbUser.getId();
        String acessToken = TokenUtil.generateToken(userId);
        String refreshToken = TokenUtil.generateRefreshToken(userId);
        HashMap<String, Object> result = new HashMap<>();
        result.put("accessToken", acessToken);
        result.put("refreshToken", refreshToken);
        deleteRefreshToken(refreshToken);
        addRefreshToken(refreshToken,userId,new Date());
        return result;
    }


    public void deleteRefreshToken(String refreshToken) {
        userDao.deleteRefreshToken(refreshToken);
    }

    public void addRefreshToken(String refreshToken, Long userId,Date createTime) {
        userDao.addRefreshToken(refreshToken, userId,createTime);
    }

    public void logout(String refreshToken, Long userId) {
        deleteRefreshToken(refreshToken);
    }

    public String getrefreshedAccessToken(String refreshToken) throws Exception {
        userDao.getRefreshTokenDetailByRefreshToken(refreshToken);
        if (refreshToken == null) {
            throw new ConditionException("555","token过期");
        }
        if (TokenUtil.verifyToken(refreshToken) == null) {
            throw new ConditionException("555","token过期");
        }
        return TokenUtil.generateToken(TokenUtil.verifyToken(refreshToken));
    }

    public List<UserInfo> getUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.getUserInfoByUserIds(userIdList);
    }
}








































