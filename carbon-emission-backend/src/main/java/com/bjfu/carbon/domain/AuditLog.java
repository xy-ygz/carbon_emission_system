package com.bjfu.carbon.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 日志审计实体类
 * 用于记录用户访问接口时的相关信息
 *
 * @author xgy
 */
@TableName(value = "audit_log")
@Data
public class AuditLog implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 请求URL
     */
    private String url;

    /**
     * 请求方法（GET/POST等）
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 响应状态码
     */
    private Integer statusCode;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
