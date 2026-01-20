package com.bjfu.carbon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bjfu.carbon.domain.Permission;
import com.bjfu.carbon.vo.PermissionVo;

import java.util.List;

/**
 * 权限服务接口
 * 接口隔离：只提供权限管理相关方法
 *
 * @author xgy
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 获取权限树
     *
     * @return 权限树列表
     */
    List<PermissionVo> getPermissionTree();

    /**
     * 构建权限树
     *
     * @param permissions 权限列表
     * @param parentId    父权限ID
     * @return 权限树列表
     */
    List<PermissionVo> buildPermissionTree(List<Permission> permissions, Long parentId);

    /**
     * 更新权限（带权限校验）
     *
     * @param permission 权限对象
     * @return 是否成功
     */
    boolean updatePermissionWithValidation(Permission permission);

    /**
     * 删除权限（带权限校验）
     *
     * @param permissionId 权限ID
     * @return 是否成功
     */
    boolean deletePermissionWithValidation(Long permissionId);
}

