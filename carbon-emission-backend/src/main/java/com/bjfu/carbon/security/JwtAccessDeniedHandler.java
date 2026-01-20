package com.bjfu.carbon.security;

import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 访问拒绝处理器
 * 单一职责：只负责处理权限不足的请求（已认证，但权限不足）
 * 
 * @author xgy
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        response.setContentType("application/json;charset=UTF-8");
        
        // 获取当前认证用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 检查是否是游客（ID为-1）
        boolean isGuest = false;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            // 游客用户的ID为-1
            if (userDetails.getUserId() != null && userDetails.getUserId() == -1L) {
                isGuest = true;
            }
        }
        
        if (isGuest) {
            // 游客尝试访问需要权限的接口，返回未登录错误
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Object result = ResultUtils.error(ErrorCode.NOT_LOGIN, "请先完成登录");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), result);
            log.debug("游客尝试访问需要权限的接口: {}", request.getRequestURI());
        } else {
            // 已登录用户但无权限，返回无权限错误
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            Object result = ResultUtils.error(ErrorCode.NO_AUTH, "不允许访问");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), result);
            log.debug("已登录用户无权限访问: {}, 用户: {}", request.getRequestURI(), 
                    authentication != null ? authentication.getName() : "unknown");
        }
    }
}

