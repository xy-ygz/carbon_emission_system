package com.bjfu.carbon.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.domain.User;
import com.bjfu.carbon.mapper.RoleMapper;
import com.bjfu.carbon.mapper.UserRoleMapper;
import com.bjfu.carbon.service.UserManageService;
import com.bjfu.carbon.service.UserService;
import com.bjfu.carbon.utils.SaltUtils;
import com.bjfu.carbon.vo.RoleVo;
import com.bjfu.carbon.vo.UserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现类
 * 单一职责：只负责用户管理业务逻辑
 *
 * @author xgy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManageServiceImpl implements UserManageService {

    private final UserService userService;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public IPage<UserVo> getUserList(Page<UserVo> page, String name, String username, String department) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(User::getName, name);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (department != null && !department.isEmpty()) {
            wrapper.like(User::getDepartment, department);
        }
        wrapper.orderByDesc(User::getCreatedTime);

        IPage<User> userPage = userService.page(new Page<>(page.getCurrent(), page.getSize()), wrapper);

        // 转换为UserVo
        IPage<UserVo> voPage = new Page<>(page.getCurrent(), page.getSize(), userPage.getTotal());
        List<UserVo> voList = userPage.getRecords().stream()
                .map(this::convertToUserVo)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public UserVo getUserWithRoles(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }

        UserVo userVo = convertToUserVo(user);
        return userVo;
    }

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
     * 获取指定用户的最低权限等级（order值，越小等级越高）
     * @param userId 用户ID
     * @return 用户的最低权限等级，如果用户不存在或没有角色，返回999（最低等级）
     */
    private Integer getUserMinOrder(Long userId) {
        if (userId == null) {
            return 999;
        }
        
        // 获取用户的所有角色
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

    /**
     * 校验操作权限：操作者的角色等级必须 <= 被操作用户的角色等级
     * 即：操作者的order值必须 >= 被操作用户的order值（order越小等级越高）
     * @param targetUserId 被操作用户ID
     * @throws RuntimeException 如果操作者权限等级不足
     */
    private void validateUserOperationPermission(Long targetUserId) {
        if (targetUserId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        // 获取被操作用户的最低权限等级
        Integer targetUserMinOrder = getUserMinOrder(targetUserId);
        
        // 获取当前操作者的最低权限等级
        Integer currentUserMinOrder = getCurrentUserMinOrder();
        
        // 如果操作者的order > 被操作用户的order，说明操作者权限等级更低，不允许操作
        if (currentUserMinOrder > targetUserMinOrder) {
            User targetUser = userService.getById(targetUserId);
            String targetUserName = targetUser != null ? targetUser.getName() : "未知用户";
            throw new RuntimeException("您没有权限操作用户：" + targetUserName + "（权限等级不足）");
        }
    }

    /**
     * 校验角色权限等级：当前用户只能分配权限等级>=当前用户权限等级的角色
     * @param roleIds 要分配的角色ID列表
     * @throws RuntimeException 如果尝试分配权限等级更高的角色
     */
    private void validateRoleOrder(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return; // 没有分配角色，不需要校验
        }
        
        Integer currentUserMinOrder = getCurrentUserMinOrder();
        
        // 查询要分配的所有角色
        List<Role> targetRoles = roleMapper.selectBatchIds(roleIds);
        for (Role role : targetRoles) {
            if (role.getOrder() == null) {
                role.setOrder(999); // 如果没有设置order，默认为最低等级
            }
            // 如果目标角色的order < 当前用户的order，说明目标角色权限更高，不允许分配
            if (role.getOrder() < currentUserMinOrder) {
                throw new RuntimeException("您没有权限分配角色：" + role.getRoleName() + "（权限等级不足）");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUser(User user, List<Long> roleIds) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User existingUser = userService.getOne(wrapper);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在，请使用其他用户名");
        }

        // 校验角色权限等级
        validateRoleOrder(roleIds);

        // 设置密码和盐值
        String salt = SaltUtils.getSalt(4);
        user.setSalt(salt);
        user.setPassword(SecureUtil.md5(salt + user.getPassword()));
        user.setCreatedTime(new Date());
        user.setModifiedTime(new Date());

        // 保存用户
        boolean saved = userService.save(user);

        // 分配角色
        if (saved) {
            if (roleIds != null && !roleIds.isEmpty()) {
                // 如果指定了角色，使用指定的角色
                userRoleMapper.insertUserRoles(user.getId(), roleIds);
            } else {
                // 如果没有指定角色，默认分配普通用户角色（USER）
                Role defaultRole = roleMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleCode, "USER")
                        .eq(Role::getStatus, 1)
                );
                if (defaultRole != null) {
                    // 校验默认角色权限等级
                    validateRoleOrder(java.util.Collections.singletonList(defaultRole.getId()));
                    userRoleMapper.insertUserRoles(user.getId(), java.util.Collections.singletonList(defaultRole.getId()));
                    log.info("用户 {} 未指定角色，已默认分配普通用户角色", user.getUsername());
                } else {
                    log.warn("未找到普通用户角色（USER），用户 {} 未分配任何角色", user.getUsername());
                }
            }
        }

        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user, List<Long> roleIds) {
        if (user.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        // 校验操作权限：操作者的角色等级必须 <= 被操作用户的角色等级
        validateUserOperationPermission(user.getId());
        
        // 校验角色权限等级：只能分配权限等级>=当前用户权限等级的角色
        if (roleIds != null && !roleIds.isEmpty()) {
            validateRoleOrder(roleIds);
        }
        
        user.setModifiedTime(new Date());

        // 更新用户
        boolean updated = userService.updateById(user);

        // 更新角色关联
        if (updated && roleIds != null) {
            userRoleMapper.deleteByUserId(user.getId());
            if (!roleIds.isEmpty()) {
                userRoleMapper.insertUserRoles(user.getId(), roleIds);
            }
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId) {
        // 校验操作权限：操作者的角色等级必须 <= 被操作用户的角色等级
        validateUserOperationPermission(userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return false;
        }

        String salt = SaltUtils.getSalt(4);
        user.setSalt(salt);
        user.setPassword(SecureUtil.md5(salt + "123456")); // 默认密码123456
        user.setModifiedTime(new Date());

        return userService.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<Long> roleIds) {
        // 校验操作权限：操作者的角色等级必须 <= 被操作用户的角色等级
        validateUserOperationPermission(userId);
        
        // 校验角色权限等级：只能分配权限等级>=当前用户权限等级的角色
        if (roleIds != null && !roleIds.isEmpty()) {
            validateRoleOrder(roleIds);
        }
        
        // 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);

        // 批量插入新角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.insertUserRoles(userId, roleIds);
        }

        return true;
    }

    /**
     * 转换为UserVo
     *
     * @param user 用户对象
     * @return 用户视图对象
     */
    private UserVo convertToUserVo(User user) {
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);

        // 获取用户角色
        List<Long> roleIds = userRoleMapper.getRoleIdsByUserId(user.getId());
        if (roleIds != null && !roleIds.isEmpty()) {
            List<RoleVo> roles = roleIds.stream()
                    .map(roleId -> {
                        Role role = roleMapper.selectById(roleId);
                        if (role != null) {
                            RoleVo roleVo = new RoleVo();
                            BeanUtils.copyProperties(role, roleVo);
                            return roleVo;
                        }
                        return null;
                    })
                    .filter(roleVo -> roleVo != null)
                    .collect(Collectors.toList());
            userVo.setRoles(roles);
        }

        return userVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        // 校验操作权限：操作者的角色等级必须 <= 被操作用户的角色等级
        validateUserOperationPermission(userId);
        
        // MyBatis-Plus的removeById会自动处理外键关联（CASCADE）
        // 但需要先删除用户角色关联
        userRoleMapper.deleteByUserId(userId);
        return userService.removeById(userId);
    }
}

