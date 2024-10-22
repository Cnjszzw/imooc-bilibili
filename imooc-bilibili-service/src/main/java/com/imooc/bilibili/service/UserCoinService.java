package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.UserCoinDao;
import com.imooc.bilibili.domain.VideoCoin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserCoinService {


    @Autowired
    private UserCoinDao userCoinDao;

    public Integer getUserCoinAmount(Long userId) {
        return userCoinDao.getUserCoinAmount(userId);
    }

    public VideoCoin queryVideoCoinStatus(Long videoId, Long userId) {
        return userCoinDao.queryVideoCoinStatus(videoId, userId);
    }

    public void addVideoCoin(VideoCoin videoCoins) {
        userCoinDao.addVideoCoin(videoCoins);
    }

    public void updateVideoCoin(VideoCoin videoCoins) {
        userCoinDao.updateVideoCoin(videoCoins);
    }

    public void updateUserCoinAmount(Long userId, Integer amount , Date date) {
        userCoinDao.updateUserCoinAmount(userId, amount , date);
    }
}
