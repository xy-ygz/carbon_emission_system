package com.bjfu.carbon.utils;

import com.bjfu.carbon.domain.CarbonEmission;
import com.bjfu.carbon.domain.EmissionAndConsume;
import com.bjfu.carbon.vo.EmissionMulberryVo;
import com.bjfu.carbon.vo.MulberryDiagramVo;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJcTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Word文档导出工具类
 * 用于生成包含图表和表格的Word报告文档
 * 
 * @author Zhang chengliang
 * @date 2023/9/22
 */
@Slf4j
public class XWPFUtils {
    
    // ==================== 图表字体样式配置 ====================
    
    /** 图表字体名称：宋体（当未加载到含上下标的字体时使用） */
    private static final String CHART_FONT_NAME = "宋体";
    /** 优先加载的字体（仅列举 fonts 目录下实际存在的文件），按优先级排序 */
    private static final String[] CHART_FONT_FILE_PRIORITY = {
        "NotoSerifCJKsc-Regular.otf",
        "NotoSansCJKsc-VF.ttf",
        "SourceHanSerifSC-VF.ttf",
        "SimSun.ttf"
    };
    
    /** 加载的中文字体对象 */
    private static volatile Font loadedChineseFont = null;
    
    /** 字体加载状态：false=未加载，true=已加载（成功或失败） */
    private static volatile boolean fontLoadAttempted = false;
    
    /** 字体加载成功标志 */
    private static volatile boolean fontLoadSuccess = false;
    
    /** 字体加载锁 */
    private static final Object fontLoadLock = new Object();
    
    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;
    
    /** 重试延迟（毫秒） */
    private static final long RETRY_DELAY_MS = 500;
    
    static {
        // 异步尝试加载，不阻塞类初始化
        loadChineseFont(0);
    }
    
    /**
     * 加载中文字体文件（线程安全，支持重试）
     * 优先从文件系统加载（支持Docker部署），如果找不到则从classpath加载
     * 
     * @param retryCount 当前重试次数
     */
    private static void loadChineseFont(int retryCount) {
        if (fontLoadSuccess) {
            return; // 已经成功加载
        }
        
        synchronized (fontLoadLock) {
            if (fontLoadSuccess) {
                return; // 双重检查
            }
            
            try {
                if (retryCount == 0) {
                    log.info("开始加载中文字体文件...");
                } else {
                    log.info("重试加载中文字体文件（第{}次）...", retryCount);
                }
                
                // 按优先级查找并加载字体（某格式如 .otf 可能不被 Java 支持，失败则尝试下一个）
                File[] fontDirs = getFontDirectories();
                boolean loaded = false;
                for (String fileName : CHART_FONT_FILE_PRIORITY) {
                    File fontFile = null;
                    for (File fontDir : fontDirs) {
                        if (fontDir != null && fontDir.exists() && fontDir.isDirectory()) {
                            File candidate = new File(fontDir, fileName);
                            if (candidate.exists() && candidate.isFile() && candidate.length() > 0) {
                                fontFile = candidate;
                                break;
                            }
                        }
                    }
                    if (fontFile == null) {
                        String resourcePath = "fonts/" + fileName;
                        try (InputStream fontStream = XWPFUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
                            if (fontStream != null) {
                                String ext = fileName.contains(".otf") ? ".otf" : ".ttf";
                                File tempFontFile = File.createTempFile("chart_font_", ext);
                                tempFontFile.deleteOnExit();
                                Files.copy(fontStream, tempFontFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                fontFile = tempFontFile;
                            }
                        } catch (IOException e) {
                            log.debug("classpath 中未找到或无法复制字体 {}: {}", resourcePath, e.getMessage());
                        }
                    }
                    if (fontFile != null && fontFile.exists() && fontFile.length() > 0) {
                        try {
                            loadedChineseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                            ge.registerFont(loadedChineseFont);
                            fontLoadSuccess = true;
                            loaded = true;
                            log.info("✓ 已成功加载并注册图表字体: {} (文件: {}, 重试次数: {})",
                                    fontFile.getAbsolutePath(), fileName, retryCount);
                            break;
                        } catch (Exception ex) {
                            log.warn("字体文件加载失败，尝试下一个: {} - {}", fileName, ex.getMessage());
                        }
                    }
                }
                fontLoadAttempted = true;
                if (!loaded) {
                    if (retryCount < MAX_RETRY_COUNT) {
                        log.warn("未找到可加载的字体文件，将在{}ms后重试（第{}次）", RETRY_DELAY_MS * (retryCount + 1), retryCount + 1);
                    } else {
                        log.error("✗ 未找到可用字体文件（已重试{}次），将使用系统默认字体（可能导致中文或 CO₂ 下标不显示）", MAX_RETRY_COUNT);
                    }
                }
            } catch (Exception e) {
                if (retryCount < MAX_RETRY_COUNT) {
                    log.warn("加载中文字体失败，将在{}ms后重试（第{}次）: {}", RETRY_DELAY_MS * (retryCount + 1), retryCount + 1, e.getMessage());
                } else {
                    fontLoadAttempted = true; // 达到最大重试次数，标记为已尝试
                    log.error("✗ 加载中文字体失败（已重试{}次），图表中的中文可能显示为方块: {}", MAX_RETRY_COUNT, e.getMessage(), e);
                }
            }
        }
    }
    
    /** 当前重试次数 */
    private static volatile int currentRetryCount = 0;
    
    /**
     * 检查字体是否已成功加载
     * 
     * @return true=已成功加载，false=未加载或加载失败
     */
    public static boolean isFontLoaded() {
        return fontLoadSuccess;
    }
    
    /**
     * 检查字体加载状态（用于日志和错误提示）
     * 
     * @return 字体加载状态描述
     */
    public static String getFontLoadStatus() {
        if (fontLoadSuccess) {
            return "字体已成功加载";
        } else if (fontLoadAttempted) {
            return "字体加载失败，已重试" + currentRetryCount + "次";
        } else {
            return "字体正在加载中...";
        }
    }
    
    /**
     * 确保字体已加载（如果未加载则重试）
     * 用于在首次使用前确保字体可用
     * 支持延迟重试，处理Docker volume挂载延迟的情况
     * 
     * @return true=字体已加载或加载成功，false=字体加载失败
     */
    private static boolean ensureFontLoaded() {
        if (fontLoadSuccess) {
            return true; // 已经成功加载
        }
        
        // 如果还未尝试加载，先尝试一次
        if (!fontLoadAttempted) {
            log.info("首次使用图表功能，开始加载中文字体...");
            loadChineseFont(0);
            currentRetryCount = 0;
        }
        
        // 如果加载失败且未达到最大重试次数，进行延迟重试
        if (!fontLoadSuccess && currentRetryCount < MAX_RETRY_COUNT) {
            synchronized (fontLoadLock) {
                // 双重检查：再次确认是否需要重试
                if (!fontLoadSuccess && currentRetryCount < MAX_RETRY_COUNT) {
                    try {
                        log.info("字体加载中，正在重试（第{}次），预计等待{}ms...", 
                                currentRetryCount + 1, RETRY_DELAY_MS * (currentRetryCount + 1));
                        // 延迟重试，给Docker volume挂载时间
                        Thread.sleep(RETRY_DELAY_MS * (currentRetryCount + 1)); // 递增延迟
                        currentRetryCount++;
                        loadChineseFont(currentRetryCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("字体加载重试被中断");
                    }
                }
            }
        }
        
        return fontLoadSuccess;
    }
    
    /**
     * 获取字体文件目录列表（按优先级排序）
     * 支持Docker volume绑定和相对路径
     * 
     * @return 字体目录数组
     */
    private static File[] getFontDirectories() {
        List<File> fontDirs = new ArrayList<>();
        
        // 1. 环境变量指定的字体目录（最高优先级，支持Docker部署）
        String fontDirEnv = System.getenv("FONT_DIR");
        if (fontDirEnv != null && !fontDirEnv.isEmpty()) {
            File envFontDir = new File(fontDirEnv);
            if (envFontDir.exists() && envFontDir.isDirectory()) {
                fontDirs.add(envFontDir);
            }
        }
        
        // 2. Docker容器内常见路径 /app/fonts
        File dockerFontDir = new File("/app/fonts");
        if (dockerFontDir.exists() && dockerFontDir.isDirectory()) {
            fontDirs.add(dockerFontDir);
        }
        
        // 3. 相对路径 ./fonts（项目根目录下的fonts目录）
        File relativeFontDir = new File("fonts");
        if (relativeFontDir.exists() && relativeFontDir.isDirectory()) {
            fontDirs.add(relativeFontDir);
        }
        
        // 4. 相对路径 ./src/main/resources/fonts（开发环境）
        File resourcesFontDir = new File("src/main/resources/fonts");
        if (resourcesFontDir.exists() && resourcesFontDir.isDirectory()) {
            fontDirs.add(resourcesFontDir);
        }
        
        return fontDirs.toArray(new File[0]);
    }
    
    /**
     * 创建字体对象
     * 优先使用加载的字体文件，如果加载失败则使用字体名称
     * 
     * @param fontName 字体名称
     * @param style 字体样式（Font.PLAIN, Font.BOLD等）
     * @param size 字体大小
     * @return 字体对象
     * @throws RuntimeException 如果字体未加载且无法使用系统字体
     */
    private static Font createFont(String fontName, int style, int size) {
        // 确保字体已加载
        boolean fontReady = ensureFontLoaded();
        
        if (!fontReady && !fontLoadAttempted) {
            // 字体正在加载中，抛出异常提示用户稍后重试
            String status = getFontLoadStatus();
            log.warn("字体未加载完成，无法创建图表: {}", status);
            throw new RuntimeException("系统正在初始化中文字体，请稍候几秒后重试。当前状态: " + status);
        }
        
        if (loadedChineseFont != null) {
            try {
                // 使用加载的字体创建派生字体（显式 float 字号，便于可变字体 VF 正确实例化并渲染 ₂ 等字形）
                return loadedChineseFont.deriveFont(style, (float) size);
            } catch (Exception e) {
                log.warn("使用加载的字体创建派生字体失败，回退到字体名称: {}", e.getMessage());
                return new Font(fontName, style, size);
            }
        } else {
            // 回退到使用字体名称
            // 尝试使用系统已注册的字体
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();
            for (String availableFont : availableFonts) {
                if (availableFont.contains("宋体") || availableFont.contains("SimSun") || 
                    availableFont.contains("Songti") || availableFont.contains("STSong")) {
                    log.info("使用系统字体: {}", availableFont);
                    return new Font(availableFont, style, size);
                }
            }
            // 如果找不到中文字体，使用默认字体（可能显示为方块）
            log.warn("未找到中文字体，使用默认字体（可能导致中文显示为方块）");
            return new Font(fontName, style, size);
        }
    }
    
    /** 图2图表样式配置（标题：12pt加粗，普通文字：10pt正常） */
    private static final Font CHART_TITLE_FONT = createFont(CHART_FONT_NAME, Font.BOLD, 12);
    private static final Font CHART_TEXT_FONT = createFont(CHART_FONT_NAME, Font.PLAIN, 10);
    
    /** 图3、图4、图5、图6 字体懒加载 */
    private static volatile Font chart3456TitleFont;
    private static volatile Font chart3456TextFont;
    private static volatile Font chart3456LegendFont;
    private static volatile Font chart3456AxisLabelFont;
    private static volatile Font chart3456AxisTickFont;
    private static final Object CHART3456_FONT_LOCK = new Object();

    private static Font getChart3456TitleFont() {
        if (chart3456TitleFont == null) {
            synchronized (CHART3456_FONT_LOCK) {
                if (chart3456TitleFont == null) {
                    chart3456TitleFont = createFont(CHART_FONT_NAME, Font.BOLD, 26);
                }
            }
        }
        return chart3456TitleFont;
    }
    
    private static Font getChart3456TextFont() {
        if (chart3456TextFont == null) {
            synchronized (CHART3456_FONT_LOCK) {
                if (chart3456TextFont == null) {
                    chart3456TextFont = createFont(CHART_FONT_NAME, Font.PLAIN, 20);
                }
            }
        }
        return chart3456TextFont;
    }
    
    private static Font getChart3456LegendFont() {
        if (chart3456LegendFont == null) {
            synchronized (CHART3456_FONT_LOCK) {
                if (chart3456LegendFont == null) {
                    chart3456LegendFont = createFont(CHART_FONT_NAME, Font.PLAIN, 20);
                }
            }
        }
        return chart3456LegendFont;
    }
    
    private static Font getChart3456AxisLabelFont() {
        if (chart3456AxisLabelFont == null) {
            synchronized (CHART3456_FONT_LOCK) {
                if (chart3456AxisLabelFont == null) {
                    chart3456AxisLabelFont = createFont(CHART_FONT_NAME, Font.PLAIN, 20);
                }
            }
        }
        return chart3456AxisLabelFont;
    }
    
    private static Font getChart3456AxisTickFont() {
        if (chart3456AxisTickFont == null) {
            synchronized (CHART3456_FONT_LOCK) {
                if (chart3456AxisTickFont == null) {
                    chart3456AxisTickFont = createFont(CHART_FONT_NAME, Font.PLAIN, 20);
                }
            }
        }
        return chart3456AxisTickFont;
    }

    // ==================== 公共工具方法 ====================
    
    /**
     * 文本片段类型：普通文本、下标、上标
     */
    private enum TextSegmentType {
        NORMAL,    // 普通文本
        SUBSCRIPT, // 下标
        SUPERSCRIPT // 上标
    }
    
    /**
     * 文本片段：表示单位字符串中的一个片段
     */
    private static class TextSegment {
        String text;
        TextSegmentType type;
        
        TextSegment(String text, TextSegmentType type) {
            this.text = text;
            this.type = type;
        }
    }
    
    /**
     * 解析单位字符串，识别上标/下标字符
     * 例如："kgCO₂/Nm³" -> [NORMAL("kgCO"), SUBSCRIPT("2"), NORMAL("/Nm"), SUPERSCRIPT("3")]
     */
    private static List<TextSegment> parseUnitString(String unit) {
        List<TextSegment> segments = new ArrayList<>();
        if (unit == null || unit.isEmpty()) {
            return segments;
        }
        
        // Unicode下标字符映射：₂->2, ₃->3, 等等
        Map<Character, Character> subscriptMap = new HashMap<>();
        subscriptMap.put('\u2080', '0');
        subscriptMap.put('\u2081', '1');
        subscriptMap.put('\u2082', '2');
        subscriptMap.put('\u2083', '3');
        subscriptMap.put('\u2084', '4');
        subscriptMap.put('\u2085', '5');
        subscriptMap.put('\u2086', '6');
        subscriptMap.put('\u2087', '7');
        subscriptMap.put('\u2088', '8');
        subscriptMap.put('\u2089', '9');
        
        // Unicode上标字符映射：¹->1, ²->2, ³->3, 等等
        Map<Character, Character> superscriptMap = new HashMap<>();
        superscriptMap.put('\u00B9', '1');
        superscriptMap.put('\u00B2', '2');
        superscriptMap.put('\u00B3', '3');
        superscriptMap.put('\u2074', '4');
        superscriptMap.put('\u2075', '5');
        superscriptMap.put('\u2076', '6');
        superscriptMap.put('\u2077', '7');
        superscriptMap.put('\u2078', '8');
        superscriptMap.put('\u2079', '9');
        superscriptMap.put('\u2070', '0');
        
        StringBuilder currentText = new StringBuilder();
        TextSegmentType currentType = TextSegmentType.NORMAL;
        
        for (int i = 0; i < unit.length(); i++) {
            char c = unit.charAt(i);
            
            // 检查是否是下标字符
            if (subscriptMap.containsKey(c)) {
                // 如果之前有普通文本，先保存
                if (currentText.length() > 0 && currentType == TextSegmentType.NORMAL) {
                    segments.add(new TextSegment(currentText.toString(), TextSegmentType.NORMAL));
                    currentText.setLength(0);
                }
                // 添加下标字符（转换为普通数字）
                currentText.append(subscriptMap.get(c));
                currentType = TextSegmentType.SUBSCRIPT;
            }
            // 检查是否是上标字符
            else if (superscriptMap.containsKey(c)) {
                // 如果之前有普通文本或下标文本，先保存
                if (currentText.length() > 0 && currentType != TextSegmentType.SUPERSCRIPT) {
                    segments.add(new TextSegment(currentText.toString(), currentType));
                    currentText.setLength(0);
                }
                // 添加上标字符（转换为普通数字）
                currentText.append(superscriptMap.get(c));
                currentType = TextSegmentType.SUPERSCRIPT;
            }
            // 普通字符
            else {
                // 如果之前有下标或上标文本，先保存
                if (currentText.length() > 0 && currentType != TextSegmentType.NORMAL) {
                    segments.add(new TextSegment(currentText.toString(), currentType));
                    currentText.setLength(0);
                }
                // 添加普通字符
                currentText.append(c);
                currentType = TextSegmentType.NORMAL;
            }
        }
        
        // 保存最后一个片段
        if (currentText.length() > 0) {
            segments.add(new TextSegment(currentText.toString(), currentType));
        }
        
        return segments;
    }
    
    /**
     * 使用Word格式属性设置单元格文本（支持上标/下标）
     * 将单位字符串解析为多个文本片段，使用多个XWPFRun分别设置格式
     */
    private static void setCellTextWithFormatting(XWPFTableCell cell, String unit) {
        // 获取或创建段落
        XWPFParagraph paragraph;
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        if (paragraphs.isEmpty()) {
            paragraph = cell.addParagraph();
        } else {
            paragraph = paragraphs.get(0);
            // 清空现有runs
            for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
        }
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        
        // 解析单位字符串
        List<TextSegment> segments = parseUnitString(unit);
        
        // 如果没有片段，直接设置空文本
        if (segments.isEmpty()) {
            XWPFRun run = paragraph.createRun();
            run.setText("");
            return;
        }
        
        // 为每个片段创建Run并设置格式
        for (TextSegment segment : segments) {
            XWPFRun run = paragraph.createRun();
            run.setText(segment.text);
            
            // 根据片段类型设置格式
            switch (segment.type) {
                case SUBSCRIPT:
                    run.setSubscript(VerticalAlign.SUBSCRIPT);
                    break;
                case SUPERSCRIPT:
                    run.setSubscript(VerticalAlign.SUPERSCRIPT);
                    break;
                case NORMAL:
                default:
                    // 普通文本不需要特殊设置
                    break;
            }
        }
    }

    /**
     * 图3图例/系列名：返回 "CO2排放量"
     * 由于Java的Graphics2D字体渲染，渲染图表中无法显示下标字符，统一使用CO2
     */
    private static String getChartSeriesNameCO2Emission() {
        return "CO2排放量";
    }
    
    /** 获取临时图表文件目录路径 */
    private static String getTempChartDirectory() {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (!tempDir.endsWith(File.separator)) {
            tempDir += File.separator;
        }
        String foldPath = tempDir + "carbon_export_charts" + File.separator;
        File tempFolder = new File(foldPath);
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        return foldPath;
    }
    
    /**
     * 删除旧的图表文件（如果存在）
     * 
     * @param filePath 文件路径
     */
    private static void deleteOldChartFile(String filePath) {
        File oldFile = new File(filePath);
        if (oldFile.exists()) {
            boolean deleted = oldFile.delete();
            if (!deleted) {
                // 如果删除失败，尝试延迟删除（文件可能被Word文档引用）
                oldFile.deleteOnExit();
            }
        }
    }
    
    /**
     * 强制删除图表文件（延迟删除，用于文件可能被Word文档引用的情况）
     * 
     * @param filePath 文件路径
     */
    private static void forceDeleteChartFile(String filePath) {
        File oldFile = new File(filePath);
        if (oldFile.exists()) {
            // 先尝试立即删除
            boolean deleted = oldFile.delete();
            if (!deleted) {
                // 如果删除失败，标记为退出时删除
                oldFile.deleteOnExit();
                // 尝试延迟删除（给Word文档处理时间）
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 等待1秒
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    /**
     * 在Word文档中查找并返回目标Run对象
     * 
     * @param document Word文档对象
     * @param key 查找的关键字
     * @return 找到的Run对象，未找到返回null
     */
    public static XWPFRun findTargetRun(XWPFDocument document, String key) {
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            List<XWPFRun> runs = paragraph.getRuns();
            for (XWPFRun run : runs) {
                String text = run.getText(0);
                if (text != null && text.contains(key)) {
                    run.setText("", 0);
                    return run;
                }
            }
        }
        return null;
    }
    
    /**
     * 替换Word文档中的文本内容
     * 
     * @param document Word文档对象
     * @param replaceMap 替换映射表（key: 原文本, value: 新文本）
     * @return 处理后的Word文档对象
     */
    public static XWPFDocument replaceWord(XWPFDocument document, Map<String, String> replaceMap) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceForPara(paragraph, replaceMap);
        }
        for (XWPFTable table : document.getTables()) {
            for (int i = 0; i < table.getRows().size(); i++) {
                XWPFTableRow row = table.getRow(i);
                for (int j = 0; j < row.getTableCells().size(); j++) {
                    XWPFTableCell cell = row.getCell(j);
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceForPara(paragraph, replaceMap);
                    }
                }
            }
        }
        return document;
    }
    
    /**
     * 替换段落中的文本内容
     * 
     * @param paragraph 段落对象
     * @param replaceMap 替换映射表
     */
    public static void replaceForPara(XWPFParagraph paragraph, Map<String, String> replaceMap) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            Set<String> keySet = replaceMap.keySet();
            for (String key : keySet) {
                if (text != null && text.contains(key)) {
                    text = text.replace(key, replaceMap.get(key));
                    run.setText(text, 0);
                }
            }
        }
    }
    
    /**
     * 设置表格行的样式（居中对齐）
     * 
     * @param row 表格行对象
     */
    public static void setStyle(XWPFTableRow row) {
        for (XWPFTableCell tableCell : row.getTableCells()) {
            List<XWPFParagraph> paragraphs = tableCell.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                paragraph.setFontAlignment(2);
            }
        }
    }
    
    /**
     * 跨行合并单元格
     * 
     * @param table 要合并单元格的表格
     * @param col 要合并哪一列的单元格
     * @param fromRow 从哪一行开始合并单元格
     * @param toRow 合并到哪一行（包含）
     */
    public static void mergeCellsVertically(XWPFTable table, int col, int fromRow, int toRow) {
        if (fromRow > toRow) {
            log.warn("mergeCellsVertically: fromRow({}) > toRow({}), 跳过合并", fromRow, toRow);
            return;
        }
        
        for (int rowIndex = fromRow; rowIndex <= toRow; rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            if (row == null) {
                log.warn("mergeCellsVertically: 第{}行不存在，跳过", rowIndex);
                continue;
            }
            
            XWPFTableCell cell = row.getCell(col);
            if (cell == null) {
                log.warn("mergeCellsVertically: 第{}行第{}列不存在，跳过", rowIndex, col);
                continue;
            }
            
            CTTc cttc = cell.getCTTc();
            CTTcPr tcPr = cttc.getTcPr();
            if (tcPr == null) {
                tcPr = cttc.addNewTcPr();
            }
            
            // 清除已有的VMerge设置，避免冲突
            if (tcPr.getVMerge() != null) {
                tcPr.unsetVMerge();
            }
            
            // 设置合并属性
            if (rowIndex == fromRow) {
                // 第一个合并单元格设置为RESTART
                tcPr.addNewVMerge().setVal(STMerge.RESTART);
            } else {
                // 后续合并单元格设置为CONTINUE
                tcPr.addNewVMerge().setVal(STMerge.CONTINUE);
            }
        }
        
        // 设置第一个单元格的垂直居中对齐
        XWPFTableRow firstRow = table.getRow(fromRow);
        if (firstRow != null) {
            XWPFTableCell firstCell = firstRow.getCell(col);
            if (firstCell != null) {
                CTTc cttc = firstCell.getCTTc();
                CTTcPr ctPr = cttc.getTcPr();
                if (ctPr == null) {
                    ctPr = cttc.addNewTcPr();
                }
                if (ctPr.getVAlign() == null) {
                    ctPr.addNewVAlign().setVal(STVerticalJc.CENTER);
                } else {
                    ctPr.getVAlign().setVal(STVerticalJc.CENTER);
                }
                
                // 设置段落居中对齐
                if (cttc.getPList() != null && !cttc.getPList().isEmpty()) {
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP p = cttc.getPList().get(0);
                    if (p.getPPr() == null) {
                        p.addNewPPr();
                    }
                    if (p.getPPr().getJc() == null) {
                        p.getPPr().addNewJc().setVal(STJc.CENTER);
                    } else {
                        p.getPPr().getJc().setVal(STJc.CENTER);
                    }
                }
            }
        }
    }
    
    // ==================== 图2：碳排放核算范围及清单（桑基图） ====================
    
    /**
     * 创建图2：碳排放核算范围及清单（桑基图）
     * 展示分类到排放类型的关系，包含根节点
     * 
     * @param document Word文档对象
     * @param mulberryList 桑基图数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createInventoryChartToWord(XWPFDocument document, 
                                                   List<MulberryDiagramVo> mulberryList,
                                                   int year) throws Exception {
        createInventoryChartWithGraphics2D(document, mulberryList, year);
    }
    
    /**
     * 使用Graphics2D绘制桑基图（包含根节点）
     * 
     * @param document Word文档对象
     * @param mulberryList 桑基图数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    private static void createInventoryChartWithGraphics2D(XWPFDocument document, 
                                                           List<MulberryDiagramVo> mulberryList,
                                                           int year) throws Exception {
        // 确保字体已加载（处理Docker volume挂载延迟的情况）
        ensureFontLoaded();
        
        String foldPath = getTempChartDirectory();
        String fileNameInventory = "chartInventory" + year + ".png";
        deleteOldChartFile(foldPath + fileNameInventory);
        
        // 收集所有分类和排放类型数据
        SankeyData sankeyData = collectSankeyData(mulberryList);
        
        // 创建高分辨率图像
        int width = 400;
        int height = 400;
        int scale = 2;
        BufferedImage image = createHighResolutionImage(width, height, scale);
        Graphics2D g2d = image.createGraphics();
        setupGraphics2D(g2d, scale);
        
        // 绘制标题
        drawSankeyTitle(g2d, year, width);
        
        // 计算分类框高度（按排放量比例分配）
        Map<String, Integer> categoryHeights = calculateCategoryHeights(
                sankeyData.categories, sankeyData.categoryTotals, height);
        
        // 绘制根节点
        int rootCenterY = drawRootNode(g2d, width, height);
        
        // 绘制分类框
        Map<String, Integer> categoryYMap = drawCategoryBoxes(
                g2d, sankeyData.categories, categoryHeights, width, height);
        
        // 绘制排放类型框
        Map<Integer, Integer> emissionTypeYMap = drawEmissionTypeBoxes(
                g2d, sankeyData.emissionTypes, sankeyData.emissionTypeCodes, 
                sankeyData.categoryToEmissionType, sankeyData.categories, width, height);
        
        // 绘制连接线：根节点 -> 分类
        drawRootToCategoryLines(g2d, rootCenterY, categoryYMap, width);
        
        // 绘制连接线：分类 -> 排放类型（包含标签，避免重叠）
        drawCategoryToEmissionTypeLines(g2d, categoryYMap, emissionTypeYMap, 
                sankeyData.categoryToEmissionType, sankeyData.emissionTypes, 
                sankeyData.emissionTypeCodes, width);
        
        g2d.dispose();
        
        // 保存图像并插入Word文档
        File inventoryFile = new File(foldPath + fileNameInventory);
        try {
            javax.imageio.ImageIO.write(image, "PNG", inventoryFile);
            insertImageToWord(document, inventoryFile, fileNameInventory, "inventoryChart", width, height);
        } finally {
            deleteOldChartFile(foldPath + fileNameInventory);
        }
    }
    
    /**
     * 桑基图数据收集类
     */
    private static class SankeyData {
        Set<String> categories;
        Map<String, Map<Integer, Double>> categoryToEmissionType;
        Map<String, Double> categoryTotals;
        String[] emissionTypes;
        int[] emissionTypeCodes;
    }
    
    /**
     * 收集桑基图所需的数据
     * 
     * @param mulberryList 桑基图数据列表
     * @return 整理后的桑基图数据
     */
    private static SankeyData collectSankeyData(List<MulberryDiagramVo> mulberryList) {
        SankeyData data = new SankeyData();
        data.categories = new LinkedHashSet<>();
        data.categoryToEmissionType = new HashMap<>();
        data.categoryTotals = new HashMap<>();
        data.emissionTypes = new String[]{"其他排放", "直接排放", "间接排放"};
        data.emissionTypeCodes = new int[]{2, 0, 1};
        
        // 收集分类和排放类型数据
        for (MulberryDiagramVo mulberry : mulberryList) {
            String category = mulberry.getObjectCategory();
            data.categories.add(category);
            Map<Integer, Double> emissionMap = new HashMap<>();
            for (EmissionMulberryVo emissionVo : mulberry.getEmissionTypeAmount()) {
                double amountT = (emissionVo.getEmissionAmount() != null ? emissionVo.getEmissionAmount() : 0.0) / 1000.0;
                emissionMap.put(emissionVo.getEmissionType(), amountT);
            }
            data.categoryToEmissionType.put(category, emissionMap);
        }
        
        // 计算每个分类的总排放量
        for (String category : data.categories) {
            double total = data.categoryToEmissionType.getOrDefault(category, new HashMap<>())
                    .values().stream().mapToDouble(Double::doubleValue).sum();
            data.categoryTotals.put(category, total);
        }
        
        return data;
    }
    
    /**
     * 创建高分辨率图像
     * 
     * @param width 原始宽度
     * @param height 原始高度
     * @param scale 缩放倍数
     * @return 高分辨率图像
     */
    private static BufferedImage createHighResolutionImage(int width, int height, int scale) {
        int imageWidth = width * scale;
        int imageHeight = height * scale;
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imageWidth, imageHeight);
        g2d.dispose();
        return image;
    }
    
    /**
     * 设置Graphics2D的渲染质量
     * 
     * @param g2d Graphics2D对象
     * @param scale 缩放倍数
     */
    private static void setupGraphics2D(Graphics2D g2d, int scale) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.scale(scale, scale);
    }
    
    /**
     * 绘制桑基图标题
     * 
     * @param g2d Graphics2D对象
     * @param year 年份
     * @param width 图像宽度
     */
    private static void drawSankeyTitle(Graphics2D g2d, int year, int width) {
        g2d.setColor(new Color(51, 51, 51));
        g2d.setFont(CHART_TITLE_FONT);
        String title = year + "年碳排放核算范围及清单";
        FontMetrics titleMetrics = g2d.getFontMetrics(CHART_TITLE_FONT);
        int titleWidth = titleMetrics.stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, 20);
    }
    
    /**
     * 计算分类框高度（按排放量比例分配）
     * 
     * @param categories 分类列表
     * @param categoryTotals 每个分类的总排放量
     * @param height 可用高度
     * @return 每个分类的高度映射
     */
    private static Map<String, Integer> calculateCategoryHeights(Set<String> categories, 
                                                                  Map<String, Double> categoryTotals, 
                                                                  int height) {
        int categoryCount = categories.size();
        int topY = 45;
        int bottomY = height - 15;
        int availableHeight = bottomY - topY;
        int boxSpacing = 3;
        int totalSpacing = (categoryCount - 1) * boxSpacing;
        int totalBoxHeight = availableHeight - totalSpacing;
        
        double totalEmission = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Integer> categoryHeights = new HashMap<>();
        int minBoxHeight = 20;
        
        List<String> categoryList = new ArrayList<>(categories);
        Map<String, Integer> proportionalHeights = new HashMap<>();
        for (String category : categoryList) {
            double emission = categoryTotals.get(category);
            double ratio = totalEmission > 0 ? emission / totalEmission : 1.0 / categoryCount;
            proportionalHeights.put(category, (int)(totalBoxHeight * ratio));
        }
        
        int totalProportionalHeight = proportionalHeights.values().stream().mapToInt(Integer::intValue).sum();
        int allocatedHeight = 0;
        
        if (totalProportionalHeight > totalBoxHeight && totalProportionalHeight > 0) {
            double heightScale = (double)totalBoxHeight / totalProportionalHeight;
            for (String category : categoryList) {
                int scaledHeight = Math.max(minBoxHeight, (int)(proportionalHeights.get(category) * heightScale));
                categoryHeights.put(category, scaledHeight);
                allocatedHeight += scaledHeight;
            }
        } else {
            for (String category : categoryList) {
                int boxHeight = Math.max(minBoxHeight, proportionalHeights.get(category));
                categoryHeights.put(category, boxHeight);
                allocatedHeight += boxHeight;
            }
        }
        
        if (!categoryList.isEmpty() && allocatedHeight != totalBoxHeight) {
            String lastCategory = categoryList.get(categoryList.size() - 1);
            int lastBoxHeight = Math.max(minBoxHeight, 
                    categoryHeights.getOrDefault(lastCategory, minBoxHeight) + (totalBoxHeight - allocatedHeight));
            categoryHeights.put(lastCategory, lastBoxHeight);
        }
        
        return categoryHeights;
    }
    
    /**
     * 绘制根节点
     * 
     * @param g2d Graphics2D对象
     * @param width 图像宽度
     * @param height 图像高度
     * @return 根节点中心Y坐标
     */
    private static int drawRootNode(Graphics2D g2d, int width, int height) {
        int rootX = 5;
        int topY = 45;
        int bottomY = height - 15;
        int availableHeight = bottomY - topY;
        
        String rootNodeName = "校园碳排放核算范围及清单";
        int rootBoxWidth = 75;
        int rootBoxHeight = Math.min(availableHeight, 80);
        int rootY = topY + (availableHeight - rootBoxHeight) / 2;
        
        g2d.setColor(new Color(128, 100, 162));
        g2d.fillRoundRect(rootX, rootY, rootBoxWidth, rootBoxHeight, 8, 8);
        g2d.setColor(new Color(51, 51, 51));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(rootX, rootY, rootBoxWidth, rootBoxHeight, 8, 8);
        
        // 绘制根节点文字（每行5个字）
        g2d.setColor(Color.WHITE);
        g2d.setFont(CHART_TITLE_FONT);
        FontMetrics rootMetrics = g2d.getFontMetrics(CHART_TITLE_FONT);
        int charsPerLine = 5;
        List<String> rootLinesList = new ArrayList<>();
        for (int i = 0; i < rootNodeName.length(); i += charsPerLine) {
            int end = Math.min(i + charsPerLine, rootNodeName.length());
            rootLinesList.add(rootNodeName.substring(i, end));
        }
        String[] rootLines = rootLinesList.toArray(new String[0]);
        
        int totalTextHeight = (rootLines.length - 1) * rootMetrics.getHeight();
        int rootTextY = rootY + rootBoxHeight / 2 - totalTextHeight / 2 + rootMetrics.getAscent() / 2;
        
        for (int i = 0; i < rootLines.length; i++) {
            int lineWidth = rootMetrics.stringWidth(rootLines[i]);
            g2d.drawString(rootLines[i], rootX + (rootBoxWidth - lineWidth) / 2, 
                    rootTextY + i * rootMetrics.getHeight());
        }
        
        return rootY + rootBoxHeight / 2;
    }
    
    /**
     * 绘制分类框
     * 
     * @param g2d Graphics2D对象
     * @param categories 分类列表
     * @param categoryHeights 每个分类的高度
     * @param width 图像宽度
     * @param height 图像高度
     * @return 每个分类的中心Y坐标映射
     */
    private static Map<String, Integer> drawCategoryBoxes(Graphics2D g2d, 
                                                           Set<String> categories, 
                                                           Map<String, Integer> categoryHeights, 
                                                           int width, 
                                                           int height) {
        int categoryX = 105;
        int topY = 45;
        int boxSpacing = 3;
        int categoryBoxWidth = 50;
        int minBoxHeight = 20;
        
        Map<String, Integer> categoryYMap = new HashMap<>();
        int currentY = topY;
        
        for (String category : categories) {
            int boxH = categoryHeights.getOrDefault(category, minBoxHeight);
            
            g2d.setColor(new Color(79, 129, 189));
            g2d.fillRoundRect(categoryX, currentY, categoryBoxWidth, boxH, 3, 3);
            g2d.setColor(new Color(51, 51, 51));
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRoundRect(categoryX, currentY, categoryBoxWidth, boxH, 3, 3);
            
            // 绘制分类名称（支持换行）
            g2d.setColor(Color.WHITE);
            g2d.setFont(CHART_TEXT_FONT);
            FontMetrics metrics = g2d.getFontMetrics(CHART_TEXT_FONT);
            
            List<String> categoryLines = wrapText(category, metrics, categoryBoxWidth - 6);
            int totalCategoryTextHeight = (categoryLines.size() - 1) * metrics.getHeight();
            int categoryTextY = currentY + boxH / 2 - totalCategoryTextHeight / 2 + metrics.getAscent() / 2;
            
            for (int i = 0; i < categoryLines.size(); i++) {
                String line = categoryLines.get(i);
                int lineWidth = metrics.stringWidth(line);
                g2d.drawString(line, categoryX + (categoryBoxWidth - lineWidth) / 2, 
                        categoryTextY + i * metrics.getHeight());
            }
            
            categoryYMap.put(category, currentY + boxH / 2);
            currentY += boxH + boxSpacing;
        }
        
        return categoryYMap;
    }
    
    /**
     * 文本换行处理
     * 
     * @param text 原始文本
     * @param metrics 字体度量
     * @param maxWidth 最大宽度
     * @return 换行后的文本列表
     */
    private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        
        while (!remaining.isEmpty()) {
            int charsToShow = 0;
            for (int i = 1; i <= remaining.length(); i++) {
                String testStr = remaining.substring(0, i);
                if (metrics.stringWidth(testStr) <= maxWidth) {
                    charsToShow = i;
                } else {
                    break;
                }
            }
            
            if (charsToShow == 0) {
                charsToShow = 1;
            }
            
            lines.add(remaining.substring(0, charsToShow));
            if (charsToShow >= remaining.length()) {
                break;
            }
            remaining = remaining.substring(charsToShow);
        }
        
        return lines;
    }
    
    /**
     * 绘制排放类型框
     * 
     * @param g2d Graphics2D对象
     * @param emissionTypes 排放类型名称数组
     * @param emissionTypeCodes 排放类型代码数组
     * @param categoryToEmissionType 分类到排放类型的映射
     * @param categories 分类列表
     * @param width 图像宽度
     * @param height 图像高度
     * @return 每个排放类型的中心Y坐标映射
     */
    private static Map<Integer, Integer> drawEmissionTypeBoxes(Graphics2D g2d, 
                                                                String[] emissionTypes, 
                                                                int[] emissionTypeCodes, 
                                                                Map<String, Map<Integer, Double>> categoryToEmissionType, 
                                                                Set<String> categories, 
                                                                int width, 
                                                                int height) {
        int emissionTypeX = width - 85;
        int topY = 45;
        int bottomY = height - 15;
        int availableHeight = bottomY - topY;
        int boxSpacing = 3;
        int emissionTypeBoxWidth = 55;
        int emissionTypeBoxHeight = Math.max(25, 
                (availableHeight - (emissionTypes.length - 1) * boxSpacing) / emissionTypes.length);
        
        Map<Integer, Integer> emissionTypeYMap = new HashMap<>();
        int currentY = topY;
        Color[] typeColors = {new Color(155, 187, 89), new Color(79, 129, 189), new Color(192, 80, 77)};
        
        for (int i = 0; i < emissionTypes.length; i++) {
            int typeCode = emissionTypeCodes[i];
            String typeName = emissionTypes[i];
            int boxH = emissionTypeBoxHeight;
            
            g2d.setColor(typeColors[i]);
            g2d.fillRoundRect(emissionTypeX, currentY, emissionTypeBoxWidth, boxH, 3, 3);
            g2d.setColor(new Color(51, 51, 51));
            g2d.drawRoundRect(emissionTypeX, currentY, emissionTypeBoxWidth, boxH, 3, 3);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(CHART_TEXT_FONT);
            FontMetrics metrics = g2d.getFontMetrics(CHART_TEXT_FONT);
            int textWidth = metrics.stringWidth(typeName);
            int textY = currentY + boxH / 2 + metrics.getAscent() / 2 - 2;
            g2d.drawString(typeName, emissionTypeX + (emissionTypeBoxWidth - textWidth) / 2, textY);
            
            emissionTypeYMap.put(typeCode, currentY + boxH / 2);
            currentY += boxH + boxSpacing;
        }
        
        return emissionTypeYMap;
    }
    
    /**
     * 绘制根节点到分类的连接线
     * 
     * @param g2d Graphics2D对象
     * @param rootCenterY 根节点中心Y坐标
     * @param categoryYMap 分类中心Y坐标映射
     * @param width 图像宽度
     */
    private static void drawRootToCategoryLines(Graphics2D g2d, 
                                                 int rootCenterY, 
                                                 Map<String, Integer> categoryYMap, 
                                                 int width) {
        float uniformRootLineWidth = 2.0f;
        g2d.setStroke(new BasicStroke(uniformRootLineWidth));
        g2d.setColor(new Color(128, 100, 162, 180));
        
        int rootX = 5;
        int rootBoxWidth = 75;
        int rootEndX = rootX + rootBoxWidth;
        int categoryX = 105;
        int categoryStartX = categoryX;
        
        for (String category : categoryYMap.keySet()) {
            int categoryCenterY = categoryYMap.get(category);
            g2d.setStroke(new BasicStroke(uniformRootLineWidth));
            
            int controlX = (rootEndX + categoryStartX) / 2;
            int controlY = (rootCenterY + categoryCenterY) / 2;
            int verticalOffset = (categoryCenterY - rootCenterY) / 3;
            
            QuadCurve2D.Double curve = new QuadCurve2D.Double(
                    rootEndX, rootCenterY,
                    controlX, controlY + verticalOffset,
                    categoryStartX, categoryCenterY
            );
            g2d.draw(curve);
        }
    }
    
    /**
     * 标签信息类
     */
    private static class LabelInfo {
        String category;
        int typeCode;
        double amount;
        int labelX;
        int labelY;
        int textWidth;
        int textHeight;
        Color lineColor;
    }
    
    /**
     * 绘制分类到排放类型的连接线（包含标签，避免重叠）
     * 
     * @param g2d Graphics2D对象
     * @param categoryYMap 分类中心Y坐标映射
     * @param emissionTypeYMap 排放类型中心Y坐标映射
     * @param categoryToEmissionType 分类到排放类型的映射
     * @param emissionTypes 排放类型名称数组
     * @param emissionTypeCodes 排放类型代码数组
     * @param width 图像宽度
     */
    private static void drawCategoryToEmissionTypeLines(Graphics2D g2d, 
                                                          Map<String, Integer> categoryYMap, 
                                                          Map<Integer, Integer> emissionTypeYMap, 
                                                          Map<String, Map<Integer, Double>> categoryToEmissionType, 
                                                          String[] emissionTypes, 
                                                          int[] emissionTypeCodes, 
                                                          int width) {
        DecimalFormat df = new DecimalFormat("0.00");
        int categoryX = 105;
        int categoryBoxWidth = 50;
        int categoryEndX = categoryX + categoryBoxWidth;
        int emissionTypeX = width - 85;
        int emissionTypeStartX = emissionTypeX;
        float uniformLineWidth = 2.0f;
        
        // 收集所有标签信息
        List<LabelInfo> allLabels = new ArrayList<>();
        FontMetrics valueMetrics = g2d.getFontMetrics(CHART_TEXT_FONT);
        
        for (String category : categoryYMap.keySet()) {
            int categoryCenterY = categoryYMap.get(category);
            Map<Integer, Double> emissionMap = categoryToEmissionType.get(category);
            
            if (emissionMap != null) {
                for (int i = 0; i < emissionTypes.length; i++) {
                    int typeCode = emissionTypeCodes[i];
                    if (emissionMap.containsKey(typeCode)) {
                        double amount = emissionMap.get(typeCode);
                        if (amount > 0.01) {
                            int emissionCenterY = emissionTypeYMap.get(typeCode);
                            
                            int controlX = (categoryEndX + emissionTypeStartX) / 2;
                            int controlY = (categoryCenterY + emissionCenterY) / 2;
                            int verticalOffset = (emissionCenterY - categoryCenterY) / 3;
                            int midX = controlX;
                            int midY = controlY + verticalOffset;
                            
                            String valueText = df.format(amount) + "t";
                            int textWidth = valueMetrics.stringWidth(valueText);
                            int textHeight = valueMetrics.getHeight();
                            
                            Color[] typeColors = {new Color(155, 187, 89), new Color(79, 129, 189), new Color(192, 80, 77)};
                            Color lineColor = new Color(
                                    typeColors[i].getRed(),
                                    typeColors[i].getGreen(),
                                    typeColors[i].getBlue(),
                                    200
                            );
                            
                            LabelInfo labelInfo = new LabelInfo();
                            labelInfo.category = category;
                            labelInfo.typeCode = typeCode;
                            labelInfo.amount = amount;
                            labelInfo.labelX = midX;
                            labelInfo.labelY = midY;
                            labelInfo.textWidth = textWidth;
                            labelInfo.textHeight = textHeight;
                            labelInfo.lineColor = lineColor;
                            allLabels.add(labelInfo);
                        }
                    }
                }
            }
        }
        
        // 调整标签位置避免重叠
        adjustLabelPositions(allLabels, categoryYMap);
        
        // 绘制连接线和标签
        for (LabelInfo label : allLabels) {
            String category = label.category;
            int typeCode = label.typeCode;
            int categoryCenterY = categoryYMap.get(category);
            int emissionCenterY = emissionTypeYMap.get(typeCode);
            
            int typeIndex = -1;
            for (int i = 0; i < emissionTypeCodes.length; i++) {
                if (emissionTypeCodes[i] == typeCode) {
                    typeIndex = i;
                    break;
                }
            }
            
            if (typeIndex >= 0) {
                // 绘制连接线
                g2d.setColor(label.lineColor);
                g2d.setStroke(new BasicStroke(uniformLineWidth));
                
                int controlX = (categoryEndX + emissionTypeStartX) / 2;
                int controlY = (categoryCenterY + emissionCenterY) / 2;
                int verticalOffset = (emissionCenterY - categoryCenterY) / 3;
                
                QuadCurve2D.Double curve = new QuadCurve2D.Double(
                        categoryEndX, categoryCenterY,
                        controlX, controlY + verticalOffset,
                        emissionTypeStartX, emissionCenterY
                );
                g2d.draw(curve);
                
                // 绘制标签
                drawLabel(g2d, label, df);
            }
        }
    }
    
    /**
     * 调整标签位置避免重叠
     * 
     * @param allLabels 所有标签列表
     * @param categoryYMap 分类中心Y坐标映射
     */
    private static void adjustLabelPositions(List<LabelInfo> allLabels, Map<String, Integer> categoryYMap) {
        Map<Integer, List<LabelInfo>> labelsByType = new HashMap<>();
        for (LabelInfo label : allLabels) {
            labelsByType.computeIfAbsent(label.typeCode, k -> new ArrayList<>()).add(label);
        }
        
        for (Map.Entry<Integer, List<LabelInfo>> entry : labelsByType.entrySet()) {
            List<LabelInfo> typeLabels = entry.getValue();
            if (typeLabels.size() > 1) {
                typeLabels.sort((a, b) -> {
                    int yA = categoryYMap.get(a.category);
                    int yB = categoryYMap.get(b.category);
                    return Integer.compare(yA, yB);
                });
                
                int minSpacing = 25;
                for (int i = 0; i < typeLabels.size(); i++) {
                    LabelInfo current = typeLabels.get(i);
                    
                    if (i > 0) {
                        LabelInfo prev = typeLabels.get(i - 1);
                        int prevBottom = prev.labelY + prev.textHeight / 2;
                        int currentTop = current.labelY - current.textHeight / 2;
                        
                        if (currentTop < prevBottom + minSpacing) {
                            current.labelY = prevBottom + minSpacing + current.textHeight / 2;
                        }
                    }
                    
                    if (i < typeLabels.size() - 1) {
                        LabelInfo next = typeLabels.get(i + 1);
                        int currentBottom = current.labelY + current.textHeight / 2;
                        int nextTop = next.labelY - next.textHeight / 2;
                        
                        if (nextTop < currentBottom + minSpacing) {
                            next.labelY = currentBottom + minSpacing + next.textHeight / 2;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 绘制标签（背景+文字）
     * 
     * @param g2d Graphics2D对象
     * @param label 标签信息
     * @param df 数字格式化器
     */
    private static void drawLabel(Graphics2D g2d, LabelInfo label, DecimalFormat df) {
        int midX = label.labelX;
        int midY = label.labelY;
        
        // 绘制背景
        g2d.setColor(new Color(255, 255, 255, 220));
        String valueText = df.format(label.amount) + "t";
        int padding = 2;
        g2d.fillRoundRect(
                midX - label.textWidth / 2 - padding,
                midY - label.textHeight / 2 - padding,
                label.textWidth + padding * 2,
                label.textHeight + padding * 2,
                3, 3
        );
        
        // 绘制文字
        g2d.setColor(new Color(51, 51, 51));
        g2d.setFont(CHART_TEXT_FONT);
        FontMetrics valueMetrics = g2d.getFontMetrics(CHART_TEXT_FONT);
        int textX = midX - label.textWidth / 2;
        g2d.drawString(valueText, textX, midY + valueMetrics.getAscent() / 2 - 2);
    }
    
    /**
     * 将图像插入到Word文档
     * 
     * @param document Word文档对象
     * @param imageFile 图像文件
     * @param fileName 文件名
     * @param key 查找关键字
     * @param width 图像宽度
     * @param height 图像高度
     * @throws Exception 处理异常
     */
    private static void insertImageToWord(XWPFDocument document, File imageFile, 
                                           String fileName, String key, int width, int height) throws Exception {
        XWPFRun run = findTargetRun(document, key);
        if (run != null) {
            @SuppressWarnings("deprecation")
            XWPFParagraph paragraph = run.getParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            run.addPicture(Files.newInputStream(imageFile.toPath()),
                    XWPFDocument.PICTURE_TYPE_PNG, fileName, 
                    Units.toEMU(width), Units.toEMU(height));
        }
    }
    
    // ==================== 表1、表2、表3：表格填充 ====================
    
    /**
     * 填充表1：排放源消耗量表格
     * 使用Word格式属性设置单位文本的上标/下标，FOP在转换为PDF时能正确识别和渲染
     */
    public static void replaceForTable(List<EmissionAndConsume> emissionAndConsumes, XWPFTable table) {
        setTableWidthAndAlignment(table);
        for (int i = 0; i < emissionAndConsumes.size(); i++) {
            XWPFTableRow row = table.getRows().get(1);
            if (i != 0) {
                row = table.insertNewTableRow(i);
                row.createCell();
                row.createCell();
                row.createCell();
            }
            List<XWPFTableCell> tableCells = row.getTableCells();
            tableCells.get(0).setText(emissionAndConsumes.get(i).getObjectCategory());
            tableCells.get(1).setText(String.valueOf(emissionAndConsumes.get(i).getObjectConsumption()));
            
            // 使用Word格式属性设置单位文本（支持上标/下标）
            String unit = emissionAndConsumes.get(i).getUnit();
            setCellTextWithFormatting(tableCells.get(2), unit);
            
            setStyle(row);
        }
        setThreeLineTableStyle(table);
    }
    
    /**
     * 填充表2或表3：排放量表格（支持合并单元格和汇总）
     * 
     * @param rowIndex 起始行索引
     * @param emissionAndConsumes 排放源数据列表
     * @param table Word表格对象
     * @return 总排放量
     */
    public static Double replaceForTable(int rowIndex, List<EmissionAndConsume> emissionAndConsumes, XWPFTable table) {
        return replaceForTable(rowIndex, emissionAndConsumes, table, null);
    }
    
    /**
     * 填充表2或表3：排放量表格（支持合并单元格和汇总）
     * 
     * @param rowIndex 起始行索引
     * @param emissionAndConsumes 排放源数据列表
     * @param table Word表格对象
     * @param emissionType 排放类型（用于表3，设置第一列的文本，可为null）
     * @return 总排放量
     */
    public static Double replaceForTable(int rowIndex, List<EmissionAndConsume> emissionAndConsumes, XWPFTable table, String emissionType) {
        // 设置表格宽度和居中对齐
        setTableWidthAndAlignment(table);
        
        BigDecimal bigDecimal = new BigDecimal("0.0");
        
        // 填充数据行
        for (int i = 0; i < emissionAndConsumes.size(); i++) {
            XWPFTableRow row;
            
            // 如果模板中的行数足够，直接使用现有行
            if (rowIndex + i < table.getRows().size()) {
                row = table.getRows().get(rowIndex + i);
            } else {
                // 如果行数不足，需要插入新行
                // 注意：插入位置应该在数据行区域，而不是在小计行之后
                row = table.insertNewTableRow(rowIndex + i);
                row.createCell();
                row.createCell();
                row.createCell();
            }
            
            List<XWPFTableCell> tableCells = row.getTableCells();
            
            // 确保行有足够的单元格
            while (tableCells.size() < 3) {
                row.createCell();
                tableCells = row.getTableCells();
            }
            
            // 第一列：排放类型（只在第一行设置，后续行通过合并单元格显示）
            if (i == 0 && emissionType != null && !emissionType.isEmpty()) {
                tableCells.get(0).setText(emissionType);
            }
            // 第二列：名称
            tableCells.get(1).setText(emissionAndConsumes.get(i).getObjectCategory());
            // 第三列：排放数值
            tableCells.get(2).setText(String.valueOf(emissionAndConsumes.get(i).getEmissionAmount()));
            BigDecimal bigDecimalChild = new BigDecimal(emissionAndConsumes.get(i).getEmissionAmount() + "");
            bigDecimal = bigDecimal.add(bigDecimalChild);
            setStyle(row);
        }
        
        // 设置小计行的值（小计行应该在rowIndex + emissionAndConsumes.size()位置）
        String dirEmiNumberStr = String.valueOf(bigDecimal.doubleValue());
        int summaryRowIndex = rowIndex + emissionAndConsumes.size();
        
        // 确保小计行存在
        if (summaryRowIndex >= table.getRows().size()) {
            // 如果小计行不存在，插入一行作为小计行
            XWPFTableRow summaryRow = table.insertNewTableRow(summaryRowIndex);
            summaryRow.createCell();
            summaryRow.createCell();
            summaryRow.createCell();
        }
        
        XWPFTableRow row = table.getRow(summaryRowIndex);
        List<XWPFTableCell> cells = row.getTableCells();
        // 确保有足够的单元格
        while (cells.size() < 3) {
            row.createCell();
            cells = row.getTableCells();
        }
        // 小计行：只设置第三列的数值，不修改第二列（保持模板原有内容）
        XWPFTableCell cell = cells.get(2);
        cell.setText(dirEmiNumberStr);
        setStyle(row);
        
        // 合并第一列：从rowIndex到rowIndex + emissionAndConsumes.size()（包括小计行）
        // 重要：小计行也应该包含在合并范围内，因为小计行也是这个排放类型的一部分
        // mergeCellsVertically的toRow参数是包含的（<= toRow），所以传入rowIndex + emissionAndConsumes.size()
        // 这样会合并从rowIndex到rowIndex + emissionAndConsumes.size()的所有行（包括小计行）
        mergeCellsVertically(table, 0, rowIndex, rowIndex + emissionAndConsumes.size());
        
        // 注意：不在这里设置三线表样式，因为表3有多个分组，需要在所有分组填充完成后统一设置
        // 表2会在调用处单独设置三线表样式
        
        return bigDecimal.doubleValue();
    }
    
    /**
     * 设置表格宽度和居中对齐
     * 
     * @param table Word表格对象
     */
    private static void setTableWidthAndAlignment(XWPFTable table) {
        try {
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = table.getCTTbl().addNewTblPr();
            }
            
            // 设置表格宽度为页面宽度的90%（A4纸宽度约21cm，约8000单位）
            int tableWidth = 8000; // 约21cm
            // 使用POI的setWidth方法设置宽度
            table.setWidth(tableWidth + "");
            
            // 设置表格宽度类型为DXA（twips）
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth tblWidth = tblPr.getTblW();
            if (tblWidth == null) {
                tblWidth = tblPr.addNewTblW();
            }
            tblWidth.setW(BigInteger.valueOf(tableWidth));
            tblWidth.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
            
            // 设置表格在页面中居中
            // 注意：在POI 5.1.0中，CTTblPr.getJc()和addNewJc()返回CTJcTable类型（使用STJcTable枚举）
            // 表格对齐使用CTJcTable和STJcTable，段落对齐使用CTJc和STJc
            CTJcTable jc = tblPr.getJc();
            if (jc == null) {
                // 如果不存在，创建新的CTJcTable并设置居中
                jc = tblPr.addNewJc();
            }
            jc.setVal(STJcTable.CENTER);
        } catch (Exception e) {
            log.warn("设置表格宽度和对齐失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 设置表格为三线表样式
     * 三线表特点：顶部粗线、表头下方细线、底部粗线，其他边框无
     * 只设置表格级别的边框，不设置单元格边框，避免黑色背景问题
     * 
     * @param table Word表格对象
     */
    public static void setThreeLineTableStyle(XWPFTable table) {
        try {
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = table.getCTTbl().addNewTblPr();
            }
            
            // 获取或创建表格边框设置
            CTTblBorders tblBorders = tblPr.getTblBorders();
            if (tblBorders == null) {
                tblBorders = tblPr.addNewTblBorders();
            }
            
            // 设置顶部边框：粗线（15 twips）
            if (tblBorders.getTop() == null) {
                tblBorders.addNewTop();
            }
            tblBorders.getTop().setVal(STBorder.SINGLE);
            tblBorders.getTop().setSz(BigInteger.valueOf(15));
            tblBorders.getTop().setColor("000000");
            
            // 设置底部边框：粗线（15 twips）
            if (tblBorders.getBottom() == null) {
                tblBorders.addNewBottom();
            }
            tblBorders.getBottom().setVal(STBorder.SINGLE);
            tblBorders.getBottom().setSz(BigInteger.valueOf(15));
            tblBorders.getBottom().setColor("000000");
            
            // 设置内部水平边框：细线（10 twips），只用于表头下方
            // 注意：InsideH 会影响所有内部水平线，但我们只希望表头下方有细线
            // 所以先设置为细线，然后在单元格级别覆盖中间行的边框
            if (tblBorders.getInsideH() == null) {
                tblBorders.addNewInsideH();
            }
            tblBorders.getInsideH().setVal(STBorder.SINGLE);
            tblBorders.getInsideH().setSz(BigInteger.valueOf(10));
            tblBorders.getInsideH().setColor("000000");
            
            // 去掉左右边框
            if (tblBorders.getLeft() == null) {
                tblBorders.addNewLeft();
            }
            tblBorders.getLeft().setVal(STBorder.NIL);
            
            if (tblBorders.getRight() == null) {
                tblBorders.addNewRight();
            }
            tblBorders.getRight().setVal(STBorder.NIL);
            
            // 去掉内部垂直边框
            if (tblBorders.getInsideV() == null) {
                tblBorders.addNewInsideV();
            }
            tblBorders.getInsideV().setVal(STBorder.NIL);
            
            // 遍历所有行，只设置必要的单元格边框，避免黑色背景
            List<XWPFTableRow> rows = table.getRows();
            if (rows.size() < 2) {
                return; // 至少需要表头和数据行
            }
            
            // 第一行（表头）：确保顶部粗线，底部细线
            for (XWPFTableCell cell : rows.get(0).getTableCells()) {
                CTTcPr tcPr = cell.getCTTc().getTcPr();
                if (tcPr == null) {
                    tcPr = cell.getCTTc().addNewTcPr();
                }
                
                CTTcBorders tcBorders = tcPr.getTcBorders();
                if (tcBorders == null) {
                    tcBorders = tcPr.addNewTcBorders();
                }
                
                // 顶部边框：粗线（15 twips）
                if (tcBorders.getTop() == null) {
                    tcBorders.addNewTop();
                }
                tcBorders.getTop().setVal(STBorder.SINGLE);
                tcBorders.getTop().setSz(BigInteger.valueOf(15));
                tcBorders.getTop().setColor("000000");
                
                // 底部边框：细线（10 twips，表头下方）
                if (tcBorders.getBottom() == null) {
                    tcBorders.addNewBottom();
                }
                tcBorders.getBottom().setVal(STBorder.SINGLE);
                tcBorders.getBottom().setSz(BigInteger.valueOf(10));
                tcBorders.getBottom().setColor("000000");
                
                // 左右边框：无
                if (tcBorders.getLeft() == null) {
                    tcBorders.addNewLeft();
                }
                tcBorders.getLeft().setVal(STBorder.NIL);
                
                if (tcBorders.getRight() == null) {
                    tcBorders.addNewRight();
                }
                tcBorders.getRight().setVal(STBorder.NIL);
            }
            
            // 最后一行：确保底部粗线
            int lastRowIndex = rows.size() - 1;
            for (XWPFTableCell cell : rows.get(lastRowIndex).getTableCells()) {
                CTTcPr tcPr = cell.getCTTc().getTcPr();
                if (tcPr == null) {
                    tcPr = cell.getCTTc().addNewTcPr();
                }
                
                CTTcBorders tcBorders = tcPr.getTcBorders();
                if (tcBorders == null) {
                    tcBorders = tcPr.addNewTcBorders();
                }
                
                // 底部边框：粗线（15 twips）
                if (tcBorders.getBottom() == null) {
                    tcBorders.addNewBottom();
                }
                tcBorders.getBottom().setVal(STBorder.SINGLE);
                tcBorders.getBottom().setSz(BigInteger.valueOf(15));
                tcBorders.getBottom().setColor("000000");
                
                // 顶部边框：无（让表格级别的InsideH控制，但我们需要去掉它）
                if (tcBorders.getTop() == null) {
                    tcBorders.addNewTop();
                }
                tcBorders.getTop().setVal(STBorder.NIL);
                
                // 左右边框：无
                if (tcBorders.getLeft() == null) {
                    tcBorders.addNewLeft();
                }
                tcBorders.getLeft().setVal(STBorder.NIL);
                
                if (tcBorders.getRight() == null) {
                    tcBorders.addNewRight();
                }
                tcBorders.getRight().setVal(STBorder.NIL);
            }
            
            // 中间行：去掉所有边框，避免显示内部水平线
            for (int i = 1; i < lastRowIndex; i++) {
                for (XWPFTableCell cell : rows.get(i).getTableCells()) {
                    CTTcPr tcPr = cell.getCTTc().getTcPr();
                    if (tcPr == null) {
                        tcPr = cell.getCTTc().addNewTcPr();
                    }
                    
                    CTTcBorders tcBorders = tcPr.getTcBorders();
                    if (tcBorders == null) {
                        tcBorders = tcPr.addNewTcBorders();
                    }
                    
                    // 所有边框都设为无
                    if (tcBorders.getTop() == null) {
                        tcBorders.addNewTop();
                    }
                    tcBorders.getTop().setVal(STBorder.NIL);
                    
                    if (tcBorders.getBottom() == null) {
                        tcBorders.addNewBottom();
                    }
                    tcBorders.getBottom().setVal(STBorder.NIL);
                    
                    if (tcBorders.getLeft() == null) {
                        tcBorders.addNewLeft();
                    }
                    tcBorders.getLeft().setVal(STBorder.NIL);
                    
                    if (tcBorders.getRight() == null) {
                        tcBorders.addNewRight();
                    }
                    tcBorders.getRight().setVal(STBorder.NIL);
                }
            }
        } catch (Exception e) {
            log.warn("设置三线表样式失败: {}", e.getMessage());
        }
    }
    
    /**
     * 设置表3为三线表样式（存在行合并情况）
     * 表3特点：第一列是合并单元格（排放类型）
     * 三线表样式：顶部粗线、表头下方细线、底部粗线
     * 注意：中间行不添加任何横线，保持模板原有样式
     * 
     * @param table Word表格对象
     */
    public static void setThreeLineTableStyleForTable3(XWPFTable table) {
        try {
            List<XWPFTableRow> rows = table.getRows();
            if (rows.size() < 2) {
                log.warn("表3行数不足，无法设置三线表样式");
                return; // 至少需要表头和数据行
            }
            
            // 设置表格级别的边框
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = table.getCTTbl().addNewTblPr();
            }
            
            CTTblBorders tblBorders = tblPr.getTblBorders();
            if (tblBorders == null) {
                tblBorders = tblPr.addNewTblBorders();
            }
            
            // 设置顶部边框：粗线（15 twips）
            if (tblBorders.getTop() == null) {
                tblBorders.addNewTop();
            }
            tblBorders.getTop().setVal(STBorder.SINGLE);
            tblBorders.getTop().setSz(BigInteger.valueOf(15));
            tblBorders.getTop().setColor("000000");
            
            // 设置底部边框：粗线（15 twips）
            if (tblBorders.getBottom() == null) {
                tblBorders.addNewBottom();
            }
            tblBorders.getBottom().setVal(STBorder.SINGLE);
            tblBorders.getBottom().setSz(BigInteger.valueOf(15));
            tblBorders.getBottom().setColor("000000");
            
            // 去掉内部水平边框（中间行不添加横线）
            if (tblBorders.getInsideH() == null) {
                tblBorders.addNewInsideH();
            }
            tblBorders.getInsideH().setVal(STBorder.NIL);
            
            // 去掉左右边框
            if (tblBorders.getLeft() == null) {
                tblBorders.addNewLeft();
            }
            tblBorders.getLeft().setVal(STBorder.NIL);
            
            if (tblBorders.getRight() == null) {
                tblBorders.addNewRight();
            }
            tblBorders.getRight().setVal(STBorder.NIL);
            
            // 去掉内部垂直边框
            if (tblBorders.getInsideV() == null) {
                tblBorders.addNewInsideV();
            }
            tblBorders.getInsideV().setVal(STBorder.NIL);
            
            // 第一行（表头）：顶部粗线，底部细线
            List<XWPFTableCell> headerCells = rows.get(0).getTableCells();
	        for (XWPFTableCell cell : headerCells) {
		        CTTcPr tcPr = cell.getCTTc().getTcPr();
		        if (tcPr == null) {
			        tcPr = cell.getCTTc().addNewTcPr();
		        }

		        CTTcBorders tcBorders = tcPr.getTcBorders();
		        if (tcBorders == null) {
			        tcBorders = tcPr.addNewTcBorders();
		        }

		        // 顶部边框：粗线（15 twips）
		        if (tcBorders.getTop() == null) {
			        tcBorders.addNewTop();
		        }
		        tcBorders.getTop().setVal(STBorder.SINGLE);
		        tcBorders.getTop().setSz(BigInteger.valueOf(15));
		        tcBorders.getTop().setColor("000000");

		        // 底部边框：细线（10 twips，表头下方）
		        if (tcBorders.getBottom() == null) {
			        tcBorders.addNewBottom();
		        }
		        tcBorders.getBottom().setVal(STBorder.SINGLE);
		        tcBorders.getBottom().setSz(BigInteger.valueOf(10));
		        tcBorders.getBottom().setColor("000000");

		        // 左右边框：无
		        if (tcBorders.getLeft() == null) {
			        tcBorders.addNewLeft();
		        }
		        tcBorders.getLeft().setVal(STBorder.NIL);

		        if (tcBorders.getRight() == null) {
			        tcBorders.addNewRight();
		        }
		        tcBorders.getRight().setVal(STBorder.NIL);
	        }

            // 最后一行：底部粗线
            // 找到真正的最后一行（第一列有内容的行）
            // 如果最后一行第一列为空，则向上查找，找到第一列有内容的行
            int lastRowIndex = rows.size() - 1;
            int actualLastRowIndex = lastRowIndex;
            
            // 从最后一行向上查找，找到第一列有内容的行
            for (int i = lastRowIndex; i >= 1; i--) { // 从最后一行开始，跳过表头（索引0）
                List<XWPFTableCell> rowCells = rows.get(i).getTableCells();
                if (!rowCells.isEmpty()) {
                    XWPFTableCell firstCell = rowCells.get(0);
                    String firstCellText = firstCell.getText().trim();
                    if (!firstCellText.isEmpty()) {
                        actualLastRowIndex = i;
                        break;
                    }
                }
            }
            
            List<XWPFTableCell> lastRowCells = rows.get(actualLastRowIndex).getTableCells();
            // 检查最后一行第一列的状态
            boolean isFirstCellEmpty = false;
            String firstCellMergeStatus = "NORMAL";
            if (!lastRowCells.isEmpty()) {
                XWPFTableCell firstCell = lastRowCells.get(0);
                String firstCellText = firstCell.getText().trim();
                isFirstCellEmpty = firstCellText.isEmpty();
                
                CTTcPr firstTcPr = firstCell.getCTTc().getTcPr();
                if (firstTcPr != null && firstTcPr.getVMerge() != null) {
                    if (firstTcPr.getVMerge().getVal() == STMerge.RESTART) {
                        firstCellMergeStatus = "RESTART";
                    } else if (firstTcPr.getVMerge().getVal() == STMerge.CONTINUE) {
                        firstCellMergeStatus = "CONTINUE";
                    }
                }
            }
            
            for (int colIndex = 0; colIndex < lastRowCells.size(); colIndex++) {
                XWPFTableCell cell = lastRowCells.get(colIndex);
                CTTcPr tcPr = cell.getCTTc().getTcPr();
                if (tcPr == null) {
                    tcPr = cell.getCTTc().addNewTcPr();
                }
                
                CTTcBorders tcBorders = tcPr.getTcBorders();
                if (tcBorders == null) {
                    tcBorders = tcPr.addNewTcBorders();
                }
                
                // 特殊处理：如果第一列为空且是NORMAL状态，可能需要特殊处理
                if (colIndex == 0 && isFirstCellEmpty && "NORMAL".equals(firstCellMergeStatus)) {
                    // 第一列为空且是NORMAL状态：只设置底部边框，其他边框保持NIL
                    if (tcBorders.getBottom() == null) {
                        tcBorders.addNewBottom();
                    }
                    tcBorders.getBottom().setVal(STBorder.SINGLE);
                    tcBorders.getBottom().setSz(BigInteger.valueOf(15));
                    tcBorders.getBottom().setColor("000000");
                    
                    // 顶部边框：无
                    if (tcBorders.getTop() == null) {
                        tcBorders.addNewTop();
                    }
                    tcBorders.getTop().setVal(STBorder.NIL);
                    
                    // 左右边框：无
                    if (tcBorders.getLeft() == null) {
                        tcBorders.addNewLeft();
                    }
                    tcBorders.getLeft().setVal(STBorder.NIL);
                    
                    if (tcBorders.getRight() == null) {
                        tcBorders.addNewRight();
                    }
                    tcBorders.getRight().setVal(STBorder.NIL);
                } else {
                    // 其他列或非空第一列：正常设置底部边框
                    // 底部边框：粗线（15 twips）
                    if (tcBorders.getBottom() == null) {
                        tcBorders.addNewBottom();
                    }
                    tcBorders.getBottom().setVal(STBorder.SINGLE);
                    tcBorders.getBottom().setSz(BigInteger.valueOf(15));
                    tcBorders.getBottom().setColor("000000");
                    
                    // 顶部边框：无（保持模板原有样式）
                    if (tcBorders.getTop() == null) {
                        tcBorders.addNewTop();
                    }
                    tcBorders.getTop().setVal(STBorder.NIL);
                    
                    // 左右边框：无
                    if (tcBorders.getLeft() == null) {
                        tcBorders.addNewLeft();
                    }
                    tcBorders.getLeft().setVal(STBorder.NIL);
                    
                    if (tcBorders.getRight() == null) {
                        tcBorders.addNewRight();
                    }
                    tcBorders.getRight().setVal(STBorder.NIL);
                }
            }
        } catch (Exception e) {
            log.error("设置表3简化三线表样式失败", e);
        }
    }
    
    // ==================== 图3：各排放源二氧化碳排放量（柱状图） ====================
    
    /**
     * 创建图3：各排放源二氧化碳排放量（柱状图）
     * 
     * @param document Word文档对象
     * @param carbonEmissions 排放数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createBarChartToWord(XWPFDocument document, 
                                             List<EmissionAndConsume> carbonEmissions, 
                                             int year) throws Exception {
        // 确保字体已加载（处理Docker volume挂载延迟的情况）
        ensureFontLoaded();
        
        String foldPath = getTempChartDirectory();
        String fileNameBar = "chartBar" + year + ".png";
        deleteOldChartFile(foldPath + fileNameBar);
        
        // 创建数据集
        DefaultCategoryDataset dataset = createBarChartDataset(carbonEmissions);
        
        // 创建图表
        JFreeChart chartBar = createBarChart(dataset, year);
        
        // 保存并插入Word
        File barFile = new File(foldPath + fileNameBar);
        try {
            int barWidth = 450;
            int barHeight = 240;
            int scale = 2;
            ChartUtils.saveChartAsPNG(barFile, chartBar, barWidth * scale, barHeight * scale);
            insertChartToWord(document, barFile, fileNameBar, "bar", barWidth, barHeight);
        } finally {
            deleteOldChartFile(foldPath + fileNameBar);
        }
    }
    
    /**
     * 创建柱状图数据集
     * 
     * @param carbonEmissions 排放数据列表
     * @return 数据集对象
     */
    private static DefaultCategoryDataset createBarChartDataset(List<EmissionAndConsume> carbonEmissions) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String seriesName = getChartSeriesNameCO2Emission();
        for (EmissionAndConsume eac : carbonEmissions) {
            double emissionAmountT = (eac.getEmissionAmount() != null ? eac.getEmissionAmount() : 0.0) / 1000.0;
            dataset.addValue(emissionAmountT, seriesName, eac.getObjectCategory());
        }
        return dataset;
    }
    
    /**
     * 创建柱状图
     * 
     * @param dataset 数据集
     * @param year 年份
     * @return 图表对象
     */
    private static JFreeChart createBarChart(DefaultCategoryDataset dataset, int year) {
        // 设置主题
        StandardChartTheme sct = createChartTheme3456();
        ChartFactory.setChartTheme(sct);
        
        JFreeChart chartBar = ChartFactory.createBarChart(
                year + "各排放源二氧化碳排放量",
                "碳排放种类",
                "二氧化碳排放量(t)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        chartBar.setTextAntiAlias(true);
        chartBar.setBackgroundPaint(Color.WHITE);
        chartBar.setAntiAlias(true);
        
        // 美化柱状图
        CategoryPlot barPlot = chartBar.getCategoryPlot();
        barPlot.setBackgroundPaint(Color.WHITE);
        barPlot.setRangeGridlinePaint(new Color(230, 230, 230));
        barPlot.setDomainGridlinePaint(new Color(230, 230, 230));
        
        // 设置X轴
        CategoryAxis barAxis = barPlot.getDomainAxis();
        barAxis.setLabelFont(getChart3456AxisLabelFont());
        barAxis.setTickLabelFont(getChart3456AxisTickFont());
        barAxis.setMaximumCategoryLabelLines(2);
        barAxis.setCategoryMargin(0.15);
        
        // 设置柱状图颜色
        BarRenderer barRenderer = (BarRenderer) barPlot.getRenderer();
        barRenderer.setBarPainter(new StandardBarPainter());
        Paint[] colors = new Paint[]{
            new Color(79, 129, 189), new Color(192, 80, 77), new Color(155, 187, 89),
            new Color(128, 100, 162), new Color(247, 150, 70), new Color(75, 172, 198),
            new Color(255, 192, 0), new Color(112, 173, 71)
        };
        for (int i = 0; i < Math.min(barPlot.getDataset().getRowCount(), colors.length); i++) {
            barRenderer.setSeriesPaint(i, colors[i % colors.length]);
        }
        
        // 设置图例
        chartBar.getLegend().setFrame(new BlockBorder(Color.WHITE));
        chartBar.getLegend().setPosition(RectangleEdge.RIGHT);
        chartBar.getLegend().setItemFont(getChart3456LegendFont());
        
        return chartBar;
    }
    
    /**
     * 创建图3、图4、图5、图6的图表主题
     * 
     * @return 图表主题对象
     */
    private static StandardChartTheme createChartTheme3456() {
        StandardChartTheme sct = new StandardChartTheme("美化主题");
        sct.setExtraLargeFont(getChart3456TitleFont());
        sct.setLargeFont(getChart3456TextFont());
        sct.setRegularFont(getChart3456TextFont());
        sct.setChartBackgroundPaint(Color.WHITE);
        sct.setPlotBackgroundPaint(Color.WHITE);
        sct.setDomainGridlinePaint(new Color(230, 230, 230));
        sct.setRangeGridlinePaint(new Color(230, 230, 230));
        sct.setTitlePaint(new Color(51, 51, 51));
        sct.setTickLabelPaint(new Color(102, 102, 102));
        return sct;
    }
    
    // ==================== 图4：二氧化碳排放构成（饼图） ====================
    
    /**
     * 创建图4：二氧化碳排放构成（饼图）
     * 
     * @param document Word文档对象
     * @param carbonEmissions 排放数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createPieChartToWord(XWPFDocument document, 
                                              List<EmissionAndConsume> carbonEmissions, 
                                              int year) throws Exception {
        // 确保字体已加载（处理Docker volume挂载延迟的情况）
        ensureFontLoaded();
        
        String foldPath = getTempChartDirectory();
        String fileNamePie = "chartPie" + year + ".png";
        deleteOldChartFile(foldPath + fileNamePie);
        
        // 创建数据集
        DefaultPieDataset<String> pieDataset = createPieChartDataset(carbonEmissions);
        
        // 创建图表
        JFreeChart chartPie = createPieChart(pieDataset, carbonEmissions, year);
        
        // 保存并插入Word
        File pieFile = new File(foldPath + fileNamePie);
        try {
            int pieWidth = 470;
            int pieHeight = 340;
            int scale = 2;
            ChartUtils.saveChartAsPNG(pieFile, chartPie, pieWidth * scale, pieHeight * scale);
            insertChartToWord(document, pieFile, fileNamePie, "pie", pieWidth, pieHeight);
        } finally {
            deleteOldChartFile(foldPath + fileNamePie);
        }
    }
    
    /**
     * 创建饼图数据集
     * 
     * @param carbonEmissions 排放数据列表
     * @return 数据集对象
     */
    private static DefaultPieDataset<String> createPieChartDataset(List<EmissionAndConsume> carbonEmissions) {
        DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
        for (EmissionAndConsume eac : carbonEmissions) {
            double emissionAmountT = (eac.getEmissionAmount() != null ? eac.getEmissionAmount() : 0.0) / 1000.0;
            pieDataset.setValue(eac.getObjectCategory(), emissionAmountT);
        }
        return pieDataset;
    }
    
    /**
     * 创建饼图
     * 
     * @param pieDataset 数据集
     * @param carbonEmissions 排放数据列表（用于设置颜色）
     * @param year 年份
     * @return 图表对象
     */
    private static JFreeChart createPieChart(DefaultPieDataset<String> pieDataset, 
                                              List<EmissionAndConsume> carbonEmissions, 
                                              int year) {
        // 设置主题
        StandardChartTheme sct = createChartTheme3456();
        ChartFactory.setChartTheme(sct);
        
        JFreeChart chartPie = ChartFactory.createPieChart(
                year + "年二氧化碳排放构成",
                pieDataset,
                true,
                true,
                false
        );
        
        chartPie.setTextAntiAlias(true);
        chartPie.setBackgroundPaint(Color.WHITE);
        chartPie.setAntiAlias(true);
        
        // 美化饼图
        @SuppressWarnings("unchecked")
        PiePlot<String> piePlot = (PiePlot<String>) chartPie.getPlot();
        piePlot.setBackgroundPaint(Color.WHITE);
        piePlot.setOutlineVisible(false); // 去掉饼图边框
        piePlot.setInteriorGap(0.20);
        
        // 设置饼图颜色
        Paint[] colors = new Paint[]{
            new Color(79, 129, 189), new Color(192, 80, 77), new Color(155, 187, 89),
            new Color(128, 100, 162), new Color(247, 150, 70), new Color(75, 172, 198),
            new Color(255, 192, 0), new Color(112, 173, 71)
        };
        for (int i = 0; i < carbonEmissions.size() && i < colors.length; i++) {
            piePlot.setSectionPaint(carbonEmissions.get(i).getObjectCategory(), colors[i % colors.length]);
        }
        
        // 设置图例
        chartPie.getLegend().setFrame(new BlockBorder(Color.WHITE));
        chartPie.getLegend().setPosition(RectangleEdge.RIGHT);
        chartPie.getLegend().setItemFont(getChart3456LegendFont());
        
        // 调整标题位置
        if (chartPie.getTitle() != null && chartPie.getTitle() instanceof TextTitle) {
            TextTitle title = (TextTitle) chartPie.getTitle();
            title.setPadding(new RectangleInsets(0, -70, 0, 0));
            title.setFont(getChart3456TitleFont());
        }
        
        // 减少图表整体的下方空白：设置图表的padding
        chartPie.setPadding(new RectangleInsets(5, 5, -70, 5)); // 上、左、下、右，减少底部边距
        
        return chartPie;
    }
    
    // ==================== 图3和图4的合并方法（保持向后兼容） ====================
    
    /**
     * 创建图3和图4：柱状图和饼图（合并方法，保持向后兼容）
     * 
     * @param document Word文档对象
     * @param carbonEmissions 排放数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createBarAndPieToWord(XWPFDocument document, 
                                               List<EmissionAndConsume> carbonEmissions, 
                                               int year) throws Exception {
        createBarChartToWord(document, carbonEmissions, year);
        createPieChartToWord(document, carbonEmissions, year);
    }
    
    // ==================== 图5：碳排放变化趋势（按排放源，堆叠柱状图） ====================
    
    /**
     * 创建图5：碳排放变化趋势（按排放源，堆叠柱状图）
     * 
     * @param document Word文档对象
     * @param carbonEmissions 排放数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createStackBarChartByCategoryToWord(XWPFDocument document, 
                                                            List<CarbonEmission> carbonEmissions, 
                                                            int year) throws Exception {
        // 确保字体已加载（处理Docker volume挂载延迟的情况）
        ensureFontLoaded();
        
        String foldPath = getTempChartDirectory();
        String fileNameStackBarCategory = "chartStackBarCategory" + year + ".png";
        deleteOldChartFile(foldPath + fileNameStackBarCategory);
        
        // 创建数据集
        DefaultCategoryDataset datasetCategory = createStackBarCategoryDataset(carbonEmissions);
        
        // 创建图表
        JFreeChart chartStackBarCategory = createStackBarChartByCategory(datasetCategory, year);
        
        // 保存并插入Word
        File categoryFile = new File(foldPath + fileNameStackBarCategory);
        try {
            int stackBarWidth = 400;
            int stackBarHeight = 270;
            int scale = 2;
            ChartUtils.saveChartAsPNG(categoryFile, chartStackBarCategory, 
                    stackBarWidth * scale, stackBarHeight * scale);
            insertChartToWord(document, categoryFile, fileNameStackBarCategory, 
                    "stackBarCategory", stackBarWidth, stackBarHeight);
        } finally {
            // 使用强制删除，因为文件可能被Word文档引用
            forceDeleteChartFile(foldPath + fileNameStackBarCategory);
        }
    }
    
    /**
     * 创建按排放源的堆叠柱状图数据集
     * 
     * @param carbonEmissions 排放数据列表
     * @return 数据集对象
     */
    private static DefaultCategoryDataset createStackBarCategoryDataset(List<CarbonEmission> carbonEmissions) {
        DefaultCategoryDataset datasetCategory = new DefaultCategoryDataset();
        for (CarbonEmission carbonEmission : carbonEmissions) {
            double amountT = (carbonEmission.getAmount() != null ? carbonEmission.getAmount() : 0.0) / 1000.0;
            datasetCategory.addValue(amountT, carbonEmission.getCategory(), carbonEmission.getMonth() + "月");
        }
        return datasetCategory;
    }
    
    /**
     * 创建按排放源的堆叠柱状图
     * 
     * @param datasetCategory 数据集
     * @param year 年份
     * @return 图表对象
     */
    private static JFreeChart createStackBarChartByCategory(DefaultCategoryDataset datasetCategory, int year) {
        StandardChartTheme sct = createChartTheme3456();
        ChartFactory.setChartTheme(sct);
        
        JFreeChart chartStackBarCategory = ChartFactory.createStackedBarChart(
                year + "碳排放变化趋势(按排放源)",
                "时间(月)",
                "碳排放量(t)",
                datasetCategory,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        chartStackBarCategory.setTextAntiAlias(true);
        chartStackBarCategory.setBackgroundPaint(Color.WHITE);
        chartStackBarCategory.setAntiAlias(true);
        
        CategoryPlot categoryPlot = chartStackBarCategory.getCategoryPlot();
        categoryPlot.setBackgroundPaint(Color.WHITE);
        categoryPlot.setRangeGridlinePaint(new Color(230, 230, 230));
        categoryPlot.setDomainGridlinePaint(new Color(230, 230, 230));
        
        CategoryAxis categoryAxis = categoryPlot.getDomainAxis();
        categoryAxis.setLabelFont(getChart3456AxisLabelFont());
        categoryAxis.setTickLabelFont(getChart3456AxisTickFont());
        categoryAxis.setMaximumCategoryLabelLines(2);
        categoryAxis.setCategoryMargin(0.1);
        
        chartStackBarCategory.getLegend().setFrame(new BlockBorder(Color.WHITE));
        chartStackBarCategory.getLegend().setItemFont(getChart3456LegendFont());
        
        return chartStackBarCategory;
    }
    
    // ==================== 图6：碳排放变化趋势（按排放类型，堆叠柱状图） ====================
    
    /**
     * 创建图6：碳排放变化趋势（按排放类型，堆叠柱状图）
     * 
     * @param document Word文档对象
     * @param carbonEmissions 排放数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createStackBarChartByTypeToWord(XWPFDocument document, 
                                                         List<CarbonEmission> carbonEmissions, 
                                                         int year) throws Exception {
        // 确保字体已加载（处理Docker volume挂载延迟的情况）
        ensureFontLoaded();
        
        String foldPath = getTempChartDirectory();
        String fileNameStackBarType = "chartStackBarType" + year + ".png";
        deleteOldChartFile(foldPath + fileNameStackBarType);
        
        // 创建数据集
        DefaultCategoryDataset datasetType = createStackBarTypeDataset(carbonEmissions);
        
        // 创建图表
        JFreeChart chartStackBarType = createStackBarChartByType(datasetType, year);
        
        // 保存并插入Word
        File typeFile = new File(foldPath + fileNameStackBarType);
        try {
            int stackBarWidth = 400;
            int stackBarHeight = 270;
            int scale = 2;
            ChartUtils.saveChartAsPNG(typeFile, chartStackBarType, 
                    stackBarWidth * scale, stackBarHeight * scale);
            insertChartToWord(document, typeFile, fileNameStackBarType, 
                    "stackBarType", stackBarWidth, stackBarHeight);
        } finally {
            // 使用强制删除，因为文件可能被Word文档引用
            forceDeleteChartFile(foldPath + fileNameStackBarType);
        }
    }
    
    /**
     * 创建按排放类型的堆叠柱状图数据集
     * 
     * @param carbonEmissions 排放数据列表
     * @return 数据集对象
     */
    private static DefaultCategoryDataset createStackBarTypeDataset(List<CarbonEmission> carbonEmissions) {
        DefaultCategoryDataset datasetType = new DefaultCategoryDataset();
        for (CarbonEmission carbonEmission : carbonEmissions) {
            double amountT = (carbonEmission.getAmount() != null ? carbonEmission.getAmount() : 0.0) / 1000.0;
            switch (carbonEmission.getEmissionType()) {
                case 0:
                    datasetType.addValue(amountT, "直接排放", carbonEmission.getMonth() + "月");
                    break;
                case 1:
                    datasetType.addValue(amountT, "间接排放", carbonEmission.getMonth() + "月");
                    break;
                case 2:
                    datasetType.addValue(amountT, "其它排放", carbonEmission.getMonth() + "月");
                    break;
            }
        }
        return datasetType;
    }
    
    /**
     * 创建按排放类型的堆叠柱状图
     * 
     * @param datasetType 数据集
     * @param year 年份
     * @return 图表对象
     */
    private static JFreeChart createStackBarChartByType(DefaultCategoryDataset datasetType, int year) {
        StandardChartTheme sct = createChartTheme3456();
        ChartFactory.setChartTheme(sct);
        
        JFreeChart chartStackBarType = ChartFactory.createStackedBarChart(
                year + "碳排放变化趋势(按排放类型)",
                "时间(月)",
                "碳排放量(t)",
                datasetType,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        chartStackBarType.setTextAntiAlias(true);
        chartStackBarType.setBackgroundPaint(Color.WHITE);
        chartStackBarType.setAntiAlias(true);
        
        CategoryPlot typePlot = chartStackBarType.getCategoryPlot();
        typePlot.setBackgroundPaint(Color.WHITE);
        typePlot.setRangeGridlinePaint(new Color(230, 230, 230));
        typePlot.setDomainGridlinePaint(new Color(230, 230, 230));
        
        CategoryAxis typeAxis = typePlot.getDomainAxis();
        typeAxis.setLabelFont(getChart3456AxisLabelFont());
        typeAxis.setTickLabelFont(getChart3456AxisTickFont());
        typeAxis.setMaximumCategoryLabelLines(2);
        typeAxis.setCategoryMargin(0.1);
        
        // 设置堆叠柱状图颜色
        CategoryItemRenderer typeRenderer = typePlot.getRenderer();
        typeRenderer.setSeriesPaint(0, new Color(79, 129, 189));   // 直接排放 - 蓝色
        typeRenderer.setSeriesPaint(1, new Color(192, 80, 77));    // 间接排放 - 红色
        typeRenderer.setSeriesPaint(2, new Color(155, 187, 89));    // 其它排放 - 绿色
        
        chartStackBarType.getLegend().setFrame(new BlockBorder(Color.WHITE));
        chartStackBarType.getLegend().setItemFont(getChart3456LegendFont());
        
        return chartStackBarType;
    }
    
    // ==================== 图5和图6的合并方法（保持向后兼容） ====================
    
    /**
     * 创建图5和图6：堆叠柱状图（合并方法，保持向后兼容）
     * 
     * @param document Word文档对象
     * @param carbonEmissions 排放数据列表
     * @param year 年份
     * @throws Exception 处理异常
     */
    public static void createStackBarToWord(XWPFDocument document, 
                                             List<CarbonEmission> carbonEmissions, 
                                             int year) throws Exception {
        createStackBarChartByCategoryToWord(document, carbonEmissions, year);
        createStackBarChartByTypeToWord(document, carbonEmissions, year);
    }
    
    // ==================== 图表插入Word的公共方法 ====================
    
    /**
     * 将图表插入到Word文档
     * 
     * @param document Word文档对象
     * @param chartFile 图表文件
     * @param fileName 文件名
     * @param key 查找关键字
     * @param width 图表宽度
     * @param height 图表高度
     * @throws Exception 处理异常
     */
    private static void insertChartToWord(XWPFDocument document, File chartFile, 
                                           String fileName, String key, int width, int height) throws Exception {
        XWPFRun run = findTargetRun(document, key);
        if (run != null) {
            @SuppressWarnings("deprecation")
            XWPFParagraph paragraph = run.getParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            // 使用try-with-resources确保文件流正确关闭
            try (InputStream pictureStream = Files.newInputStream(chartFile.toPath())) {
                run.addPicture(pictureStream,
                        XWPFDocument.PICTURE_TYPE_PNG, fileName, 
                        Units.toEMU(width), Units.toEMU(height));
            }
        }
    }
}
