package com.bjfu.carbon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.domain.AuditLog;

import java.util.Date;

/**
 * 日志审计服务接口
 *
 * @author xgy
 */
public interface AuditLogService {

    /**
     * 保存日志记录
     *
     * @param auditLog 日志对象
     * @return 是否成功
     */
    boolean saveLog(AuditLog auditLog);

    /**
     * 分页查询日志列表
     *
     * @param page     分页参数
     * @param userId   用户ID（可选）
     * @param username 用户名（可选）
     * @param url      接口URL（可选）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 日志分页列表
     */
    IPage<AuditLog> getLogList(Page<AuditLog> page, Long userId, String username, String url, Date startDate, Date endDate);

    /**
     * 根据ID查询日志
     *
     * @param id 日志ID
     * @return 日志对象
     */
    AuditLog getLogById(Long id);

    /**
     * 更新日志
     *
     * @param auditLog 日志对象
     * @return 是否成功
     */
    boolean updateLog(AuditLog auditLog);

    /**
     * 删除日志
     *
     * @param id 日志ID
     * @return 是否成功
     */
    boolean deleteLog(Long id);
}
