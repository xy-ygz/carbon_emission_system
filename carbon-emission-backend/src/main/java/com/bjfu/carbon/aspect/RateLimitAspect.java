package com.bjfu.carbon.aspect;

import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流AOP切面
 * 实现IP限流和接口限流功能
 * 
 * @author xgy
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    /**
     * 定义切点：所有标注了@RateLimit注解的方法
     */
    @Pointcut("@annotation(com.bjfu.carbon.annotation.RateLimit)")
    public void rateLimitPointcut() {
    }
    
    /**
     * 环绕通知：执行限流逻辑
     */
    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return joinPoint.proceed();
        }
        
        // 获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取请求上下文，跳过限流检查");
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        String ip = getClientIp(request);
        String apiKey = getApiKey(joinPoint);
        
        int ipLimit = rateLimit.ipLimit();
        int apiLimit = rateLimit.apiLimit();
        int timeWindow = rateLimit.timeWindow();
        String message = rateLimit.message();
        
        // IP限流检查
        if (ipLimit > 0) {
            String ipKey = "rate_limit:ip:" + ip;
            if (!checkLimit(ipKey, ipLimit, timeWindow)) {
                log.warn("IP限流触发：IP={}, 限制={}次/{}秒", ip, ipLimit, timeWindow);
                throw new RateLimitException(message);
            }
        }
        
        // 接口限流检查
        if (apiLimit > 0) {
            String apiLimitKey = "rate_limit:api:" + apiKey;
            if (!checkLimit(apiLimitKey, apiLimit, timeWindow)) {
                log.warn("接口限流触发：接口={}, 限制={}次/{}秒", apiKey, apiLimit, timeWindow);
                throw new RateLimitException(message);
            }
        }
        
        // 执行原方法
        return joinPoint.proceed();
    }
    
    /**
     * 检查是否超过限流阈值
     * 
     * @param key Redis键
     * @param limit 限制次数
     * @param timeWindow 时间窗口（秒）
     * @return true表示未超过限制，false表示超过限制
     */
    private boolean checkLimit(String key, int limit, int timeWindow) {
        try {
            // 获取当前计数
            String countStr = stringRedisTemplate.opsForValue().get(key);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);
            
            // 如果超过限制，返回false
            if (count >= limit) {
                return false;
            }
            
            // 增加计数
            if (count == 0) {
                // 第一次访问，设置过期时间
                stringRedisTemplate.opsForValue().set(key, "1", timeWindow, TimeUnit.SECONDS);
            } else {
                // 增加计数，不更新过期时间
                stringRedisTemplate.opsForValue().increment(key);
            }
            
            return true;
        } catch (Exception e) {
            log.error("限流检查失败，key={}", key, e);
            // 发生异常时，为了不影响业务，允许通过
            return true;
        }
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
     * 获取接口唯一标识
     * 格式：类名:方法名
     */
    private String getApiKey(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        return className + ":" + methodName;
    }
}
