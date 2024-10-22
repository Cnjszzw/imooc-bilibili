package com.imooc.bilibili.api;


import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

}
