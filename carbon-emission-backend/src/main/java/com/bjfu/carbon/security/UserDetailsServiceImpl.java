package com.bjfu.carbon.security;

import com.bjfu.carbon.domain.User;
import com.bjfu.carbon.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * UserDetailsService实现
 * 依赖倒置：实现Spring Security的UserDetailsService接口
 * 单一职责：只负责根据用户名加载用户信息
 *
 * @author xgy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;
    private final SecurityPermissionService securityPermissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 获取用户权限
        Set<String> permissions = securityPermissionService.getUserPermissions(user.getId());

        return UserDetailsImpl.create(user, permissions);
    }
}

