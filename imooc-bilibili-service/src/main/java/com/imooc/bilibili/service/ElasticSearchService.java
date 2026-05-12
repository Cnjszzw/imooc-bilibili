package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.repository.UserInfoRepository;
import com.imooc.bilibili.dao.repository.VideoRepository;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.domain.constant.SearchConstant;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ElasticSearchService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public void addUserInfo(UserInfo userInfo){
        userInfoRepository.save(userInfo);
    }

    public void addVideo(Video video){
        videoRepository.save(video);
    }


    public List<Map<String, Object>> getContents(String keyword,
                                                 Integer pageNo,
                                                 Integer pageSize) throws IOException {
        String[] indices = {"videos", "user-infos"};
        SearchRequest searchRequest = new SearchRequest(indices);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //分页
        sourceBuilder.from(pageNo - 1);
        sourceBuilder.size(pageSize);
        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "nick", "description");
        sourceBuilder.query(matchQueryBuilder);
        searchRequest.source(sourceBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        //高亮显示
        String[] array = {"title", "nick", "description"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for(String key : array){
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false); //如果要多个字段进行高亮，要为false
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        //执行搜索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String, Object>> arrayList = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            //处理高亮字段
            Map<String, HighlightField> highLightBuilderFields = hit.getHighlightFields();
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            for(String key : array){
                HighlightField field = highLightBuilderFields.get(key);
                if(field != null){
                    Text[] fragments = field.fragments();
                    String str = Arrays.toString(fragments);
                    str = str.substring(1, str.length()-1);
                    sourceMap.put(key, str);
                }
            }
            arrayList.add(sourceMap);
        }
        return arrayList;
    }

    public Video getVideos(String keyword){
       return videoRepository.findByTitleLike(keyword);
    }

    public void deleteAllVideos(){
        videoRepository.deleteAll();
    }

    public long countVideoBySearchTxt(String searchTxt) {
        return this.videoRepository.countByTitleOrDescription(searchTxt, searchTxt);
    }

    public long countUserBySearchTxt(String searchTxt) {
        return this.userInfoRepository.countByNick(searchTxt);
    }

    public void updateVideoViewCount(Long videoId) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if(videoOpt.isPresent()){
            Video video = videoOpt.get();
            int viewCount = video.getViewCount() == null? 0 : video.getViewCount();
            video.setViewCount(viewCount+1);
            videoRepository.save(video);
        }
    }

    public Page<Video> pageListSearchVideos(String keyword, Integer pageSize,
                                            Integer pageNo, String searchType) throws IOException {
        SearchRequest searchRequest = new SearchRequest("videos");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
        // 多字段匹配 title + description
        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "description");
        sourceBuilder.query(matchQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        // 排序
        if(SearchConstant.CREATE_TIME.equals(searchType)){
            sourceBuilder.sort("createTime", SortOrder.DESC);
        } else if(SearchConstant.DANMU_COUNT.equals(searchType)){
            sourceBuilder.sort("danmuCount", SortOrder.DESC);
        } else {
            // 默认 / 最多播放
            sourceBuilder.sort("viewCount", SortOrder.DESC);
        }
        // 高亮
        String[] highlightFields = {"title", "description"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for(String key : highlightFields){
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(sourceBuilder);
        // 执行搜索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Video> videoList = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            Map<String, HighlightField> highlightFieldMap = hit.getHighlightFields();
            // 用高亮内容替换原始字段
            for(String key : highlightFields){
                HighlightField field = highlightFieldMap.get(key);
                if(field != null){
                    Text[] fragments = field.fragments();
                    String str = Arrays.toString(fragments);
                    str = str.substring(1, str.length()-1);
                    sourceMap.put(key, str);
                }
            }
            Video video = objectToBean(sourceMap, Video.class);
            videoList.add(video);
        }
        long total = searchResponse.getHits().getTotalHits().value;
        return new PageImpl<>(videoList, PageRequest.of(pageNo, pageSize), total);
    }

    public Page<UserInfo> pageListSearchUsers(String keyword, Integer pageSize,
                                              Integer pageNo, String searchType) throws IOException {
        SearchRequest searchRequest = new SearchRequest("user-infos");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
        // 匹配 nick 字段
        sourceBuilder.query(QueryBuilders.matchQuery("nick", keyword));
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        // 排序
        if(SearchConstant.USER_FAN_COUNT_ASC.equals(searchType)){
            sourceBuilder.sort("fanCount", SortOrder.ASC);
        } else {
            // 默认 / 粉丝数由高到低
            sourceBuilder.sort("fanCount", SortOrder.DESC);
        }
        // 高亮
        String[] highlightFields = {"nick"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for(String key : highlightFields){
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(sourceBuilder);
        // 执行搜索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<UserInfo> userList = new ArrayList<>();
        for(SearchHit hit : searchResponse.getHits()){
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            Map<String, HighlightField> highlightFieldMap = hit.getHighlightFields();
            for(String key : highlightFields){
                HighlightField field = highlightFieldMap.get(key);
                if(field != null){
                    Text[] fragments = field.fragments();
                    String str = Arrays.toString(fragments);
                    str = str.substring(1, str.length()-1);
                    sourceMap.put(key, str);
                }
            }
            UserInfo userInfo = objectToBean(sourceMap, UserInfo.class);
            userList.add(userInfo);
        }
        long total = searchResponse.getHits().getTotalHits().value;
        return new PageImpl<>(userList, PageRequest.of(pageNo, pageSize), total);
    }

    /**
     * 将 ES 返回的 Map 转换为目标类型对象
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private <T> T objectToBean(Map<String, Object> sourceMap, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(sourceMap, clazz);
    }
}
