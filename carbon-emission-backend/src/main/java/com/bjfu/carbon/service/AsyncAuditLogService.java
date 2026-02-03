package com.bjfu.carbon.service;

import com.bjfu.carbon.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步日志审计服务
 * 用于异步保存日志记录，避免影响接口性能
 *
 * @author xgy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAuditLogService {

    private final AuditLogService auditLogService;

    /**
     * 异步保存日志到数据库和文件
     */
    @Async
    public void saveAuditLogAsync(Long userId, String username, String ip, String url, String method, 
                                  String params, int statusCode, long responseTime, String errorMessage) {
        try {
            // 创建日志对象
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username != null ? username : "anonymous");
            auditLog.setIp(ip);
            auditLog.setUrl(url);
            auditLog.setMethod(method);
            auditLog.setParams(params);
            auditLog.setStatusCode(statusCode);
            auditLog.setResponseTime(responseTime);
            auditLog.setCreateTime(new java.util.Date());

            // 保存到数据库
            auditLogService.saveLog(auditLog);

            // 记录到文件日志（使用专门的审计日志logger）
            String logMessage = String.format(
                "[审计日志] 用户ID:%s, 用户名:%s, IP:%s, URL:%s, 方法:%s, 状态码:%d, 响应时间:%dms, 参数:%s",
                userId != null ? userId : "null",
                username != null ? username : "anonymous",
                ip,
                url,
                method,
                statusCode,
                responseTime,
                params != null && params.length() > 500 ? params.substring(0, 500) + "..." : params
            );
            
            if (errorMessage != null) {
                logMessage += ", 错误:" + errorMessage;
            }
            
            log.info(logMessage);
        } catch (Exception e) {
            log.error("保存审计日志失败", e);
        }
    }
}
