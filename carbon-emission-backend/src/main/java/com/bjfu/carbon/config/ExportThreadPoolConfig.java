package com.bjfu.carbon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

/**
 * 导出功能线程池配置
 * 用于异步并行加载数据库资源，提升导出性能
 * 
 * 设计说明：
 * 1. 导出功能涉及多个独立的数据库查询，可以并行执行
 * 2. 使用专用线程池避免影响主业务线程
 * 3. 线程池参数根据实际业务场景调优
 * 
 * @author xgy
 */
@Slf4j
@Configuration
@EnableAsync
public class ExportThreadPoolConfig {
    
    /**
     * 导出数据加载线程池
     * 
     * 线程池参数说明：
     * - 核心线程数：5（对应5个主要数据查询：总体碳排放、表1数据、桑基图数据、堆叠图数据、学校信息）
     * - 最大线程数：10（预留扩展空间，应对高并发场景）
     * - 队列容量：50（缓冲任务，避免任务丢失）
     * - 线程存活时间：60秒（非核心线程空闲60秒后回收）
     * - 拒绝策略：CallerRunsPolicy（调用者运行，确保任务不丢失，但会阻塞调用线程）
     * 
     * 线程命名规则：export-data-{n}，便于监控和调试
     * 
     * @return 导出数据加载线程池
     */
    @Bean(name = "exportDataExecutor")
    public ExecutorService exportDataExecutor() {
        int corePoolSize = 5;
        int maximumPoolSize = 10;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(50);
        
        // 自定义线程工厂：设置线程名称，便于监控和调试
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadNumber = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "export-data-" + threadNumber++);
                thread.setDaemon(false); // 非守护线程，确保任务完成
                return thread;
            }
        };
        
        // 拒绝策略：调用者运行，确保任务不丢失
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            workQueue,
            threadFactory,
            handler
        );
        
        // 不允许核心线程超时，保持核心线程常驻，提升响应速度
        executor.allowCoreThreadTimeOut(false);
        
        log.info("导出数据加载线程池初始化完成: corePoolSize={}, maximumPoolSize={}, queueCapacity=50", 
            corePoolSize, maximumPoolSize);
        
        return executor;
    }
}
