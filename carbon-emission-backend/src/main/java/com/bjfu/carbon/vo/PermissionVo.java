package com.bjfu.carbon.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 权限视图对象（树形结构）
 */
@Data
public class PermissionVo implements Serializable {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private String description;
    private Integer status;
    private Date createdTime;
    private Date modifiedTime;
    
    /**
     * 子权限列表
     */
    private List<PermissionVo> children;
    
    private static final long serialVersionUID = 1L;
}

