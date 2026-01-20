package com.bjfu.carbon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis缓存注解
 * 用于标记需要缓存的方法，将结果缓存到Redis中
 * 
 * @author xgy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {
    
    /**
     * 缓存键前缀
     * 最终缓存键格式：prefix:key
     * 
     * @return 缓存键前缀
     */
    String prefix() default "cache";
    
    /**
     * 缓存键生成策略
     * 支持SpEL表达式，可以使用方法参数
     * 例如：year、#year、#p0等
     * 如果为空，则使用默认键生成策略（类名:方法名:参数值）
     * 
     * @return 缓存键表达式
     */
    String key() default "";
    
    /**
     * 缓存过期时间（秒）
     * 默认3600秒（1小时）
     * 
     * @return 过期时间（秒）
     */
    long expire() default 3600;
    
    /**
     * 是否缓存null值
     * 默认false，不缓存null值
     * 
     * @return 是否缓存null值
     */
    boolean cacheNull() default false;
}
