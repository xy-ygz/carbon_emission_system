package com.bjfu.carbon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 支持IP限流和接口限流两种方式
 * 
 * @author xgy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * IP限流：同一IP每秒最大请求次数
     * 0表示不限制IP访问次数
     * 
     * @return IP每秒最大请求次数
     */
    int ipLimit() default 0;
    
    /**
     * 接口限流：同一接口每秒最大执行次数
     * 0表示不限制接口访问次数
     * 
     * @return 接口每秒最大执行次数
     */
    int apiLimit() default 0;
    
    /**
     * 限流时间窗口（秒）
     * 默认1秒
     * 
     * @return 时间窗口（秒）
     */
    int timeWindow() default 1;
    
    /**
     * 限流提示信息
     * 
     * @return 提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
