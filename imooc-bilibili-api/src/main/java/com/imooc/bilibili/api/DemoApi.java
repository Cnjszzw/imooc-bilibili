package com.imooc.bilibili.api;


import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.service.DemoService;
import com.imooc.bilibili.service.ElasticSearchService;
import com.imooc.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DemoApi {

    @Autowired
    private DemoService demoService;

    @Autowired
    private FastDFSUtil fastDFSUtil;

    @Autowired
    private ElasticSearchService elasticSearchService;

    @GetMapping("/query")
    public Long query(Long id) {
        return demoService.query(id);
    }

    @GetMapping("/slices")
    public void slices(MultipartFile file) throws Exception {
        fastDFSUtil.convertFileToSlices(file);
    }

    @GetMapping("/es-videos")
    public JsonResponse<Video> getEsVideos(@RequestParam String keyword){
        Video video = elasticSearchService.getVideos(keyword);
        return new JsonResponse<>(video);
    }

    @PostMapping("/es-videos")
    public JsonResponse<String> addVideos(@RequestBody Video video){
        elasticSearchService.addVideo(video);
        return JsonResponse.success();
    }

    @DeleteMapping("/es-videos")
    public JsonResponse<String> deleteVideos(){
        elasticSearchService.deleteAllVideos();
        return JsonResponse.success();
    }

    @PostMapping("/es-users")
    public JsonResponse<String> addUsers(@RequestBody UserInfo userInfo){
        elasticSearchService.addUserInfo(userInfo);
        return JsonResponse.success();
    }

}
