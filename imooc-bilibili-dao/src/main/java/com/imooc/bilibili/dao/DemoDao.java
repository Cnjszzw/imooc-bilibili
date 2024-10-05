package com.imooc.bilibili.dao;


import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DemoDao {

    Long query(Long id);

}
