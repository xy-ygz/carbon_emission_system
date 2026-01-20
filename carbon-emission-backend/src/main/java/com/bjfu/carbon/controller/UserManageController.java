package com.bjfu.carbon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.domain.User;
import com.bjfu.carbon.dto.UserCreateDto;
import com.bjfu.carbon.mapper.RoleMapper;
import com.bjfu.carbon.mapper.UserRoleMapper;
import com.bjfu.carbon.security.UserDetailsImpl;
import com.bjfu.carbon.service.RoleService;
import com.bjfu.carbon.service.UserManageService;
import com.bjfu.carbon.vo.RoleVo;
import com.bjfu.carbon.vo.UserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * 单一职责：只负责用户管理相关的HTTP请求处理
 *
 * @author xgy
 */
@Slf4j
@RestController
@RequestMapping("/userManage")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;
    private final RoleService roleService;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    /**
     * 分页查询用户列表
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getUserList")
    @PreAuthorize("hasAuthority('USER_MANAGE_QUERY')")
    public Object getUserList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String department) {
        Page<UserVo> page = new Page<>(current, size);
        IPage<UserVo> result = userManageService.getUserList(page, name, username, department);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户详情（包含角色）
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getUserWithRoles")
    @PreAuthorize("hasAuthority('USER_MANAGE_QUERY')")
    public Object getUserWithRoles(@RequestParam Long userId) {
        UserVo userVo = userManageService.getUserWithRoles(userId);
        if (userVo == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "用户不存在");
        }
        return ResultUtils.success(userVo);
    }

    /**
     * 新增用户
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addUser")
    @PreAuthorize("hasAuthority('USER_MANAGE_ADD')")
    public Object addUser(@Valid @RequestBody UserCreateDto dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setDepartment(dto.getDepartment());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        boolean success = userManageService.addUser(user, dto.getRoleIds());
        return success ? ResultUtils.success("新增成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "新增失败");
    }

    /**
     * 更新用户
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updateUser")
    @PreAuthorize("hasAuthority('USER_MANAGE_UPDATE')")
    public Object updateUser(@Valid @RequestBody UserVo userVo) {
        if (userVo.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "用户ID不能为空");
        }

        User user = new User();
        user.setId(userVo.getId());
        user.setName(userVo.getName());
        user.setUsername(userVo.getUsername());
        user.setDepartment(userVo.getDepartment());
        user.setPhone(userVo.getPhone());
        user.setStatus(userVo.getStatus());

        boolean success = userManageService.updateUser(user, userVo.getRoles() != null ?
                userVo.getRoles().stream().map(RoleVo::getId).collect(Collectors.toList()) : null);

        return success ? ResultUtils.success("更新成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "更新失败");
    }

    /**
     * 删除用户
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/deleteUser")
    @PreAuthorize("hasAuthority('USER_MANAGE_DELETE')")
    public Object deleteUser(@RequestBody UserVo userVo) {
        if (userVo.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "用户ID不能为空");
        }

        // 检查用户是否存在
        if (userManageService.getUserWithRoles(userVo.getId()) == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "用户不存在");
        }

        // 删除用户（MyBatis-Plus会自动删除关联的角色关系，因为设置了CASCADE）
        boolean success = userManageService.deleteUser(userVo.getId());
        return success ? ResultUtils.success("删除成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "删除失败");
    }

    /**
     * 重置密码
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/resetPassword")
    @PreAuthorize("hasAuthority('USER_MANAGE_RESET_PASSWORD')")
    public Object resetPassword(@RequestBody UserVo userVo) {
        if (userVo.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "用户ID不能为空");
        }

        boolean success = userManageService.resetPassword(userVo.getId());
        return success ? ResultUtils.success("密码已重置为123456") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "重置失败");
    }

    /**
     * 分配角色
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/assignRoles")
    @PreAuthorize("hasAuthority('USER_MANAGE_ASSIGN_ROLE')")
    public Object assignRoles(@RequestBody UserVo userVo) {
        if (userVo.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "用户ID不能为空");
        }

        List<Long> roleIds = userVo.getRoles() != null ?
                userVo.getRoles().stream().map(RoleVo::getId).collect(Collectors.toList()) : null;

        boolean success = userManageService.assignRoles(userVo.getId(), roleIds);
        return success ? ResultUtils.success("分配成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "分配失败");
    }

    /**
     * 获取所有角色列表（用于下拉选择）
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getAllRoles")
    @PreAuthorize("hasAuthority('USER_MANAGE_QUERY')")
    public Object getAllRoles() {
        return ResultUtils.success(roleService.getAllEnabledRoles());
    }

    /**
     * 获取当前登录用户的角色信息（用于前端权限等级判断）
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getCurrentUserRoles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_QUERY')")
    public Object getCurrentUserRoles() {
        try {
            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                Long userId = userDetails.getUserId();
                
                // 获取当前用户的所有角色
                List<Long> roleIds = userRoleMapper.getRoleIdsByUserId(userId);
                if (roleIds != null && !roleIds.isEmpty()) {
                    List<Role> roles = roleMapper.selectBatchIds(roleIds);
                    // 转换为RoleVo并返回
                    List<RoleVo> roleVos = new ArrayList<>();
                    for (Role role : roles) {
                        RoleVo roleVo = new RoleVo();
                        BeanUtils.copyProperties(role, roleVo);
                        roleVos.add(roleVo);
                    }
                    return ResultUtils.success(roleVos);
                }
            }
            return ResultUtils.success(new ArrayList<>());
        } catch (Exception e) {
            log.error("获取当前用户角色失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取当前用户角色失败");
        }
    }
}

