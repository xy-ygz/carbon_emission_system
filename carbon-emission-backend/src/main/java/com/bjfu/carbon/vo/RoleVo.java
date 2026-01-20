package com.bjfu.carbon.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 角色视图对象（包含权限信息）
 */
@Data
public class RoleVo implements Serializable {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private Integer order; // 权限等级（越小等级越高）
    private Date createdTime;
    private Date modifiedTime;
    
    /**
     * 角色权限列表
     */
    private List<PermissionVo> permissions;
    
    private static final long serialVersionUID = 1L;
}

