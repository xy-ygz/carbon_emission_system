package com.bjfu.carbon.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelUtils工具类
 *
 * @author xgy
 * @since 2023-02-13
 */
public class ExcelUtils {
    public static List<Map<String,String>> excelToShopIdList(InputStream inputStream) throws Exception {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            if (sheet == null) {
                throw new IOException("Excel文件中没有工作表");
            }
            
            List<Map<String,String>> list = new ArrayList<>();
            int totalRows = sheet.getPhysicalNumberOfRows();
            
            if (totalRows == 0) {
                return list;
            }
            
            // 获取表头的总列数
            if (sheet.getRow(0) == null) {
                throw new IOException("Excel文件第一行为空，无法读取表头");
            }
            
            int totalCols = sheet.getRow(0).getPhysicalNumberOfCells();
            
            // 遍历行
            for (int i = 0; i < totalRows; i++) {
                Map<String,String> map = new HashMap<>();
                if (sheet.getRow(i) == null) {
                    continue;
                }
                
                // 遍历列
                for (int j = 0; j < totalCols; j++) {
                    Cell cell = sheet.getRow(i).getCell(j);
                    // 判断该列是否为 null
                    if (cell == null) {
                        continue;
                    }
                    
                    CellType cellType = cell.getCellType();
                    if (cellType == CellType.BLANK) {
                        continue;
                    }
                    
                    // 根据单元格类型读取数据
                    String cellValue;
                    if (cellType == CellType.STRING) {
                        cellValue = cell.getStringCellValue();
                    } else if (cellType == CellType.NUMERIC) {
                        // 处理数值类型，避免精度丢失
                        cellValue = new BigDecimal(String.valueOf(cell.getNumericCellValue())).toPlainString();
                    } else if (cellType == CellType.BOOLEAN) {
                        cellValue = String.valueOf(cell.getBooleanCellValue());
                    } else if (cellType == CellType.FORMULA) {
                        // 公式类型，获取计算后的值
                        try {
                            cellValue = cell.getStringCellValue();
                        } catch (Exception e) {
                            cellValue = String.valueOf(cell.getNumericCellValue());
                        }
                    } else {
                        // 其他类型，尝试转换为字符串
                        cellValue = cell.toString();
                    }
                    
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        map.put("col"+j, cellValue);
                    }
                }
                if (!map.isEmpty()) {
                    list.add(map);
                }
            }
            return list;
        } catch (Exception e) {
            // 检查是否是POI的InvalidFormatException
            if (e.getClass().getName().equals("org.apache.poi.openxml4j.exceptions.InvalidFormatException") 
                    || (e.getMessage() != null && e.getMessage().contains("docProps"))) {
                // 转换POI异常为更友好的错误信息
                String errorMsg = "Excel文件格式错误或文件已损坏。";
                if (e.getMessage() != null && e.getMessage().contains("docProps")) {
                    errorMsg += "请尝试：1) 使用Microsoft Excel重新保存文件；2) 检查文件是否完整；3) 确认文件是标准的.xlsx格式。";
                } else {
                    errorMsg += "错误详情：" + e.getMessage();
                }
                throw new Exception(errorMsg, e);
            }
            // 如果是IOException，提供更详细的错误信息
            if (e instanceof IOException) {
                throw new Exception("读取Excel文件失败：" + e.getMessage() + "。请检查文件是否完整且未被其他程序占用。", e);
            }
            // 包装其他异常
            throw new Exception("读取Excel文件时发生错误：" + e.getMessage(), e);
        } finally {
            // 确保关闭workbook以释放资源
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    // 忽略关闭时的异常
                }
            }
        }
    }
}
