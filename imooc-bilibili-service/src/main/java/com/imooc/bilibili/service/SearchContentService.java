package com.imooc.bilibili.service;

import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SearchContentService {

    @Autowired
    private ElasticSearchService elasticSearchService;

    public Map<String, Object> countBySearchTxt(String searchTxt) {
        Map<String, Object> result = new HashMap<>();
        //算视频
        long videoCount = elasticSearchService.countVideoBySearchTxt(searchTxt);
        //算用户
        long userCount = elasticSearchService.countUserBySearchTxt(searchTxt);
        //构建返回结果
        result.put("videoCount", videoCount);
        result.put("userCount", userCount);
        return result;
    }

    public Page<Video> pageListSearchVideos(String keyword,Integer pageSize,
                                            Integer pageNo, String searchType) throws IOException {
        Page<Video> result = elasticSearchService.pageListSearchVideos(keyword, pageSize,
                                                                pageNo-1, searchType);
        return result;
    }

    public Page<UserInfo> pageListSearchUsers(String keyword, Integer pageSize,
                                              Integer pageNo, String searchType) throws IOException {
        Page<UserInfo> result = elasticSearchService.pageListSearchUsers(keyword, pageSize,
                                                                pageNo-1, searchType);
        return result;
    }
}
