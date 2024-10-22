package com.imooc.bilibili.api;


import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.domain.VideoCollection;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@RestController
public class VideoApi {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private VideoService videoService;


    /**
     * 视频投稿接口
     */
    @PostMapping("/videos")
    public JsonResponse<String> addVideos(@RequestBody Video video){
        Long userId = userSupport.getCurrentUserId();
        video.setUserId(userId);
        videoService.addVideos(video);
        return JsonResponse.success();


    }

    /**
     * 分页查询视频列表接口
     */
    @GetMapping("/videos")
    public JsonResponse<PageResult<Video>> pageListVideos(@RequestParam Integer size,
                                                          @RequestParam Integer no,
                                                          String area){
        Integer start = (no - 1) * size;
        Integer limit = size;
        PageResult<Video> res = videoService.pageListVideos(start,limit,area);
        return new JsonResponse<>(res);
    }

    /**
     * 视频在线播放
     */
    @GetMapping("/video-slices")
    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) {
        videoService.viewVideoOnlineBySlices(request, response, url);
    }

    /**
     * 视频在线播放(simple)
     */
    @GetMapping("/video-slices-simple")
    public void viewVideoOnlineBySlicesSimple(HttpServletRequest request,HttpServletResponse response,String url) throws Exception {
        videoService.viewVideoOnlineBySlicesSimple(request,response,url);
    }

    /**
     * 点赞视频接口
     * 难点：如何进行非法校验？
     */
    @PostMapping("/video-likes")
    public JsonResponse<String> addLikeVideo(Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.addLikeVideo(videoId,userId);
        return JsonResponse.success();
    }

    /**
     * 取消点赞视频接口
     */
    @DeleteMapping("/video-likes")
    public JsonResponse<String> deleteLikeVideo(Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteLikeVideo(videoId,userId);
        return JsonResponse.success();
    }

    /**
     * 查询视频点赞的接口
     * 难点：如何进行登录和未登录用户的兼容，扽登录用户显示点赞状态
     */
    @GetMapping("/video-likes")
    public JsonResponse<Map<String,Object>> getLikeVideo(@RequestParam Long videoId){
        Long userId  = null;
        try {
            userId = userSupport.getCurrentUserId();
        }catch(Exception ignore){}
        Map<String,Object> map = videoService.getLikeVideo(videoId,userId);
        return new JsonResponse<>(map);
    }

    /**
     * 视频收藏接口，也可以是视频更新接口
     * 难点：同样要做非法校验
     */
    @PostMapping("/video-collections")
    public JsonResponse<String> addVideoCollection(@RequestBody VideoCollection videoCollection){
        Long userId = userSupport.getCurrentUserId();
        videoCollection.setUserId(userId);
        videoService.addVideoCollection(videoCollection);
        return JsonResponse.success();
    }

    /**
     * 视频更新接口
     */
    @PutMapping("/video-collections")
    public JsonResponse<String> updateVideoCollection(@RequestBody VideoCollection videoCollection){
        Long userId = userSupport.getCurrentUserId();
        videoCollection.setUserId(userId);
        videoService.addVideoCollection(videoCollection);
        return JsonResponse.success();
    }

    /**
     * 取消收藏视频接口
     */
    @DeleteMapping("/video-collections")
    public JsonResponse<String> deleteVideoCollection(@RequestParam Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoCollection(videoId,userId);
        return JsonResponse.success();
    }

    /**
     * 获取视频数量接口
     */
    @GetMapping("/video-collections")
    public JsonResponse<Map<String,Object>> getVideoCollectionCounts(@RequestParam Long videoId){
        Long userId = null;
        try{
            userId = userSupport.getCurrentUserId();
        }catch(Exception ignore){}
        Map<String,Object> map = videoService.getVideoCollectionCounts(videoId,userId);
        return new JsonResponse<>(map);
    }













































}