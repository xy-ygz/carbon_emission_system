package com.bjfu.carbon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjfu.carbon.domain.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 权限Mapper接口
 *
 * @author xgy
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 获取用户的所有权限编码
     *
     * @param userId 用户ID
     * @return 权限编码集合
     */
    Set<String> getUserPermissions(@Param("userId") Long userId);

    /**
     * 获取角色的所有权限编码
     *
     * @param roleId 角色ID
     * @return 权限编码集合
     */
    Set<String> getRolePermissions(@Param("roleId") Long roleId);

    /**
     * 获取角色的所有权限ID
     *
     * @param roleId 角色ID
     * @return 权限ID集合
     */
    List<Long> getRolePermissionIds(@Param("roleId") Long roleId);

    /**
     * 获取权限树（所有权限）
     *
     * @return 权限列表
     */
    List<Permission> getPermissionTree();

    /**
     * 根据父ID获取子权限列表
     *
     * @param parentId 父权限ID
     * @return 子权限列表
     */
    List<Permission> getPermissionsByParentId(@Param("parentId") Long parentId);
}

