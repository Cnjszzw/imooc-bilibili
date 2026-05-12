package com.imooc.bilibili.dao.repository;

import com.imooc.bilibili.domain.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoRepository extends ElasticsearchRepository<Video, Long> {

    Video findByTitleLike(String keyword);

    long countByTitleOrDescription(String titleKeyword, String DescriptionKeyword);

    Page<Video> findByTitleOrDescriptionOrderByViewCountDesc(String title, String description,
                                                             PageRequest pageRequest);

    Page<Video> findByTitleOrDescriptionOrderByCreateTimeDesc(String title, String description,
                                                              PageRequest pageRequest);

    Page<Video> findByTitleOrDescriptionOrderByDanmuCountDesc(String title, String description,
                                                              PageRequest pageRequest);
}
