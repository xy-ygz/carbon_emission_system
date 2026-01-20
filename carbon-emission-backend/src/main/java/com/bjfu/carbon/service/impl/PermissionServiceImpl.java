package com.bjfu.carbon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjfu.carbon.domain.Permission;
import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.mapper.PermissionMapper;
import com.bjfu.carbon.mapper.RoleMapper;
import com.bjfu.carbon.mapper.RolePermissionMapper;
import com.bjfu.carbon.mapper.UserRoleMapper;
import com.bjfu.carbon.service.PermissionService;
import com.bjfu.carbon.vo.PermissionVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限服务实现类
 * 单一职责：只负责权限管理业务逻辑
 *
 * @author xgy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 获取当前登录用户的最低权限等级（order值，越小等级越高）
     * @return 当前用户的最低权限等级，如果未登录或没有角色，返回999（最低等级）
     */
    private Integer getCurrentUserMinOrder() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                Long userId = ((com.bjfu.carbon.security.UserDetailsImpl) userDetails).getUserId();
                
                // 获取当前用户的所有角色
                List<Long> roleIds = userRoleMapper.getRoleIdsByUserId(userId);
                if (roleIds == null || roleIds.isEmpty()) {
                    return 999; // 没有角色，返回最低等级
                }
                
                // 查询所有角色的order值，取最小值（最高权限等级）
                List<Role> roles = roleMapper.selectBatchIds(roleIds);
                return roles.stream()
                        .map(Role::getOrder)
                        .filter(order -> order != null)
                        .min(Integer::compareTo)
                        .orElse(999);
            }
        } catch (Exception e) {
            log.warn("获取当前用户权限等级失败", e);
        }
        return 999; // 默认返回最低等级
    }

    /**
     * 校验权限操作权限：只有超级管理员（order=0）可以修改/删除权限
     * @throws RuntimeException 如果操作者不是超级管理员
     */
    private void validatePermissionOperationPermission() {
        Integer currentUserMinOrder = getCurrentUserMinOrder();
        
        // 只有超级管理员（order=0）可以修改/删除权限
        if (currentUserMinOrder > 0) {
            throw new RuntimeException("只有超级管理员可以修改或删除权限（权限等级不足）");
        }
    }

    @Override
    public List<PermissionVo> getPermissionTree() {
        List<Permission> allPermissions = permissionMapper.getPermissionTree();
        return buildPermissionTree(allPermissions, 0L);
    }

    @Override
    public List<PermissionVo> buildPermissionTree(List<Permission> permissions, Long parentId) {
        List<PermissionVo> tree = new ArrayList<>();

        for (Permission permission : permissions) {
            if (parentId.equals(permission.getParentId())) {
                PermissionVo vo = new PermissionVo();
                BeanUtils.copyProperties(permission, vo);

                // 递归构建子权限
                List<PermissionVo> children = buildPermissionTree(permissions, permission.getId());
                vo.setChildren(children);

                tree.add(vo);
            }
        }

        return tree;
    }

    /**
     * 更新权限（带权限校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePermissionWithValidation(Permission permission) {
        // 校验权限操作权限：只有超级管理员可以修改权限
        validatePermissionOperationPermission();
        
        return this.updateById(permission);
    }

    /**
     * 删除权限（带权限校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePermissionWithValidation(Long permissionId) {
        // 校验权限操作权限：只有超级管理员可以删除权限
        validatePermissionOperationPermission();
        
        // 删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(permissionId);
        
        return this.removeById(permissionId);
    }
}

