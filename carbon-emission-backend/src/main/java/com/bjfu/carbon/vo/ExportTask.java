package com.bjfu.carbon.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导出任务实体
 * 用于异步导出功能
 * 
 * @author xgy
 */
@Data
public class ExportTask implements Serializable {
    
    /**
     * 任务ID（唯一标识）
     */
    private String taskId;
    
    /**
     * 用户ID（可选，用于权限控制）
     */
    private Long userId;
    
    /**
     * 导出年份
     */
    private Integer year;
    
    /**
     * 导出格式（docx/pdf）
     */
    private String format;
    
    /**
     * 任务状态：PENDING（等待中）、PROCESSING（处理中）、COMPLETED（已完成）、FAILED（失败）
     */
    private TaskStatus status;
    
    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 任务开始处理时间
     */
    private LocalDateTime startTime;
    
    /**
     * 任务完成时间
     */
    private LocalDateTime completeTime;
    
    /**
     * 错误信息（如果任务失败）
     */
    private String errorMessage;
    
    /**
     * 文件缓存键（任务完成后，用于获取文件）
     */
    private String fileCacheKey;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,      // 等待中
        PROCESSING,   // 处理中
        COMPLETED,    // 已完成
        FAILED        // 失败
    }
    
    /**
     * 创建新任务
     */
    public static ExportTask create(String taskId, Long userId, Integer year, String format) {
        ExportTask task = new ExportTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setYear(year);
        task.setFormat(format);
        task.setStatus(TaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        return task;
    }
    
    /**
     * 标记任务开始处理
     */
    public void markProcessing() {
        this.status = TaskStatus.PROCESSING;
        this.startTime = LocalDateTime.now();
    }
    
    /**
     * 标记任务完成
     */
    public void markCompleted(String fileCacheKey, String fileName, Long fileSize) {
        this.status = TaskStatus.COMPLETED;
        this.completeTime = LocalDateTime.now();
        this.fileCacheKey = fileCacheKey;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    
    /**
     * 标记任务失败
     */
    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.completeTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
}
