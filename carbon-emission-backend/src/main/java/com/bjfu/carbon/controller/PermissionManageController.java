package com.bjfu.carbon.controller;

import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.Permission;
import com.bjfu.carbon.service.PermissionService;
import com.bjfu.carbon.vo.PermissionVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 权限管理控制器
 * 单一职责：只负责权限管理相关的HTTP请求处理
 *
 * @author xgy
 */
@Slf4j
@RestController
@RequestMapping("/permissionManage")
@RequiredArgsConstructor
public class PermissionManageController {

    private final PermissionService permissionService;

    /**
     * 获取权限树
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getPermissionTree")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE_QUERY')")
    public Object getPermissionTree() {
        List<PermissionVo> tree = permissionService.getPermissionTree();
        return ResultUtils.success(tree);
    }

    /**
     * 新增权限
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addPermission")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE_ADD')")
    public Object addPermission(@Valid @RequestBody Permission permission) {
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        boolean success = permissionService.save(permission);
        return success ? ResultUtils.success("新增成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "新增失败");
    }

    /**
     * 更新权限
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updatePermission")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE_UPDATE')")
    public Object updatePermission(@Valid @RequestBody Permission permission) {
        if (permission.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "权限ID不能为空");
        }
        boolean success = permissionService.updatePermissionWithValidation(permission);
        return success ? ResultUtils.success("更新成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "更新失败");
    }

    /**
     * 删除权限
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/deletePermission")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE_DELETE')")
    public Object deletePermission(@RequestBody Permission permission) {
        if (permission.getId() == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "权限ID不能为空");
        }
        boolean success = permissionService.deletePermissionWithValidation(permission.getId());
        return success ? ResultUtils.success("删除成功") : ResultUtils.error(ErrorCode.PARAMS_ERROR, "删除失败");
    }
}

