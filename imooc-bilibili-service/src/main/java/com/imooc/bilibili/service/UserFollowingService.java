package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.UserFollowingDao;
import com.imooc.bilibili.domain.FollowingGroup;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserFollowing;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserFollowingService {

    @Autowired
    private UserFollowingDao userFollowingDao;

    @Autowired
    private FollowingGroupService followingGroupService;

    @Autowired
    private UserService userService;

    //添加用户关注的方法
    //基本流程是，前端第一次设置用户只能加入默认的收藏夹，后续可以修改
    @Transactional
    public void addUserFollowings(UserFollowing userFollowing) {
        //1.判断有没有传入groupId，没有就是第一次收藏，直接放入到默认的收藏夹就好了
        Long groupId = userFollowing.getGroupId();
        if (groupId == null) {
            //前端没有传入group_id , 查询默认的收藏夹
            FollowingGroup followingGroup =  followingGroupService.getByType(UserConstant.USER_FOLLOWING_GROUP_TYPE_DEFAULT);
            userFollowing.setGroupId(followingGroup.getId());
        }else{
            //前端传入了group_id，说明修改收藏夹，做一个非法校验？判断这个收藏夹有没有
            FollowingGroup followingGroup = followingGroupService.getById(groupId);
            if(followingGroup == null){
                throw new ConditionException("要添加的收藏夹不存在");
            }
        }
        //判断关注的人是否存在，如果不存在就无法关注
        Long followingId = userFollowing.getFollowingId();
        User user = userService.getUserById(followingId);
        if(user == null){
            throw new ConditionException("要关注的人不存在");
        }
        //进行关注，进行入库操作，要注意，关注的逻辑没有更新一说，无论是第一次关注，还是第二次修改关注，都是先删除，再添加
        userFollowingDao.deleteUserFollowing(userFollowing.getUserId(),userFollowing.getFollowingId());
        userFollowing.setCreateTime(new Date());
        userFollowingDao.addUserFollowing(userFollowing);
    }

    //获取用户关注列表
    //第一步：获取关注的用户列表
    //第二步：根据关注用户的id查询关注用户的基本信息
    //第三步；将关注用户按关注分组进行分类
    public List<FollowingGroup> getUserFollowings(Long userId) {
        //1.获取到所有的关注的用户id
        List<UserFollowing> userFollowings = userFollowingDao.getUserFollowings(userId);
        Set<Long> userFollowingsIds = userFollowings.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        //2.查询出所有的关注用户的信息
        if(userFollowingsIds == null || userFollowingsIds.size() <= 0){
            throw new ConditionException("关注用户列表为空");
        }
        List<UserInfo> userInfos = userService.getUserInfoByIds(userFollowingsIds);
        for (UserFollowing userFollowing : userFollowings) {
            for (UserInfo userInfo : userInfos) {
                if(userFollowing.getFollowingId().equals(userInfo.getUserId()))
                    userFollowing.setUserInfo(userInfo);
            }
        }
        //3.将关注用户按关注分组进行分类,记住这里自己添加一个分类:全部关注
        List<FollowingGroup> followingGroups = followingGroupService.getByUserId(userId);
        FollowingGroup allGroup = new FollowingGroup();
        allGroup.setName(UserConstant.USER_FOLLOWING_GROUP_ALL_NAME);
        allGroup.setUserId(userId);
        allGroup.setFollowingUserInfoList(userInfos);
        for (FollowingGroup followingGroup : followingGroups) {
            List<UserInfo> followingUserInfoList = new ArrayList<>();
            for (UserFollowing userFollowing : userFollowings) {
                if(followingGroup.getId().equals(userFollowing.getGroupId())){
                    followingUserInfoList.add(userFollowing.getUserInfo());
                }
            }
            followingGroup.setFollowingUserInfoList(followingUserInfoList);
        }
        followingGroups.add(allGroup);
        return followingGroups;
    }

    //获取用户粉丝列表(包含互相关注的功能)
    //第一步：获取当前用户的粉丝列表
    //第二步骤：根据粉丝的用户id查询基本信息
    //第三步骤：查询当前用户是否关注该粉丝
    public List<UserFollowing> getUserFans(Long userId) {
        //第一步骤：查询到所有的粉丝id
        List<UserFollowing> userFanFollowings = userFollowingDao.getUserFanFollowings(userId);
        Set<Long> fanIds = userFanFollowings.stream().map(UserFollowing::getUserId).collect(Collectors.toSet());
        if(fanIds == null || fanIds.size() <= 0){
            return userFanFollowings;
        }
        //第二步骤：查询出所有的粉丝信息
        List<UserInfo> fanInfos = userService.getUserInfoByIds(fanIds);
        //第三步骤：查询当前用户是否关注该粉丝
        //查询一下该用户关注的所有用户id
        List<UserFollowing> userFollowings = userFollowingDao.getUserFollowings(userId);
        Set<Long> followingIds = userFollowings.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        for (UserFollowing userFanFollowing : userFanFollowings) {
            //初始化信息
            for (UserInfo fanInfo : fanInfos) {
                if(userFanFollowing.getUserId().equals(fanInfo.getUserId())){
                    fanInfo.setFollowed(false);
                    userFanFollowing.setUserInfo(fanInfo);
                }
            }

            for (Long followingId : followingIds) {
                if(userFanFollowing.getUserId().equals(followingId))
                    userFanFollowing.getUserInfo().setFollowed(true);
            }

        }

        return userFanFollowings;
    }

    public Long addUserFollowingGroups(FollowingGroup followingGroup) {
        followingGroupService.addFollowingGroup(followingGroup);
        return followingGroup.getId();
    }

    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupService.getByUserId(userId);
    }

    //获取用户关注列表（仅仅返回id集合）
    public Set<Long> getUserFollowingIds(Long userId){
        List<UserFollowing> userFollowings = userFollowingDao.getUserFollowings(userId);
        Set<Long> userFollowingsIds = userFollowings.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        return userFollowingsIds;
    }


}





















































































