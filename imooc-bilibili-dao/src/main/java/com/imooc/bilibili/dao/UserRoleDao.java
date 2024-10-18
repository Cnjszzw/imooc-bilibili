package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.auth.AuthRoleElementOperation;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import com.imooc.bilibili.domain.auth.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserRoleDao {
    Set<UserRole> getUserRoleListByUserId(Long userId);

    List<AuthRoleElementOperation> getRoleElementOperationList(@Param("userRoleUserRoleIdList") Set<Long> userRoleUserRoleIdList);

    List<AuthRoleMenu> getRoleMenuList(@Param("userRoleUserRoleIdList")Set<Long> userRoleUserRoleIdList);
}
