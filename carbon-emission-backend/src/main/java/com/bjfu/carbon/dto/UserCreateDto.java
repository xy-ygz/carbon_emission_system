package com.bjfu.carbon.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 用户创建DTO
 * 单一职责：只负责用户创建请求的数据传输
 *
 * @author xgy
 */
@Data
public class UserCreateDto implements Serializable {
    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String department;
    private String phone;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private List<Long> roleIds;

    private static final long serialVersionUID = 1L;
}

