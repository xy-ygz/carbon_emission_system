package com.bjfu.carbon.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bjfu.carbon.security.UserDetailsImpl;
import com.bjfu.carbon.service.AsyncAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志审计AOP切面
 * 记录用户访问接口时的相关信息
 * 
 * @author xgy
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AsyncAuditLogService asyncAuditLogService;

    /**
     * 定义切点：所有 Controller 类的方法（含 {@code AiQaProxyController} 转发的 /ai/qa/*）。
     * 注意：若网关/Nginx 将 /api/ai/qa/ 直连 Python 而不进本服务，则此类请求不会被本切面记录。
     */
    @Pointcut("execution(* com.bjfu.carbon.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        // 跳过OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return joinPoint.proceed();
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 获取用户信息
        Long userId = null;
        String username = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                userId = userDetails.getUserId();
                username = userDetails.getUsername();
            } else if (authentication != null && authentication.getPrincipal() instanceof String) {
                // 游客用户
                username = "guest";
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败", e);
        }

        // 获取请求信息
        String ip = getClientIp(request);
        String url = request.getRequestURI();
        String method = request.getMethod();
        String params = getRequestParams(request, joinPoint);

        // 执行原方法
        Object result = null;
        int statusCode = 200;
        String errorMessage = null;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            statusCode = 500;
            errorMessage = getExceptionMessage(e);
            throw e;
        } finally {
            // 计算响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 异步记录日志
            asyncAuditLogService.saveAuditLogAsync(userId, username, ip, url, method, params, statusCode, responseTime, errorMessage);
        }

        return result;
    }

    /**
     * 获取客户端IP地址
     * 考虑代理和负载均衡的情况
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip == null ? "unknown" : ip;
    }

    /**
     * 获取请求参数
     * 不记录请求体和响应体
     */
    private String getRequestParams(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        try {
            Map<String, Object> params = new HashMap<>();
            
            // 获取URL参数（包括GET请求的查询参数和POST请求的表单参数）
            Map<String, String[]> parameterMap = request.getParameterMap();
            boolean hasUrlParams = parameterMap != null && !parameterMap.isEmpty();
            
            if (hasUrlParams) {
                // 如果有URL参数，优先使用URL参数（避免与方法参数重复）
                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    String[] values = entry.getValue();
                    if (values != null && values.length > 0) {
                        params.put(entry.getKey(), values.length == 1 ? values[0] : values);
                    }
                }
            } else {
                // 如果没有URL参数（通常是POST请求的RequestBody），记录方法参数
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        if (arg != null &&
                            !(arg instanceof HttpServletRequest) &&
                            !(arg instanceof HttpServletResponse)) {
                            if (arg instanceof byte[]) {
                                putByteArrayAuditParam(params, i, (byte[]) arg);
                            } else if (isSimpleType(arg)) {
                                params.put("arg" + i, arg.toString());
                            } else {
                                // 对于复杂对象，只记录类名
                                params.put("arg" + i, arg.getClass().getSimpleName());
                            }
                        }
                    }
                }
            }
            
            if (params.isEmpty()) {
                return null;
            }
            
            // 限制参数长度，避免过长
            String paramsStr = JSON.toJSONString(params);
            return paramsStr.length() > 1000 ? paramsStr.substring(0, 1000) + "..." : paramsStr;
        } catch (Exception e) {
            log.debug("获取请求参数失败", e);
            return null;
        }
    }

    /**
     * {@code @RequestBody byte[]}：解码为 UTF-8，若为 JSON 且含 {@code question} 则记问题文案，否则记 body 摘要。
     */
    private void putByteArrayAuditParam(Map<String, Object> params, int index, byte[] bytes) {
        if (bytes.length == 0) {
            params.put("arg" + index, "");
            return;
        }
        String raw = new String(bytes, StandardCharsets.UTF_8);
        String trimmed = raw.length() > 1000 ? raw.substring(0, 1000) + "..." : raw;
        try {
            JSONObject jo = JSON.parseObject(raw);
            if (jo != null && jo.containsKey("question")) {
                String q = jo.getString("question");
                params.put("question", q != null ? q : "");
                return;
            }
        } catch (Exception e) {
            log.debug("解析 JSON body 失败，使用原文摘要", e);
        }
        params.put("body", trimmed);
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               clazz == Character.class ||
               Number.class.isAssignableFrom(clazz);
    }

    /**
     * 获取异常信息
     */
    private String getExceptionMessage(Exception e) {
        if (e == null) {
            return null;
        }
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }
        // 限制错误信息长度
        return message.length() > 200 ? message.substring(0, 200) + "..." : message;
    }
}
