package com.bjfu.carbon.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导出策略工厂
 * 根据文件扩展名动态选择导出策略
 *
 * @author xgy
 */
@Component
public class ExportStrategyFactory {

    private final Map<String, ExportStrategy> strategyMap;

    /**
     * 通过Spring自动注入所有ExportStrategy实现
     */
    @Autowired
    public ExportStrategyFactory(List<ExportStrategy> strategies) {
        // 按扩展名分组策略
        Map<String, List<ExportStrategy>> strategiesByExtension = strategies.stream()
                .collect(Collectors.groupingBy(ExportStrategy::getSupportedExtension));
        
        // 对于每个扩展名，如果有多个策略，选择第一个（通常只有一个）
        this.strategyMap = strategiesByExtension.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<ExportStrategy> strategyList = entry.getValue();
                            if (strategyList.isEmpty()) {
                                throw new RuntimeException("未找到支持扩展名 " + entry.getKey() + " 的导出策略");
                            }
                            // 如果有多个策略，选择第一个
                            return strategyList.get(0);
                        }
                ));
    }

    /**
     * 根据文件扩展名获取导出策略
     * @param fileExtension 文件扩展名（不包含点号），如 "docx", "pdf"
     * @return 导出策略
     * @throws RuntimeException 如果找不到对应的策略
     */
    public ExportStrategy getStrategy(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            throw new RuntimeException("文件扩展名不能为空");
        }
        
        String extension = fileExtension.toLowerCase().trim();
        ExportStrategy strategy = strategyMap.get(extension);
        
        if (strategy == null) {
            String supportedFormats = String.join(", ", strategyMap.keySet());
            throw new RuntimeException(
                String.format("不支持的文件格式: %s。支持的格式: %s", extension, supportedFormats)
            );
        }
        
        return strategy;
    }

    /**
     * 根据文件名获取导出策略
     * @param filename 文件名，如 "report.docx", "report.pdf"
     * @return 导出策略
     * @throws RuntimeException 如果文件名无效或找不到对应的策略
     */
    public ExportStrategy getStrategyByFilename(String filename) {
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
