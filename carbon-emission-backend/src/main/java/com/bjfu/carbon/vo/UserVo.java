package com.bjfu.carbon.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户视图对象（包含角色信息）
 */
@Data
public class UserVo implements Serializable {
    private Long id;
    private String name;
    private String username;
    private String department;
    private String phone;
    private Integer status;
    private Date createdTime;
    private Date modifiedTime;
    
    /**
     * 用户角色列表
     */
    private List<RoleVo> roles;
    
    private static final long serialVersionUID = 1L;
}

