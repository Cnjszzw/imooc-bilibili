package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.service.UserCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UserCenterApi {

    @Autowired
    private UserCenterService userCenterService;

    @Autowired
    private UserSupport userSupport;

    /**
     * 查询视频分区
     */
    @GetMapping("/user-center-video-areas")
    public JsonResponse<Map<String, Integer>> getUserCenterVideoAreas(){
        Long userId = userSupport.getCurrentUserId();
        Map<String, Integer> result = userCenterService.getUserCenterVideoAreas(userId);
        return new JsonResponse<>(result);
    }

    /**
     * 分页查询用户视频
     */
    @GetMapping("/user-center-videos")
    public JsonResponse<PageResult<Video>> pageListUserVideos(@RequestParam Integer size,
                                                              @RequestParam Integer no,
                                                              String area){
        Long userId = userSupport.getCurrentUserId();
        PageResult<Video> result = userCenterService.pageListUserVideos(size, no,
                                                                        area, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 分页查询用户空间收藏
     */
    @GetMapping("/user-center-collections")
    public JsonResponse<Map<String, Object>> pageListUserCenterCollections(@RequestParam Integer size,
                                                                           @RequestParam Integer no,
                                                                           Long groupId){
        Long userId = userSupport.getCurrentUserId();
        Map<String, Object> result = userCenterService.pageListUserCenterCollections(size, no, userId, groupId);
        return new JsonResponse<>(result);
    }

    /**
     * 添加收藏分组
     */
    @PostMapping("/user-center-collection-groups")
    public JsonResponse<String> addUserCollectionGroups(@RequestBody VideoCollectionGroup videoCollectionGroup){
        Long userId = userSupport.getCurrentUserId();
        videoCollectionGroup.setUserId(userId);
        userCenterService.addUserCollectionGroups(videoCollectionGroup);
        return JsonResponse.success();
    }

    /**
     * 查询用户关注分组
     */
    @GetMapping("/user-center-following-groups")
    public JsonResponse<List<FollowingGroup>> getUserCenterFollowingGroups(){
        Long userId = userSupport.getCurrentUserId();
        List<FollowingGroup> result =  userCenterService.getUserCenterFollowingGroups(userId);
        return new JsonResponse<>(result);
    }

    /**
     * 分页查询用户关注
     */
    @GetMapping("/user-center-followings")
    public JsonResponse<PageResult<UserFollowing>> pageListUserCenterFollowings(@RequestParam Integer size,
                                                                          @RequestParam Integer no,
                                                                          Long groupId){
        Long userId = userSupport.getCurrentUserId();
        PageResult<UserFollowing> list = userCenterService.pageListUserCenterFollowings(userId, size,
                no ,groupId);
        return new JsonResponse<>(list);
    }

    /**
     * 分页查询用户粉丝
     */
    @GetMapping("/user-center-fans")
    public JsonResponse<PageResult<UserFollowing>> pageListUserFans(@RequestParam Integer size,
                                                                    @RequestParam Integer no){
        Long userId = userSupport.getCurrentUserId();
        PageResult<UserFollowing> result = userCenterService.pageListUserFans(userId, size, no);
        return new JsonResponse<>(result);
    }

    /**
     * 计算用户粉丝总数
     */
    @GetMapping("/user-center-fan-counts")
    public JsonResponse<Long> countUserFans(){
        Long userId = userSupport.getCurrentUserId();
        Long result = userCenterService.countUserFans(userId);
        return new JsonResponse<>(result);
    }

}
