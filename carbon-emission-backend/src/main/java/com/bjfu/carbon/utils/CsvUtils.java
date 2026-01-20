package com.bjfu.carbon.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV文件工具类
 * 用于读取CSV格式的文件
 *
 * @author xgy
 */
public class CsvUtils {

    /**
     * 从输入流中读取CSV数据
     * @param inputStream CSV文件输入流
     * @return 数据列表，每行数据以Map形式存储，key为"col0", "col1"等列索引
     * @throws Exception 读取异常
     */
    public static List<Map<String, String>> csvToList(InputStream inputStream) throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // 跳过空行
                }
                
                // 解析CSV行（处理引号内的逗号）
                List<String> values = parseCsvLine(line);
                
                if (!values.isEmpty()) {
                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < values.size(); i++) {
                        map.put("col" + i, values.get(i));
                    }
                    list.add(map);
                }
            }
        }
        
        return list;
    }

    /**
     * 解析CSV行，处理引号内的逗号
     * @param line CSV行
     * @return 解析后的值列表
     */
    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 双引号转义
                    currentValue.append('"');
                    i++;
                } else {
                    // 切换引号状态
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 不在引号内的逗号，作为分隔符
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        
        // 添加最后一个值
        values.add(currentValue.toString().trim());
        
        return values;
    }
}
