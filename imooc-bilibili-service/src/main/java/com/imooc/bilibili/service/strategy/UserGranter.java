package com.imooc.bilibili.service.strategy;


import com.imooc.bilibili.domain.User;

/**
 * 抽象策略类
 */
public interface UserGranter {


	/**
	 * 获取数据
	 * @param user 传入的参数
	 * @return token值
	 */
	String login(User user) throws Exception;

}
