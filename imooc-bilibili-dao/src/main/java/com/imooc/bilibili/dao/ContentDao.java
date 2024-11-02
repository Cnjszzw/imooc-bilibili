package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.Content;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentDao {
    Long addContent(Content content);
}
