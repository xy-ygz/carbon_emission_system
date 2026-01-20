package com.bjfu.carbon.strategy;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 批量导入策略接口
 * 使用策略模式支持不同格式的文件导入（Excel、CSV等）
 *
 * @author system
 */
public interface ImportStrategy {
    /**
     * 从输入流中读取数据
     * @param inputStream 文件输入流
     * @return 数据列表，每行数据以Map形式存储，key为"col0", "col1"等列索引
     * @throws Exception 读取异常
     */
    List<Map<String, String>> readData(InputStream inputStream) throws Exception;

    /**
     * 获取策略支持的文件扩展名（不包含点号）
     * @return 文件扩展名，如 "xlsx", "csv"
     */
    String getSupportedExtension();
}
