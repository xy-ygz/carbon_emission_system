package com.bjfu.carbon.strategy.impl;

import com.bjfu.carbon.common.Result;
import com.bjfu.carbon.domain.CarbonEmission;
import com.bjfu.carbon.domain.EmissionAndConsume;
import com.bjfu.carbon.service.CarbonEmissionService;
import com.bjfu.carbon.service.SchoolService;
import com.bjfu.carbon.strategy.ExportStrategy;
import com.bjfu.carbon.utils.XWPFUtils;
import com.bjfu.carbon.vo.MulberryDiagramVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Word导出策略实现
 * 基于ReportTemplate.docx模板，使用与前端相同的数据获取方式填充数据
 *
 * @author xgy
 */
@Slf4j
@Component
public class WordExportStrategy implements ExportStrategy {

    @Autowired
    private CarbonEmissionService carbonEmissionService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    @Qualifier("exportDataExecutor")
    private ExecutorService exportDataExecutor;

    private static final String TRANSFORMER_FACTORY = "javax.xml.transform.TransformerFactory";
    private static final String TRANSFORMER_FACTORY_IMPL = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
    private static final String TEMPLATE_FILE = "ReportTemplate.docx";
    private static final String SCHOOL_NAME = "北京林业大学";
    
    @Override
    public void export(Integer year, HttpServletResponse response) throws IOException {
        setTransformerFactory();
        
        if (!checkFontLoaded(response)) {
            return;
        }
        
        XWPFDocument document = createWordDocument(year);
        if (document == null) {
            return;
        }
        
        try {
            String fileName = SCHOOL_NAME + year + "年度碳排放报告.docx";
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setContentType(getContentType());
            
            document.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Word导出失败", e);
            throw new RuntimeException(e);
        } finally {
            closeDocument(document);
        }
    }
    
    /**
     * 设置TransformerFactory，解决 XML 转换实现类的兼容性问题
     */
    private void setTransformerFactory() {
        try {
            System.setProperty(TRANSFORMER_FACTORY, TRANSFORMER_FACTORY_IMPL);
        } catch (Exception e) {
            log.debug("设置 TransformerFactory 失败，使用默认值: {}", e.getMessage());
        }
    }
    
    /**
     * 检查字体加载状态
     */
    private boolean checkFontLoaded(HttpServletResponse response) throws IOException {
        if (!XWPFUtils.isFontLoaded()) {
            log.warn("中文字体正在加载中，导出可能失败，建议稍后重试");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("导出失败：系统正在初始化中文字体，请稍候几秒后重试");
            response.getWriter().flush();
            return false;
        }
        return true;
    }
    
    /**
     * 关闭文档
     */
    private void closeDocument(XWPFDocument document) {
        if (document != null) {
            try {
                document.close();
            } catch (IOException e) {
                log.warn("关闭文档时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 创建Word文档（提取的公共方法，供PDF导出复用）
     * 使用线程池异步并行加载数据库资源
     *
     * @param year 年份
     * @return 填充好数据的Word文档对象
     * @throws IOException 处理异常
     */
    public XWPFDocument createWordDocument(Integer year) throws IOException {
        // 1.1 异步任务1：获取总体碳排放情况（对应前端getTableData2 -> getJunzhi -> collegeCarbonEmission）
        CompletableFuture<Map<String, Double>> carbonInfoFuture = CompletableFuture.supplyAsync(
            () -> carbonEmissionService.collegeCarbonEmission(year, null), exportDataExecutor);
        
        // 1.2 异步任务2：获取表1数据（对应前端getTableData1 -> getBingtu -> getSpeciesCarbonData）
        CompletableFuture<List<EmissionAndConsume>> emissionAndConsumesFuture = CompletableFuture.supplyAsync(
            () -> carbonEmissionService.selectEmiAndConsume(year), exportDataExecutor);
        
        // 1.3 异步任务3：获取表3和图2数据（对应前端getTableData3 -> getCarbonMulberry -> getMulberryData）
        CompletableFuture<Result<Map<String, Object>>> mulberryResultFuture = CompletableFuture.supplyAsync(
            () -> carbonEmissionService.getMulberryData(year, null), exportDataExecutor);
        
        // 1.4 异步任务4：获取图5和图6数据（对应前端getChart6 -> getDuijizhuCategory -> getEmissionCategoryData）
        CompletableFuture<List<CarbonEmission>> carbonEmissionsFuture = CompletableFuture.supplyAsync(
            () -> carbonEmissionService.selectEmissionType(year), exportDataExecutor);
        
        // 1.5 异步任务5：获取学校信息（用于构建结论数据）
        CompletableFuture<Long> totalPeopleFuture = CompletableFuture.supplyAsync(
            () -> {
                Long people = schoolService.selectTotalPeople("北京林业大学");
                return people != null ? people : 0L;
            }, exportDataExecutor);
        
        // 2. 等待所有异步任务完成（设置超时时间10秒，避免长时间等待）
        // 使用 allOf 确保所有任务都完成后再继续，提升数据加载的并行度
        try {
            CompletableFuture.allOf(carbonInfoFuture, emissionAndConsumesFuture, mulberryResultFuture, 
                carbonEmissionsFuture, totalPeopleFuture).get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("异步加载导出数据超时（10秒）", e);
            throw new IOException("加载导出数据超时，请稍后重试", e);
        } catch (Exception e) {
            log.error("异步加载导出数据失败", e);
            throw new IOException("加载导出数据失败: " + e.getMessage(), e);
        }
        
        // 3. 获取异步任务的结果（此时所有任务已完成，join()不会阻塞）
        // 3.1 获取总体碳排放情况
        Map<String, Double> carbonInformation = carbonInfoFuture.join();
        
        // 3.2 获取表1数据（用于填充表1和构建结论）
        List<EmissionAndConsume> emissionAndConsumes = emissionAndConsumesFuture.join();
        if (emissionAndConsumes == null || emissionAndConsumes.isEmpty()) {
            throw new IOException("该年份暂无数据");
        }
        
        // 3.3 获取桑基图数据（用于填充表3和图2）
        Result<Map<String, Object>> mulberryResult = mulberryResultFuture.join();
        
        // 3.4 获取堆叠图数据（用于图5和图6）
        List<CarbonEmission> carbonEmissions = carbonEmissionsFuture.join();
        
        // 3.5 获取学校总人数（用于构建结论）
        Long totalPeople = totalPeopleFuture.join();
        
        // 4. 构建结论数据Map（对应前端getLastInfo -> exportReportDataByYear）
        Map<String, String> conclusionMap = buildConclusionMap(emissionAndConsumes, carbonInformation, totalPeople, year);
        
        // 5. 加载模板
        XWPFDocument document = loadTemplate();
        
        try {
            // 6. 填充文本数据（使用XWPFUtils.replaceWord）
            XWPFUtils.replaceWord(document, conclusionMap);
            
            // 7. 提取mulberryList数据（表3和图2，避免重复提取）
            List<MulberryDiagramVo> mulberryList = null;
            if (mulberryResult.getCode() == 200 && mulberryResult.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mulberryDataMap = (Map<String, Object>) mulberryResult.getData();
                @SuppressWarnings("unchecked")
                List<MulberryDiagramVo> extractedList = (List<MulberryDiagramVo>) mulberryDataMap.get("list");
                mulberryList = extractedList;
            }
            
            // 8. 填充表1数据
            if (!document.getTables().isEmpty()) {
                XWPFTable firstTable = document.getTables().get(0);
                XWPFUtils.replaceForTable(emissionAndConsumes, firstTable);
            }
            
            // 9. 填充表3数据（各排放源碳排放情况，对应前端getTableData3）
            if (mulberryList != null && !mulberryList.isEmpty() && document.getTables().size() > 2) {
                XWPFTable thirdTable = document.getTables().get(2);
                // 获取totalEmission值（单位：kg）
                Double totalEmission = carbonInformation.get("totalEmission");
                fillTable3(thirdTable, mulberryList, emissionAndConsumes, totalEmission);
                // 表3填充完成后，统一设置三线表样式（需要特殊处理合并单元格和汇总行）
                XWPFUtils.setThreeLineTableStyleForTable3(thirdTable);
            }
            
            // 10. 生成并插入图表（按照前端页面顺序：图2、图3、图4、图5、图6）
            // 10.1 图2（桑基图，对应前端generateInventoryChart -> getAllCategory）
            if (mulberryList != null && !mulberryList.isEmpty()) {
                XWPFUtils.createInventoryChartToWord(document, mulberryList, year);
            }
            
            // 10.2 图3和图4（柱状图和饼图，对应前端getChart3和getChart4 -> getBingtu）
            XWPFUtils.createBarAndPieToWord(document, emissionAndConsumes, year);
            
            // 10.3 图5和图6（堆叠柱状图，对应前端getChart6 -> getDuijizhuCategory）
            if (carbonEmissions != null && !carbonEmissions.isEmpty()) {
                XWPFUtils.createStackBarToWord(document, carbonEmissions, year);
            }
            
            return document;
        } catch (Exception e) {
	        try {
		        document.close();
	        } catch (IOException ignored) {
	        }
	        throw new RuntimeException("生成Word文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getSupportedExtension() {
        return "docx";
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    /**
     * 确定实际年份（与前端getTableData2逻辑一致）
     */
    private XWPFDocument loadTemplate() throws IOException {
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_FILE);
        if (templateStream == null) {
            throw new IOException("模板文件" + TEMPLATE_FILE + "不存在");
        }
        
        try {
            // 将InputStream转换为字节数组
            byte[] templateBytes = readInputStreamToBytes(templateStream);
            templateStream.close();
            
            // 使用OPCPackage打开，会自动忽略无效的目录条目
            try (java.io.ByteArrayInputStream byteArrayStream = new java.io.ByteArrayInputStream(templateBytes)) {
                OPCPackage opcPackage = OPCPackage.open(byteArrayStream);
                return new XWPFDocument(opcPackage);
            }
        } catch (Exception e) {
            log.warn("使用OPCPackage打开模板失败，回退到直接方式: {}", e.getMessage());
            // 回退到直接方式
            templateStream.close();
            templateStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_FILE);
            if (templateStream == null) {
                throw new IOException("模板文件" + TEMPLATE_FILE + "不存在");
            }
            return new XWPFDocument(templateStream);
        }
    }
    
    /**
     * 读取InputStream到字节数组
     */
    private byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    /**
     * 构建结论数据Map（对应前端getLastInfo -> exportReportDataByYear）
     */
    private Map<String, String> buildConclusionMap(List<EmissionAndConsume> emissionAndConsumes,
                                                   Map<String, Double> carbonInformation,
                                                   Long totalPeople,
                                                   int year) {
        Map<String, String> map = new HashMap<>();
        DecimalFormat df = new DecimalFormat("0.00");
        
        // 填充总体碳排放数据（单位转换：kg转换为t，除以1000）
        if (carbonInformation != null) {
            for (Map.Entry<String, Double> entry : carbonInformation.entrySet()) {
                double valueT = (entry.getValue() != null ? entry.getValue() : 0.0) / 1000.0;
                map.put(entry.getKey(), df.format(valueT));
            }
        }
        
        // 填充结论相关数据（与旧版exportReportByYear逻辑一致）
        if (emissionAndConsumes != null && !emissionAndConsumes.isEmpty()) {
            EmissionAndConsume first = emissionAndConsumes.get(0);
            map.put("maxMonth", String.valueOf(first.getEmissionMonthMax()));
            map.put("minMonth", String.valueOf(first.getEmissionMonthMin()));
            map.put("maxCategory", first.getObjectCategoryMax() != null ? first.getObjectCategoryMax() : "");
            map.put("minCategory", first.getObjectCategoryMin() != null ? first.getObjectCategoryMin() : "");
            // 单位转换：kg转换为t（除以1000）
            map.put("maxEmi", df.format((first.getEmiMaxNumber() != null ? first.getEmiMaxNumber() : 0.0) / 1000.0));
            map.put("minEmi", df.format((first.getEmiMinNumber() != null ? first.getEmiMinNumber() : 0.0) / 1000.0));
            
            // 填充各类型排放量（单位转换：kg转换为t，除以1000）
            List<EmissionAndConsume> emissionAndConsumeDir = new ArrayList<>();
            List<EmissionAndConsume> emissionAndConsumeInd = new ArrayList<>();
            List<EmissionAndConsume> emissionAndConsumeOther = new ArrayList<>();
            
            for (EmissionAndConsume item : emissionAndConsumes) {
                if (item.getEmissionType() == 0) {
                    emissionAndConsumeDir.add(item);
                } else if (item.getEmissionType() == 1) {
                    emissionAndConsumeInd.add(item);
                } else {
                    emissionAndConsumeOther.add(item);
                }
            }
            
            double dirEmiNumber = emissionAndConsumeDir.stream()
                    .mapToDouble(item -> item.getEmissionAmount() != null ? item.getEmissionAmount() : 0.0)
                    .sum() / 1000.0;
            double indEmiNumber = emissionAndConsumeInd.stream()
                    .mapToDouble(item -> item.getEmissionAmount() != null ? item.getEmissionAmount() : 0.0)
                    .sum() / 1000.0;
            double otherEmiNumber = emissionAndConsumeOther.stream()
                    .mapToDouble(item -> item.getEmissionAmount() != null ? item.getEmissionAmount() : 0.0)
                    .sum() / 1000.0;
            
            map.put("dirEmiNumber", df.format(dirEmiNumber));
            map.put("indEmiNumber", df.format(indEmiNumber));
            map.put("otherEmiNumber", df.format(otherEmiNumber));
            
            // 填充前5个排放源（单位转换：kg转换为t，除以1000）
            List<EmissionAndConsume> emissionTopFive = new ArrayList<>(emissionAndConsumes);
            emissionTopFive.sort((a, b) -> Double.compare(
                    b.getEmissionAmount() != null ? b.getEmissionAmount() : 0.0,
                    a.getEmissionAmount() != null ? a.getEmissionAmount() : 0.0
            ));
            
            for (int i = 0; i < Math.min(5, emissionTopFive.size()); i++) {
                EmissionAndConsume item = emissionTopFive.get(i);
                map.put("EmissionName" + (i + 1), item.getObjectCategory());
                double emissionAmountT = (item.getEmissionAmount() != null ? item.getEmissionAmount() : 0.0) / 1000.0;
                map.put("EmissionNumber" + (i + 1), df.format(emissionAmountT));
            }
        }
        
        map.put("totalPeople", String.valueOf(totalPeople));
        map.put("year", String.valueOf(year));
        return map;
    }

    /**
     * 填充表3数据（各排放源碳排放情况）
     * 对应前端getTableData3 -> getCarbonMulberry的处理逻辑
     */
    private void fillTable3(XWPFTable table, List<MulberryDiagramVo> mulberryList, 
                           List<EmissionAndConsume> emissionAndConsumes, Double totalEmission) {
        List<Table3Row> table3Rows = buildTable3Rows(mulberryList);
        sortTable3Rows(table3Rows);
        
        int rowIndex = 1;
        rowIndex = fillEmissionTypeRows(table, table3Rows, "其他排放", rowIndex);
        rowIndex = fillEmissionTypeRows(table, table3Rows, "直接排放", rowIndex);
        rowIndex = fillEmissionTypeRows(table, table3Rows, "间接排放", rowIndex);
        
        fillTotalRow(table, totalEmission, rowIndex);
    }
    
    /**
     * 构建表3行数据
     */
    private List<Table3Row> buildTable3Rows(List<MulberryDiagramVo> mulberryList) {
        List<Table3Row> table3Rows = new ArrayList<>();
        for (MulberryDiagramVo mulberry : mulberryList) {
            for (com.bjfu.carbon.vo.EmissionMulberryVo emissionVo : mulberry.getEmissionTypeAmount()) {
                Table3Row row = new Table3Row();
                row.emissionType = getEmissionTypeName(emissionVo.getEmissionType());
                row.name = mulberry.getObjectCategory();
                row.emissionAmount = (emissionVo.getEmissionAmount() != null ? emissionVo.getEmissionAmount() : 0.0) / 1000;
                table3Rows.add(row);
            }
        }
        return table3Rows;
    }
    
    /**
     * 获取排放类型名称
     */
    private String getEmissionTypeName(Integer emissionType) {
        if (emissionType == null) {
            return "其他排放";
        }
        switch (emissionType) {
            case 0: return "直接排放";
            case 1: return "间接排放";
            default: return "其他排放";
        }
    }
    
    /**
     * 排序表3行数据
     */
    private void sortTable3Rows(List<Table3Row> table3Rows) {
        table3Rows.sort((a, b) -> {
            int orderA = getEmissionTypeOrder(a.emissionType);
            int orderB = getEmissionTypeOrder(b.emissionType);
            if (orderA != orderB) {
                return Integer.compare(orderA, orderB);
            }
            return a.name.compareTo(b.name);
        });
    }
    
    /**
     * 获取排放类型排序顺序
     */
    private int getEmissionTypeOrder(String emissionType) {
        if ("其他排放".equals(emissionType)) return 0;
        if ("直接排放".equals(emissionType)) return 1;
        return 2;
    }
    
    /**
     * 填充指定排放类型的行
     */
    private int fillEmissionTypeRows(XWPFTable table, List<Table3Row> table3Rows, 
                                     String emissionType, int rowIndex) {
        List<EmissionAndConsume> typeList = table3Rows.stream()
            .filter(row -> emissionType.equals(row.emissionType))
            .map(row -> {
                EmissionAndConsume eac = new EmissionAndConsume();
                eac.setObjectCategory(row.name);
                eac.setEmissionAmount(row.emissionAmount);
                return eac;
            })
            .collect(java.util.stream.Collectors.toList());
        
        if (!typeList.isEmpty() && rowIndex < table.getRows().size()) {
            XWPFUtils.replaceForTable(rowIndex, typeList, table);
            rowIndex += typeList.size() + 1;
        }
        return rowIndex;
    }
    
    /**
     * 填充总计行
     */
    private void fillTotalRow(XWPFTable table, Double totalEmission, int rowIndex) {
        if (rowIndex >= table.getRows().size()) {
            return;
        }
        
        XWPFTableRow totalRow = table.getRow(rowIndex);
        List<XWPFTableCell> totalCells = totalRow.getTableCells();
        
        // 确保有足够的单元格
        while (totalCells.size() < 3) {
            totalRow.createCell();
            totalCells = totalRow.getTableCells();
        }
        
        // 设置第二列为"总计"
        String secondCellText = totalCells.get(1).getText();
        if (secondCellText == null || secondCellText.trim().isEmpty() || !secondCellText.contains("总计")) {
            totalCells.get(1).setText("总计");
        }
        
        // 设置第三列为总排放量（转换为t）
        double totalEmissionT = (totalEmission != null ? totalEmission : 0.0) / 1000.0;
        totalCells.get(2).setText(String.valueOf(totalEmissionT));
        XWPFUtils.setStyle(totalRow);
    }


    /**
     * 表3行数据
     */
    private static class Table3Row {
        String emissionType;
        String name;
        Double emissionAmount;
    }
}
