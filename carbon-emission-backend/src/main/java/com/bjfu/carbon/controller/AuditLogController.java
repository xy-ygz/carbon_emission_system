package com.bjfu.carbon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.AuditLog;
import com.bjfu.carbon.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 日志审计控制器
 * 提供日志的增删改查接口
 *
 * @author xgy
 */
@Slf4j
@RestController
@RequestMapping("/auditLog")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询日志列表
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getLogList")
    @PreAuthorize("hasAuthority('AUDIT_LOG_QUERY')")
    public Object getLogList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Page<AuditLog> page = new Page<>(current, size);
        IPage<AuditLog> result = auditLogService.getLogList(page, userId, username, url, startDate, endDate);
        return ResultUtils.success(result);
    }

    /**
     * 根据ID查询日志
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/getLogById")
    @PreAuthorize("hasAuthority('AUDIT_LOG_QUERY')")
    public Object getLogById(@RequestParam Long id) {
        AuditLog log = auditLogService.getLogById(id);
        if (log == null) {
            return ResultUtils.error(ErrorCode.NULL_ERROR, "日志不存在");
        }
        return ResultUtils.success(log);
    }

    /**
     * 新增日志（通常由AOP自动调用，这里提供接口以备需要）
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/addLog")
    @PreAuthorize("hasAuthority('AUDIT_LOG_ADD')")
    public Object addLog(@RequestBody AuditLog auditLog) {
        boolean success = auditLogService.saveLog(auditLog);
        return success ? ResultUtils.success("新增成功") : ResultUtils.error(ErrorCode.SYSTEM_ERROR, "新增失败");
    }

    /**
     * 更新日志
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/updateLog")
    @PreAuthorize("hasAuthority('AUDIT_LOG_UPDATE')")
    public Object updateLog(@RequestBody AuditLog auditLog) {
        boolean success = auditLogService.updateLog(auditLog);
        return success ? ResultUtils.success("更新成功") : ResultUtils.error(ErrorCode.SYSTEM_ERROR, "更新失败");
    }

    /**
     * 删除日志
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @GetMapping("/deleteLog")
    @PreAuthorize("hasAuthority('AUDIT_LOG_DELETE')")
    public Object deleteLog(@RequestParam Long id) {
        boolean success = auditLogService.deleteLog(id);
        return success ? ResultUtils.success("删除成功") : ResultUtils.error(ErrorCode.SYSTEM_ERROR, "删除失败");
    }
}
