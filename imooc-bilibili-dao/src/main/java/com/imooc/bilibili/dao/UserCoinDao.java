package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.VideoCoin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface UserCoinDao {
    Integer getUserCoinAmount(Long userId);

    VideoCoin queryVideoCoinStatus(@Param("videoId") Long videoId,@Param("userId") Long userId);

    Integer addVideoCoin(VideoCoin videoCoins);

    Integer updateVideoCoin(VideoCoin videoCoins);

    Integer updateUserCoinAmount(@Param("userId")Long userId, @Param("amount")Integer amount , @Param("updateTime") Date date);
}
