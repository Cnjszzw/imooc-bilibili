package com.imooc.bilibili.api;
import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.Tag;
import com.imooc.bilibili.domain.VideoTag;
import com.imooc.bilibili.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;


@RestController
public class TagApi {

    @Autowired
    private TagService tagService;

    @Autowired
    private VideoDao videoDao;

    @PostMapping("/tags")
    public JsonResponse<Long> addTag(@RequestBody Tag tag){
        Long tagId = tagService.addTag(tag);
        return new JsonResponse<>(tagId);
    }

    /**
     * 批量创建视频-标签关联（Seata 分布式事务演示）
     * 供 content-service 通过 Feign 调用，与视频入库组成 @GlobalTransactional
     */
    @PostMapping("/video-tags")
    public JsonResponse<String> batchAddVideoTags(@RequestBody List<VideoTag> tagList) {
        for (VideoTag tag : tagList) {
            tag.setCreateTime(new Date());
        }
        videoDao.batchAddVideoTags(tagList);
        return JsonResponse.success();
    }

}
