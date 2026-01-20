package com.bjfu.carbon.strategy.impl;

import com.bjfu.carbon.strategy.ImportStrategy;
import com.bjfu.carbon.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Excel导入策略实现
 * 支持.xlsx格式的Excel文件导入
 *
 * @author xgy
 */
@Slf4j
@Component
public class ExcelImportStrategy implements ImportStrategy {

    @Override
    public List<Map<String, String>> readData(InputStream inputStream) throws Exception {
        try {
            return ExcelUtils.excelToShopIdList(inputStream);
        } catch (Exception e) {
            log.error("读取Excel文件失败", e);
            throw new Exception("读取Excel文件失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String getSupportedExtension() {
        return "xlsx";
    }
}
