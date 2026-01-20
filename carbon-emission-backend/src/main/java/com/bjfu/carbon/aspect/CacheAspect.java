package com.bjfu.carbon.aspect;

import com.bjfu.carbon.annotation.RedisCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 缓存AOP切面
 * 实现方法结果缓存到Redis
 * 
 * @author xgy
 */
@Slf4j
@Aspect
@Component
public class CacheAspect {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 定义切点：所有标注了@RedisCache注解的方法
     */
    @Pointcut("@annotation(com.bjfu.carbon.annotation.RedisCache)")
    public void cacheablePointcut() {
    }
    
    /**
     * 环绕通知：执行缓存逻辑
     */
    @Around("cacheablePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名和注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisCache redisCache = method.getAnnotation(RedisCache.class);
        
        if (redisCache == null) {
            return joinPoint.proceed();
        }
        
        // 生成缓存键
        String cacheKey = generateCacheKey(joinPoint, redisCache);
        
        try {
            // 1. 尝试从缓存获取
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.debug("缓存命中，key: {}", cacheKey);
                return deserialize(cachedValue, method.getReturnType());
            }
            
            // 2. 缓存未命中，执行原方法
            log.debug("缓存未命中，执行方法，key: {}", cacheKey);
            Object result = joinPoint.proceed();
            
            // 3. 将结果写入缓存
            if (result != null || redisCache.cacheNull()) {
                String value = serialize(result);
                if (value != null) {
                    long expire = redisCache.expire();
                    if (expire > 0) {
                        stringRedisTemplate.opsForValue().set(cacheKey, value, expire, TimeUnit.SECONDS);
                    } else {
                        stringRedisTemplate.opsForValue().set(cacheKey, value);
                    }
                    log.debug("结果已缓存，key: {}, expire: {}秒", cacheKey, expire);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("缓存处理失败，key: {}", cacheKey, e);
            // 缓存失败不影响业务，直接执行原方法
            return joinPoint.proceed();
        }
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint, RedisCache redisCache) {
        String prefix = redisCache.prefix();
        String keyExpression = redisCache.key();
        
        // 如果指定了key表达式，使用SpEL解析
        if (StringUtils.hasText(keyExpression)) {
            try {
                EvaluationContext context = createEvaluationContext(joinPoint);
                Expression expression = parser.parseExpression(keyExpression);
                Object keyValue = expression.getValue(context);
                if (keyValue != null) {
                    return prefix + ":" + keyValue.toString();
                } else {
                    // 如果SpEL解析结果为null，尝试作为字符串字面量处理
                    // 检查是否是简单的字符串字面量（不包含SpEL特殊字符）
                    if (!keyExpression.contains("#") && !keyExpression.contains("$") 
                        && !keyExpression.contains("'") && !keyExpression.contains("\"")) {
                        return prefix + ":" + keyExpression;
                    }
                }
            } catch (Exception e) {
                // 如果SpEL解析失败，尝试作为字符串字面量处理
                // 检查是否是简单的字符串字面量（不包含SpEL特殊字符）
                if (!keyExpression.contains("#") && !keyExpression.contains("$") 
                    && !keyExpression.contains("'") && !keyExpression.contains("\"")) {
                    log.debug("SpEL解析失败，作为字符串字面量使用: {}", keyExpression);
                    return prefix + ":" + keyExpression;
                }
                log.warn("解析缓存键表达式失败: {}, 使用默认键", keyExpression, e);
            }
        }
        
        // 默认键生成策略：类名:方法名:参数值
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        StringBuilder keyBuilder = new StringBuilder(prefix).append(":").append(className).append(":").append(methodName);
        
        // 添加参数值
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg != null) {
                    keyBuilder.append(":").append(arg.toString());
                }
            }
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 创建SpEL表达式上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 添加方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
                context.setVariable("p" + i, args[i]); // 支持p0, p1等
            }
        }
        
        return context;
    }
    
    /**
     * 序列化对象为JSON字符串
     */
    private String serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("序列化对象失败", e);
            return null;
        }
    }
    
    /**
     * 反序列化JSON字符串为对象
     */
    private Object deserialize(String json, Class<?> returnType) {
        if (json == null || returnType == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, returnType);
        } catch (JsonProcessingException e) {
            log.error("反序列化对象失败，json: {}, type: {}", json, returnType.getName(), e);
            return null;
        }
    }
}
