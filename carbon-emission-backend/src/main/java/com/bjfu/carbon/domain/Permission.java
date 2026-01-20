package com.bjfu.carbon.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 权限实体类
 * @TableName permission
 */
@TableName(value ="permission")
@Data
public class Permission implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限编码（唯一）
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限类型（menu菜单/button按钮/api接口）
     */
    private String permissionType;

    /**
     * 父权限ID（0表示顶级）
     */
    private Long parentId;

    /**
     * 路由路径（菜单类型）
     */
    private String path;

    /**
     * 组件路径（菜单类型）
     */
    private String component;

    /**
     * 图标（菜单类型）
     */
    private String icon;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 状态（0禁用/1启用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 修改时间
     */
    private Date modifiedTime;

    /**
     * 子权限列表（不映射到数据库）
     */
    @TableField(exist = false)
    private List<Permission> children;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

