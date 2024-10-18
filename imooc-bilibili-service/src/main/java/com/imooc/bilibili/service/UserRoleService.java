package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.UserRoleDao;
import com.imooc.bilibili.domain.auth.AuthRole;
import com.imooc.bilibili.domain.auth.AuthRoleElementOperation;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import com.imooc.bilibili.domain.auth.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserRoleService {

    @Autowired
    UserRoleDao userRoleDao;


    public Set<UserRole> getUserRoleList(Long userId) {
        return userRoleDao.getUserRoleListByUserId(userId);
    }

    public List<AuthRoleElementOperation> getRoleElementOperationList(Set<Long> userRoleUserRoleIdList) {
        return userRoleDao.getRoleElementOperationList(userRoleUserRoleIdList);
    }

    public List<AuthRoleMenu> getRoleMenuList(Set<Long> userRoleUserRoleIdList) {
        return userRoleDao.getRoleMenuList(userRoleUserRoleIdList);
    }

    public AuthRole getRoleByCode(String code) {
        return userRoleDao.getRoleByCode(code);
    }

    public void addUserRole(UserRole userRole){

        userRoleDao.addUserRole(userRole);
    }
}
