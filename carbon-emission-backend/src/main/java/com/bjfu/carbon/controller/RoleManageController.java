package com.bjfu.carbon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.service.PermissionService;
import com.bjfu.carbon.service.RoleService;
import com.bjfu.carbon.vo.PermissionVo;
import com.bjfu.carbon.vo.RoleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理控制器
 * 单一职责：只负责角色管理相关的HTTP请求处理
 *
 * @author xgy
 */
@Slf4j
@RestController
@RequestMapping("/roleManage")
@RequiredArgsConstructor
public class RoleManageController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    /**
     * 分页查询角色列表
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getRoleList")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_QUERY')")
    public Object getRoleList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String roleName) {
        Page<Role> page = new Page<>(current, size);
        IPage<Role> result = roleService.getRoleList(page, roleCode, roleName);
        return ResultUtils.success(result);
    }

    /**
     * 获取角色详情（包含权限）
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getRoleWithPermissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_QUERY')")
    public Object getRoleWithPermissions(@RequestParam Long roleId) {
        RoleVo roleVo = roleService.getRoleWithPermissions(roleId);
        if (roleVo == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "角色不存在");
        }
        return ResultUtils.success(roleVo);
    }

    /**
     * 新增角色
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addRole")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_ADD')")
    public Object addRole(@Valid @RequestBody Role role) {
        boolean success = roleService.save(role);
        return success ? ResultUtils.success("新增成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "新增失败");
    }

    /**
     * 更新角色
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updateRole")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_UPDATE')")
    public Object updateRole(@Valid @RequestBody Role role) {
        if (role.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "角色ID不能为空");
        }
        boolean success = roleService.updateRoleWithValidation(role);
        return success ? ResultUtils.success("更新成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "更新失败");
    }

    /**
     * 删除角色
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/deleteRole")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_DELETE')")
    public Object deleteRole(@RequestBody Role role) {
        if (role.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "角色ID不能为空");
        }
        boolean success = roleService.deleteRoleWithValidation(role.getId());
        return success ? ResultUtils.success("删除成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "删除失败");
    }

    /**
     * 分配权限
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/assignPermissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_ASSIGN_PERMISSION')")
    public Object assignPermissions(@RequestBody RoleVo roleVo) {
        if (roleVo.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "角色ID不能为空");
        }

        List<Long> permissionIds = roleVo.getPermissions() != null ?
                roleVo.getPermissions().stream().map(PermissionVo::getId).collect(Collectors.toList()) : null;

        boolean success = roleService.assignPermissions(roleVo.getId(), permissionIds);
        return success ? ResultUtils.success("分配成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "分配失败");
    }

    /**
     * 获取权限树（用于角色分配权限）
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getPermissionTree")
    @PreAuthorize("hasAuthority('ROLE_MANAGE_QUERY')")
    public Object getPermissionTree() {
        return ResultUtils.success(permissionService.getPermissionTree());
    }
}

