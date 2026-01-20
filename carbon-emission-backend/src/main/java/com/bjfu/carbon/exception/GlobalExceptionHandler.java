package com.bjfu.carbon.exception;

import com.bjfu.carbon.common.Result;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author xgy
 * @since 2023-02-13
 */
@RestControllerAdvice//@RestControllerAdvice注解将作用在所有注解了@RequestMapping的控制器的方法上。
@Slf4j
public class GlobalExceptionHandler {

    //用于指定异常处理方法。当与@RestControllerAdvice配合使用时，用于全局处理控制器里的异常。
    @ExceptionHandler(BusinessException.class)
    public Result<?> businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    /**
     * 处理权限不足异常
     * 区分游客和已登录用户
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> accessDeniedExceptionHandler(AccessDeniedException e) {
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
            log.debug("游客尝试访问需要权限的接口");
            return ResultUtils.error(ErrorCode.NOT_LOGIN, "请先完成登录");
        } else {
            // 已登录用户但无权限，返回无权限错误
            log.debug("已登录用户无权限访问，用户: {}", 
                    authentication != null ? authentication.getName() : "unknown");
            return ResultUtils.error(ErrorCode.NO_AUTH, "不允许访问");
        }
    }

    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    public Result<?> rateLimitExceptionHandler(RateLimitException e) {
        log.warn("限流异常: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, e.getMessage(), "");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> runtimeExceptionHandler(RuntimeException e) {
        // 排除 AccessDeniedException 和 RateLimitException，因为它们已经被上面的方法处理了
        if (e instanceof AccessDeniedException) {
            return accessDeniedExceptionHandler((AccessDeniedException) e);
        }
        if (e instanceof RateLimitException) {
            return rateLimitExceptionHandler((RateLimitException) e);
        }
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "");
    }
}
