package com.bjfu.carbon.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 导入策略工厂
 * 根据文件扩展名动态选择导入策略
 *
 * @author xgy
 */
@Component
public class ImportStrategyFactory {

    private final Map<String, ImportStrategy> strategyMap;

    /**
     * 通过Spring自动注入所有ImportStrategy实现
     */
    @Autowired
    public ImportStrategyFactory(List<ImportStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        ImportStrategy::getSupportedExtension,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 根据文件扩展名获取导入策略
     * @param fileExtension 文件扩展名（不包含点号），如 "xlsx", "csv"
     * @return 导入策略
     * @throws RuntimeException 如果找不到对应的策略
     */
    public ImportStrategy getStrategy(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            throw new RuntimeException("文件扩展名不能为空");
        }
        
        String extension = fileExtension.toLowerCase().trim();
        ImportStrategy strategy = strategyMap.get(extension);
        
        if (strategy == null) {
            String supportedFormats = String.join(", ", strategyMap.keySet());
            throw new RuntimeException(
                String.format("不支持的文件格式: %s。支持的格式: %s", extension, supportedFormats)
            );
        }
        
        return strategy;
    }

    /**
     * 根据文件名获取导入策略
     * @param filename 文件名，如 "data.xlsx", "data.csv"
     * @return 导入策略
     * @throws RuntimeException 如果文件名无效或找不到对应的策略
     */
    public ImportStrategy getStrategyByFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new RuntimeException("文件名必须包含扩展名: " + filename);
        }
        
        String extension = filename.substring(lastDotIndex + 1);
        return getStrategy(extension);
    }
}
