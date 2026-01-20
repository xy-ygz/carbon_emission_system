package com.bjfu.carbon.strategy;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 导出策略接口
 * 使用策略模式支持不同格式的文件导出（Word、PDF等）
 *
 * @author xgy
 */
public interface ExportStrategy {
    /**
     * 导出报告
     * @param year 年份（可为空，为空时自动回退查找有数据的年份）
     * @param response HTTP响应对象
     * @throws IOException 导出异常
     */
    void export(Integer year, HttpServletResponse response) throws IOException;

    /**
     * 获取策略支持的文件扩展名（不包含点号）
     * @return 文件扩展名，如 "docx", "pdf"
     */
    String getSupportedExtension();

    /**
     * 获取策略支持的文件MIME类型
     * @return MIME类型，如 "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
     */
    String getContentType();
}
