package com.bjfu.carbon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjfu.carbon.domain.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 日志审计Mapper接口
 *
 * @author xgy
 */
public interface AuditLogMapper extends BaseMapper<AuditLog> {
    
    /**
     * 优化的分页查询：使用子查询先获取ID，避免全表排序
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param userId 用户ID（可选）
     * @param username 用户名（可选）
     * @param url 接口URL（可选）
     * @param offset 偏移量
     * @param size 每页大小
     * @return 日志列表
     */
    List<AuditLog> selectPageOptimized(@Param("startDate") Date startDate,
                                       @Param("endDate") Date endDate,
                                       @Param("userId") Long userId,
                                       @Param("username") String username,
                                       @Param("url") String url,
                                       @Param("offset") long offset,
                                       @Param("size") long size);
    
    /**
     * 优化的总数查询：只统计符合条件的记录数
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param userId 用户ID（可选）
     * @param username 用户名（可选）
     * @param url 接口URL（可选）
     * @return 总记录数
     */
    Long selectCountOptimized(@Param("startDate") Date startDate,
                              @Param("endDate") Date endDate,
                              @Param("userId") Long userId,
                              @Param("username") String username,
                              @Param("url") String url);
}
