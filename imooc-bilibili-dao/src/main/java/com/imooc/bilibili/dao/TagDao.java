package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagDao {
    void addTag(Tag tag);

}
