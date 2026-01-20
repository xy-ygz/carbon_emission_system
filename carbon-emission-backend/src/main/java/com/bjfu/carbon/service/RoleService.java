package com.bjfu.carbon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.vo.RoleVo;

import java.util.List;

/**
 * 角色服务接口
 * 接口隔离：只提供角色管理相关方法
 *
 * @author xgy
 */
public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色列表
     *
     * @param page     分页参数
     * @param roleCode 角色编码（可选）
     * @param roleName 角色名称（可选）
     * @return 角色分页列表
     */
    IPage<Role> getRoleList(Page<Role> page, String roleCode, String roleName);

    /**
     * 获取角色详情（包含权限）
     *
     * @param roleId 角色ID
     * @return 角色视图对象
     */
    RoleVo getRoleWithPermissions(Long roleId);

    /**
     * 分配权限给角色
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     * @return 是否成功
     */
    boolean assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 获取所有启用的角色
     *
     * @return 角色列表
     */
    List<Role> getAllEnabledRoles();

    /**
     * 更新角色（带权限校验）
     *
     * @param role 角色对象
     * @return 是否成功
     */
    boolean updateRoleWithValidation(Role role);

    /**
     * 删除角色（带权限校验）
     *
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean deleteRoleWithValidation(Long roleId);
}

