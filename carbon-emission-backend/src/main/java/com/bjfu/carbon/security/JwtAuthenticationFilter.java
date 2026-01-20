package com.bjfu.carbon.security;

import com.bjfu.carbon.domain.Role;
import com.bjfu.carbon.domain.User;
import com.bjfu.carbon.mapper.PermissionMapper;
import com.bjfu.carbon.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT认证过滤器
 * 单一职责：只负责JWT Token的验证和用户认证
 *
 * @author xgy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    
    // 缓存游客权限，避免每次请求都查询数据库
    private volatile Set<String> cachedGuestPermissions = null;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            boolean isTokenValid = false;
            String username = null;

            if (StringUtils.hasText(jwt)) {
                // 先获取用户名，避免重复解析Token
                username = tokenProvider.getUsernameFromToken(jwt);
                // 只有当用户名不为null时，才验证Token有效性
                if (username != null) {
                    isTokenValid = tokenProvider.validateToken(jwt, username);
                }
            }

            if (isTokenValid) {
                // 有有效的Token，加载用户信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 没有Token或Token无效，创建匿名用户（游客）
                UserDetails guestUserDetails = createGuestUserDetails();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        guestUserDetails, null, guestUserDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            // 即使出错，也创建游客用户，保证请求可以继续
            try {
                UserDetails guestUserDetails = createGuestUserDetails();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        guestUserDetails, null, guestUserDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("Could not create guest user", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 创建游客用户详情
     */
    private UserDetails createGuestUserDetails() {
        // 获取游客角色的权限
        Set<String> guestPermissions = getGuestPermissions();
        
        // 创建匿名用户对象
        User guestUser = new User();
        guestUser.setId(-1L); // 使用-1表示游客
        guestUser.setUsername("guest");
        guestUser.setName("游客");
        guestUser.setStatus(1);
        
        // 使用UserDetailsImpl创建用户详情
        return UserDetailsImpl.create(guestUser, guestPermissions);
    }

    /**
     * 获取游客角色的权限（带缓存）
     */
    private Set<String> getGuestPermissions() {
        // 如果缓存存在，直接返回
        if (cachedGuestPermissions != null) {
            return cachedGuestPermissions;
        }
        
        // 双重检查锁定，确保线程安全
        synchronized (this) {
            if (cachedGuestPermissions != null) {
                return cachedGuestPermissions;
            }
            
            try {
                // 查询游客角色
                Role guestRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleCode, "GUEST")
                );
                
                if (guestRole != null) {
                    // 获取游客角色的权限
                    Set<String> permissions = permissionMapper.getRolePermissions(guestRole.getId());
                    cachedGuestPermissions = permissions != null ? permissions : new HashSet<>();
                    return cachedGuestPermissions;
                }
            } catch (Exception e) {
                log.error("Failed to load guest permissions", e);
            }
            
            // 如果查询失败，返回空集合并缓存
            cachedGuestPermissions = new HashSet<>();
            return cachedGuestPermissions;
        }
    }

    /**
     * 从请求中获取JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

