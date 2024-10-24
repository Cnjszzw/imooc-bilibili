package com.imooc.bilibili.dao;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface UserDao {

    User getUserByPhone(String phone);

    Integer addUser(User user);

    Integer addUserInfo(UserInfo userInfo);

    User getUserById(Long id);

    UserInfo getUserInfoByUserId(Long userId);

    Integer updateUser(User user);

    User getUserByPhoneOrEmail(@Param("phone") String phone, @Param("email")String email);

    Integer updateUserInfo(UserInfo userInfo);

    List<UserInfo> getUserInfoByIds(@Param("userFollowingsIds") Set<Long> userFollowingsIds);

    Integer pageCountUserInfos(Map<String,Object> params);

    List<UserInfo> pageListUserInfos(Map<String,Object> params);

    Integer deleteRefreshToken(String refreshToken);

    Integer addRefreshToken(@Param("refreshToken") String refreshToken,
                            @Param("userId")Long userId,
                            @Param("createTime")Date createTime);

    RefreshTokenDetail getRefreshTokenDetailByRefreshToken(String refreshToken);

    List<UserInfo> getUserInfoByUserIds(@Param("userIdList") Set<Long> userIdList);
}
