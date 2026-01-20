package com.bjfu.carbon.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 角色实体类
 * @TableName role
 */
@TableName(value ="role")
@Data
public class Role implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码（唯一）
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态（0禁用/1启用）
     */
    private Integer status;

    /**
     * 权限等级（越小等级越高，0=超级管理员，1=管理员，2=普通用户，3=游客）
     */
    @TableField("`order`")
    private Integer order;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 修改时间
     */
    private Date modifiedTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

