package com.bjfu.carbon;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 碳排放管理系统启动类
 * 
 * @author xgy
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.bjfu.carbon.mapper")
public class CarbonApplication extends SpringBootServletInitializer {

    static {
        // 在应用启动时禁用docx4j的字体自动扫描（性能优化）
        // 在docx4j初始化之前设置，避免扫描大量系统字体
        try {
            System.setProperty("org.docx4j.fonts.fop.fonts.autodetect", "false");
            System.setProperty("org.docx4j.fonts.fop.fonts.autodetect.path", "");
            System.setProperty("org.docx4j.fonts.fop.fonts.autodetect.FontInfoFinder.enabled", "false");
            log.info("已设置系统属性禁用docx4j字体自动扫描");
        } catch (Exception e) {
            log.warn("设置字体自动扫描属性失败: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(CarbonApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CarbonApplication.class);
    }

}
