package com.bjfu.carbon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjfu.carbon.domain.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper接口
 *
 * @author xgy
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 获取角色的权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关联
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     * @return 插入数量
     */
    int insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 删除角色的所有权限关联
     *
     * @param roleId 角色ID
     * @return 删除数量
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色的指定权限关联
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     * @return 删除数量
     */
    int deleteByRoleIdAndPermissionIds(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 删除权限的所有角色关联
     *
     * @param permissionId 权限ID
     * @return 删除数量
     */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);
}

