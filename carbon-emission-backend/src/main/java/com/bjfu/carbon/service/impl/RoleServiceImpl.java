package com.bjfu.carbon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.mapper.PermissionMapper;
import com.bjfu.carbon.mapper.RoleMapper;
import com.bjfu.carbon.mapper.RolePermissionMapper;
import com.bjfu.carbon.mapper.UserRoleMapper;
import com.bjfu.carbon.service.RoleService;
import com.bjfu.carbon.vo.PermissionVo;
import com.bjfu.carbon.vo.RoleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 * 单一职责：只负责角色管理业务逻辑
 *
 * @author xgy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

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
     * 校验角色操作权限：操作者的角色等级必须 <= 被操作角色的等级
     * 即：操作者的order值必须 >= 被操作角色的order值（order越小等级越高）
     * @param targetRoleId 被操作角色ID
     * @throws RuntimeException 如果操作者权限等级不足
     */
    private void validateRoleOperationPermission(Long targetRoleId) {
        if (targetRoleId == null) {
            throw new RuntimeException("角色ID不能为空");
        }
        
        // 获取被操作角色的权限等级
        Role targetRole = this.getById(targetRoleId);
        if (targetRole == null) {
            throw new RuntimeException("角色不存在");
        }
        
        Integer targetRoleOrder = targetRole.getOrder();
        if (targetRoleOrder == null) {
            targetRoleOrder = 999; // 如果没有设置order，默认为最低等级
        }
        
        // 获取当前操作者的最低权限等级
        Integer currentUserMinOrder = getCurrentUserMinOrder();
        
        // 如果操作者的order > 被操作角色的order，说明操作者权限等级更低，不允许操作
        if (currentUserMinOrder > targetRoleOrder) {
            throw new RuntimeException("您没有权限操作角色：" + targetRole.getRoleName() + "（权限等级不足）");
        }
    }

    @Override
    public IPage<Role> getRoleList(Page<Role> page, String roleCode, String roleName) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (roleCode != null && !roleCode.isEmpty()) {
            wrapper.like(Role::getRoleCode, roleCode);
        }
        if (roleName != null && !roleName.isEmpty()) {
            wrapper.like(Role::getRoleName, roleName);
        }
        wrapper.orderByDesc(Role::getCreatedTime);
        return this.page(page, wrapper);
    }

    @Override
    public RoleVo getRoleWithPermissions(Long roleId) {
        Role role = this.getById(roleId);
        if (role == null) {
            return null;
        }

        RoleVo roleVo = new RoleVo();
        BeanUtils.copyProperties(role, roleVo);

        // 获取角色权限ID列表
        List<Long> permissionIds = permissionMapper.getRolePermissionIds(roleId);
        
        // 根据权限ID获取完整的权限信息
        List<PermissionVo> permissions = new ArrayList<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            // 根据权限ID列表批量查询权限
            List<com.bjfu.carbon.domain.Permission> rolePermissions = permissionMapper.selectBatchIds(permissionIds);
            
            // 转换为PermissionVo列表
            permissions = rolePermissions.stream()
                    .map(p -> {
                        PermissionVo vo = new PermissionVo();
                        BeanUtils.copyProperties(p, vo);
                        return vo;
                    })
                    .collect(Collectors.toList());
        }
        
        roleVo.setPermissions(permissions);

        return roleVo;
    }

    /**
     * 更新角色（带权限校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRoleWithValidation(Role role) {
        // 校验角色操作权限
        validateRoleOperationPermission(role.getId());
        
        // 如果修改了order字段，需要校验新order值
        Role existingRole = this.getById(role.getId());
        if (existingRole != null && role.getOrder() != null && !role.getOrder().equals(existingRole.getOrder())) {
            // 如果新order值 < 当前用户的order值，说明新等级更高，不允许修改
            Integer currentUserMinOrder = getCurrentUserMinOrder();
            if (role.getOrder() < currentUserMinOrder) {
                throw new RuntimeException("您没有权限将角色等级设置为：" + role.getOrder() + "（权限等级不足）");
            }
        }
        
        return this.updateById(role);
    }

    /**
     * 删除角色（带权限校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleWithValidation(Long roleId) {
        // 校验角色操作权限
        validateRoleOperationPermission(roleId);
        
        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 删除用户角色关联
        userRoleMapper.deleteByRoleId(roleId);
        
        return this.removeById(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignPermissions(Long roleId, List<Long> permissionIds) {
        // 校验角色操作权限
        validateRoleOperationPermission(roleId);
        
        // 去重，避免重复插入
        List<Long> newPermissionIds = (permissionIds != null && !permissionIds.isEmpty()) ?
                permissionIds.stream().distinct().collect(Collectors.toList()) : new ArrayList<>();

        // 获取角色现有的权限ID列表
        List<Long> existingPermissionIds = rolePermissionMapper.getPermissionIdsByRoleId(roleId);

        // 计算需要删除的权限（存在于数据库但不在新列表中的）
        List<Long> toDelete = existingPermissionIds.stream()
                .filter(id -> !newPermissionIds.contains(id))
                .collect(Collectors.toList());

        // 计算需要新增的权限（在新列表中但不存在于数据库中的）
        List<Long> toInsert = newPermissionIds.stream()
                .filter(id -> !existingPermissionIds.contains(id))
                .collect(Collectors.toList());

        // 只删除需要删除的权限
        if (!toDelete.isEmpty()) {
            rolePermissionMapper.deleteByRoleIdAndPermissionIds(roleId, toDelete);
        }

        // 只插入需要新增的权限
        if (!toInsert.isEmpty()) {
            rolePermissionMapper.insertRolePermissions(roleId, toInsert);
        }

        return true;
    }

    @Override
    public List<Role> getAllEnabledRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1);
        wrapper.orderByAsc(Role::getRoleCode);
        return this.list(wrapper);
    }
}

