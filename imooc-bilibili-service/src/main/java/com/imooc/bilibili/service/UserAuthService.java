package com.imooc.bilibili.service;


import com.imooc.bilibili.dao.UserAuthDao;
import com.imooc.bilibili.domain.auth.*;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAuthService {

    @Autowired
    UserRoleService userRoleService;

    public UserAuthorities getUserAuthorities(Long userId) {
        Set<UserRole> userRoleList = userRoleService.getUserRoleList(userId);
        Set<Long> userRoleUserRoleIdList = userRoleList.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        List<AuthRoleElementOperation> roleElementOperationList = userRoleService.getRoleElementOperationList(userRoleUserRoleIdList);
        List<AuthRoleMenu> roleMenuList = userRoleService.getRoleMenuList(userRoleUserRoleIdList);
        UserAuthorities userAuthorities = new UserAuthorities();
        userAuthorities.setRoleElementOperationList(roleElementOperationList);
        userAuthorities.setRoleMenuList(roleMenuList);
        return userAuthorities;
    }

    public void addUserDefaultRole(Long id) {
        UserRole userRole = new UserRole();
        AuthRole authRole = userRoleService.getRoleByCode(AuthRoleConstant.ROLE_LV0);
        userRole.setUserId(id);
        userRole.setRoleId(authRole.getId());
        userRole.setCreateTime(new Date());
        userRoleService.addUserRole(userRole);
    }
}
