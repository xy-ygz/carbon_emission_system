package com.bjfu.carbon.strategy.impl;

import com.bjfu.carbon.strategy.ImportStrategy;
import com.bjfu.carbon.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * CSV导入策略实现
 * 支持.csv格式的CSV文件导入
 *
 * @author xgy
 */
@Slf4j
@Component
public class CsvImportStrategy implements ImportStrategy {

    @Override
    public List<Map<String, String>> readData(InputStream inputStream) throws Exception {
        try {
            return CsvUtils.csvToList(inputStream);
        } catch (Exception e) {
            log.error("读取CSV文件失败", e);
            throw new Exception("读取CSV文件失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String getSupportedExtension() {
        return "csv";
    }
}
