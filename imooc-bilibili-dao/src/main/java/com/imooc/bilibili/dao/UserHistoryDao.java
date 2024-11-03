package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.UserVideoHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserHistoryDao {
    int pageCountUserVideoHistory(Map<String, Object> params);

    List<UserVideoHistory> pageListUserVideoHistory(Map<String, Object> params);
}
