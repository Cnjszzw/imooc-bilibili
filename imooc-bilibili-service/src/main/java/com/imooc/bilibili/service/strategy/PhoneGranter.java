package com.imooc.bilibili.service.strategy;

import com.imooc.bilibili.dao.UserDao;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.MD5Util;
import com.imooc.bilibili.service.util.RSAUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *策略：手机号登录
 */
@Component
public class PhoneGranter implements UserGranter{

	@Autowired
	private UserDao userDao;

	@Override
	public String login(User user) throws Exception {
		//1.对用户校验：校验手机号码，校验是否存在用户
		String phone = user.getAccount();
		if(StringUtils.isNullOrEmpty(phone)){
			throw new ConditionException("参数异常，手机号错误");
		}
		User dbUser = userDao.getUserByPhoneOrEmail(phone,null);
		if (dbUser == null) {
			throw new ConditionException("该手机号未注册");
		}
		String password = user.getPassword();
		try {
			String rawPassword = RSAUtil.decrypt(password);
			String md5PassWord = MD5Util.sign(rawPassword, dbUser.getSalt(), "UTF-8");
			if(!md5PassWord.equals(dbUser.getPassword())){
				throw new ConditionException("密码错误");
			}
		} catch (Exception e) {
			throw new ConditionException("解密错误");
		}
		return TokenUtil.generateToken(dbUser.getId());

	}

}
