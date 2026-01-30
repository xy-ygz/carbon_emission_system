package com.bjfu.carbon.utils;

import lombok.extern.slf4j.Slf4j;
import org.docx4j.fonts.BestMatchingMapper;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF字体处理工具类
 * 用于处理PDF导出时的字体注册和映射
 * 
 * @author xgy
 */
@Slf4j
public class PdfFontUtils {
    
    /** fonts 目录下实际存在的字体文件 */
    private static final String[] FONT_FILE_NAMES = {
        "NotoSerifCJKsc-Regular.otf",
        "NotoSansCJKsc-VF.ttf",
        "SourceHanSerifSC-VF.ttf",
        "SimSun.ttf"
    };
    
    private static final String SYSTEM_SONGTI_PATH = "/System/Library/Fonts/Supplemental/Songti.ttc";
    
    /**
     * 注册字体并设置字体映射器
     * 
     * @param tempFontDir 临时字体目录
     * @param wordMLPackage Word文档包
     * @return SimSun字体文件（如果找到）
     * @throws IOException 字体处理异常
     */
    public static File registerFontsAndSetMapper(File tempFontDir, WordprocessingMLPackage wordMLPackage) throws IOException {
        long startTime = System.currentTimeMillis();
        File simSunFontFile = null;
        
        // 创建临时字体目录，字体自动扫描已在CarbonApplication启动时禁用
        if (!tempFontDir.exists()) {
            tempFontDir.mkdirs();
        }
        
        // 复制并注册字体文件（性能瓶颈：每次都要复制8个字体文件）
        long copyStartTime = System.currentTimeMillis();
        List<File> copiedFontFiles = copyChineseFontsToTempDir(tempFontDir);
        long copyEndTime = System.currentTimeMillis();
        log.debug("字体文件复制耗时: {}ms，复制了{}个文件", copyEndTime - copyStartTime, copiedFontFiles.size());
        
        // 注册字体文件到PhysicalFonts
        long registerStartTime = System.currentTimeMillis();
        simSunFontFile = registerFontFiles(copiedFontFiles, tempFontDir);
        // 只注册必要的系统字体（macOS的Songti），不扫描整个系统字体目录
        registerSystemFont();
        long registerEndTime = System.currentTimeMillis();
        log.debug("字体注册到PhysicalFonts耗时: {}ms", registerEndTime - registerStartTime);
        
        // 设置字体映射器（传入已复制的字体列表，便于按 URI 解析回退 CJK 字体）
        setFontMapper(wordMLPackage, simSunFontFile, copiedFontFiles);
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.debug("字体处理总耗时: {}ms", totalTime);
        
        return simSunFontFile;
    }
    
    /**
     * 复制中文字体文件到临时目录
     */
    private static List<File> copyChineseFontsToTempDir(File tempDir) throws IOException {
        List<File> copiedFiles = new ArrayList<>();
        File[] fontDirs = getFontDirectories();
        
        for (String fontFileName : FONT_FILE_NAMES) {
            File sourceFile = findFontFile(fontDirs, fontFileName);
            
            if (sourceFile != null) {
                copyFontFile(sourceFile, tempDir, fontFileName, copiedFiles);
            } else {
                copyFontFromClasspath(tempDir, fontFileName, copiedFiles);
            }
        }
        
        logFontCopyResult(copiedFiles, tempDir);
        return copiedFiles;
    }
    
    /**
     * 查找字体文件
     */
    private static File findFontFile(File[] fontDirs, String fontFileName) {
        for (File fontDir : fontDirs) {
            if (fontDir != null && fontDir.exists() && fontDir.isDirectory()) {
                File fontFile = new File(fontDir, fontFileName);
                if (fontFile.exists() && fontFile.isFile()) {
                    return fontFile;
                }
            }
        }
        return null;
    }
    
    /**
     * 从文件系统复制字体文件
     */
    private static void copyFontFile(File sourceFile, File tempDir, String fontFileName, List<File> copiedFiles) {
        try {
            File targetFile = new File(tempDir, fontFileName);
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            copiedFiles.add(targetFile);
        } catch (Exception e) {
            log.debug("复制字体文件 {} 失败: {}", fontFileName, e.getMessage());
        }
    }
    
    /**
     * 从classpath复制字体文件
     */
    private static void copyFontFromClasspath(File tempDir, String fontFileName, List<File> copiedFiles) {
        String fontPath = "fonts/" + fontFileName;
        try (InputStream fontStream = PdfFontUtils.class.getClassLoader().getResourceAsStream(fontPath)) {
            if (fontStream != null) {
                File targetFile = new File(tempDir, fontFileName);
                Files.copy(fontStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copiedFiles.add(targetFile);
            }
        } catch (Exception e) {
            log.debug("从classpath复制字体文件 {} 失败: {}", fontFileName, e.getMessage());
        }
    }
    
    /**
     * 注册字体文件到PhysicalFonts
     */
    private static File registerFontFiles(List<File> copiedFontFiles, File tempFontDir) {
        File simSunFontFile = null;
        boolean hasRealSimSun = false;
        
        for (File fontFile : copiedFontFiles) {
            try {
                PhysicalFonts.addPhysicalFont(fontFile.toURI());
                if ("SimSun.ttf".equals(fontFile.getName())) {
                    hasRealSimSun = true;
                    simSunFontFile = fontFile;
                }
            } catch (Exception e) {
                log.warn("注册字体文件 {} 失败: {}", fontFile.getName(), e.getMessage());
            }
        }
        
        // 如果没有SimSun，尝试创建副本
        if (!hasRealSimSun) {
            simSunFontFile = createSimSunCopy(copiedFontFiles, tempFontDir);
        }
        
        return simSunFontFile;
    }
    
    /**
     * 优先 Noto Sans CJK / 思源
     */
    private static File createSimSunCopy(List<File> copiedFontFiles, File tempFontDir) {
        String[] preferForSimSun = {"NotoSansCJKsc-VF", "SourceHanSerifSC-VF", "NotoSerifCJKsc-Regular"};
        for (String prefix : preferForSimSun) {
            for (File fontFile : copiedFontFiles) {
                if (fontFile.getName().contains(prefix)) {
                    try {
                        File simSunFile = new File(tempFontDir, "SimSun.ttf");
                        Files.copy(fontFile.toPath(), simSunFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        PhysicalFonts.addPhysicalFont(simSunFile.toURI());
                        return simSunFile;
                    } catch (Exception e) {
                        log.warn("创建 SimSun.ttf 副本失败: {}", e.getMessage());
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 注册系统字体
     */
    private static void registerSystemFont() {
        File systemSongti = new File(SYSTEM_SONGTI_PATH);
        if (systemSongti.exists()) {
            try {
                PhysicalFonts.addPhysicalFont(systemSongti.toURI());
            } catch (Exception e) {
                log.debug("注册系统字体失败: {}", e.getMessage());
            }
        }
    }
    
    /** 已注册 CJK 字体的 (文件名前缀, 内部名称候选)，用于 getPhysicalFont(name, file.toURI()) */
    private static final String[][] CJK_NAME_BY_FILE_PREFIX = {
        {"NotoSerifCJKsc", "Noto Serif CJK SC", "Noto Serif CJK SC Regular", "Noto Serif CJK sc"},
        {"NotoSansCJKsc", "Noto Sans CJK SC", "Noto Sans CJK SC Variable", "Noto Sans CJK sc"},
        {"SourceHanSerifSC", "Source Han Serif SC", "Source Han Serif", "Source Han Serif SC Variable"},
        {"SimSun.ttf", "SimSun"}
    };

    /**
     * 设置字体映射器：统一用已注册的 CJK 字体（含 ₂）作为回退，避免 FOP 用 Helvetica 导致 kgCO#。
     */
    private static void setFontMapper(WordprocessingMLPackage wordMLPackage, File simSunFontFile,
                                     List<File> copiedFontFiles) {
        final PhysicalFont fallbackCjkFont = resolveFallbackCjkFont(simSunFontFile, copiedFontFiles);
        BestMatchingMapper fontMapper = new BestMatchingMapper() {
            @Override
            public PhysicalFont get(String fontFamily) {
                if (fallbackCjkFont != null) {
                    return fallbackCjkFont;
                }
                return super.get(fontFamily);
            }
        };
        try {
            wordMLPackage.setFontMapper(fontMapper);
        } catch (Exception e) {
            log.warn("设置字体映射器失败: {}", e.getMessage());
        }
        if (fallbackCjkFont == null) {
            log.warn("未找到可用的 CJK 字体作为回退，PDF 中 ₂ 可能显示为 #");
        } else {
            log.info("PDF 字体回退已设置为 CJK 字体，用于正确显示 ₂ 等字形");
        }
    }

    /**
     * 从已复制的字体文件中解析一个已注册的 CJK 字体（含 ₂），按 (内部名, URI) 逐个尝试。
     */
    private static PhysicalFont resolveFallbackCjkFont(File simSunFontFile, List<File> copiedFontFiles) {
        if (simSunFontFile != null) {
            try {
                List<PhysicalFont> fonts = PhysicalFonts.getPhysicalFont("SimSun", simSunFontFile.toURI());
                if (fonts != null && !fonts.isEmpty()) {
                    return fonts.get(0);
                }
            } catch (Exception e) {
                log.debug("按 SimSun URI 查找失败: {}", e.getMessage());
            }
        }
        if (copiedFontFiles != null) {
            for (File f : copiedFontFiles) {
                String fileName = f.getName();
                for (String[] row : CJK_NAME_BY_FILE_PREFIX) {
                    if (!fileName.startsWith(row[0])) {
                        continue;
                    }
                    for (int i = 1; i < row.length; i++) {
                        try {
                            List<PhysicalFont> fonts = PhysicalFonts.getPhysicalFont(row[i], f.toURI());
                            if (fonts != null && !fonts.isEmpty()) {
                                log.debug("解析到 CJK 回退字体: {} (文件: {})", row[i], fileName);
                                return fonts.get(0);
                            }
                        } catch (Exception e) {
                            log.trace("getPhysicalFont({}, {}) 失败: {}", row[i], fileName, e.getMessage());
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 获取字体文件目录列表
     */
    private static File[] getFontDirectories() {
        List<File> fontDirs = new ArrayList<>();
        
        // 环境变量指定的字体目录
        String fontDirEnv = System.getenv("FONT_DIR");
        if (fontDirEnv != null && !fontDirEnv.isEmpty()) {
            File envFontDir = new File(fontDirEnv);
            if (envFontDir.exists() && envFontDir.isDirectory()) {
                fontDirs.add(envFontDir);
            }
        }
        
        // Docker容器内常见路径
        addFontDirIfExists(fontDirs, "/app/fonts");
        addFontDirIfExists(fontDirs, "fonts");
        addFontDirIfExists(fontDirs, "src/main/resources/fonts");
        
        return fontDirs.toArray(new File[0]);
    }
    
    /**
     * 如果目录存在则添加到列表
     */
    private static void addFontDirIfExists(List<File> fontDirs, String path) {
        File fontDir = new File(path);
        if (fontDir.exists() && fontDir.isDirectory()) {
            fontDirs.add(fontDir);
        }
    }
    
    /**
     * 记录字体复制结果
     */
    private static void logFontCopyResult(List<File> copiedFiles, File tempDir) {
        if (!copiedFiles.isEmpty()) {
            log.info("已复制 {} 个字体文件到临时目录: {}", copiedFiles.size(), tempDir.getAbsolutePath());
        } else {
            log.warn("未找到项目中的字体文件，FOP 将使用系统字体");
        }
    }
}
