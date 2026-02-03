package com.bjfu.carbon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.domain.AuditLog;
import com.bjfu.carbon.exception.BusinessException;
import com.bjfu.carbon.mapper.AuditLogMapper;
import com.bjfu.carbon.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 日志审计服务实现类
 *
 * @author xgy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    
    /**
     * 最大查询页码（防止深分页问题）
     */
    private static final int MAX_PAGE_NUMBER = 1000;
    
    /**
     * 默认最大查询时间范围（天）- 最多查询最近3个月的数据
     */
    private static final int MAX_QUERY_DAYS = 90;
    
    /**
     * 默认分页大小上限
     */
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public boolean saveLog(AuditLog auditLog) {
        try {
            if (auditLog.getCreateTime() == null) {
                auditLog.setCreateTime(new Date());
            }
            return auditLogMapper.insert(auditLog) > 0;
        } catch (Exception e) {
            log.error("保存日志失败", e);
            return false;
        }
    }

    @Override
    public IPage<AuditLog> getLogList(Page<AuditLog> page, Long userId, String username, String url, Date startDate, Date endDate) {
        // 1. 限制最大页码
        if (page.getCurrent() > MAX_PAGE_NUMBER) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("页码不能超过 %d 页，请缩小查询范围或使用时间筛选", MAX_PAGE_NUMBER));
        }
        
        // 2. 限制分页大小
        if (page.getSize() > MAX_PAGE_SIZE) {
            page.setSize(MAX_PAGE_SIZE);
            log.warn("分页大小超过限制，已自动调整为: {}", MAX_PAGE_SIZE);
        }
        
        // 3. 限制查询时间范围
        Date actualStartDate = startDate;
        Date actualEndDate = endDate;
        Date now = new Date();
        
        // 如果没有指定开始日期，默认查询最近MAX_QUERY_DAYS天的数据
        if (actualStartDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            cal.add(Calendar.DAY_OF_MONTH, -MAX_QUERY_DAYS);
            actualStartDate = cal.getTime();
        }
        
        // 如果没有指定结束日期，使用当前时间
        if (actualEndDate == null) {
            actualEndDate = now;
        }
        
        // 验证时间范围不超过MAX_QUERY_DAYS天
        long daysDiff = (actualEndDate.getTime() - actualStartDate.getTime()) / (24 * 60 * 60 * 1000);
        if (daysDiff > MAX_QUERY_DAYS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("查询时间范围不能超过 %d 天，请缩小查询范围", MAX_QUERY_DAYS));
        }
        
        // 验证开始日期不能早于MAX_QUERY_DAYS天前
        Calendar maxStartDateCal = Calendar.getInstance();
        maxStartDateCal.setTime(now);
        maxStartDateCal.add(Calendar.DAY_OF_MONTH, -MAX_QUERY_DAYS);
        Date maxStartDate = maxStartDateCal.getTime();
        if (actualStartDate.before(maxStartDate)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                String.format("开始日期不能早于 %d 天前，请调整查询时间范围", MAX_QUERY_DAYS));
        }
        
        // 结束日期需要包含当天的23:59:59
        Date endDateTime = new Date(actualEndDate.getTime() + 24 * 60 * 60 * 1000 - 1);
        
        // 计算偏移量
        long offset = (page.getCurrent() - 1) * page.getSize();
        
        // 4.优化查询方法：先通过索引获取ID，再根据ID查询
        // 避免对大量数据进行排序，只需要对索引中的ID进行排序
        List<AuditLog> records = auditLogMapper.selectPageOptimized(
            actualStartDate,
            endDateTime,
            userId,
            username,
            url,
            offset,
            page.getSize()
        );
        
        // 优化查询总数
        Long total = auditLogMapper.selectCountOptimized(
            actualStartDate,
            endDateTime,
            userId,
            username,
            url
        );
        
        // 构建分页结果
        page.setRecords(records);
        page.setTotal(total != null ? total : 0);
        
        return page;
    }

    @Override
    public AuditLog getLogById(Long id) {
        return auditLogMapper.selectById(id);
    }

    @Override
    public boolean updateLog(AuditLog auditLog) {
        try {
            return auditLogMapper.updateById(auditLog) > 0;
        } catch (Exception e) {
            log.error("更新日志失败", e);
            return false;
        }
    }

    @Override
    public boolean deleteLog(Long id) {
        try {
            return auditLogMapper.deleteById(id) > 0;
        } catch (Exception e) {
            log.error("删除日志失败", e);
            return false;
        }
    }
}
