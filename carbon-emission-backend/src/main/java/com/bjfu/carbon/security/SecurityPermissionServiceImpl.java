package com.bjfu.carbon.security;

import com.bjfu.carbon.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 安全权限服务实现
 * 单一职责：只负责权限查询（用于Spring Security认证授权）
 *
 * @author xgy
 */
@Slf4j
@Service("securityPermissionService")
@RequiredArgsConstructor
public class SecurityPermissionServiceImpl implements SecurityPermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public Set<String> getUserPermissions(Long userId) {
        return permissionMapper.getUserPermissions(userId);
    }
}

