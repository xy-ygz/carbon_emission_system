package com.bjfu.carbon.security;

import java.util.Set;

/**
 * 安全权限服务接口
 * 接口隔离：只提供权限查询相关方法（用于Spring Security认证授权）
 *
 * @author xgy
 */
public interface SecurityPermissionService {
    /**
     * 获取用户的所有权限编码
     *
     * @param userId 用户ID
     * @return 权限编码集合
     */
    Set<String> getUserPermissions(Long userId);
}

