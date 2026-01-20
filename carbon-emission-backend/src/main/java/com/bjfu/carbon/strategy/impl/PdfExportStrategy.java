package com.bjfu.carbon.strategy.impl;

import com.bjfu.carbon.strategy.ExportStrategy;
import com.bjfu.carbon.utils.PdfFontUtils;
import com.bjfu.carbon.utils.XWPFUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.docx4j.Docx4J;
import org.docx4j.convert.out.FOSettings;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * PDF导出策略实现
 * 复用Word导出逻辑，将生成的Word文档转换为PDF格式
 * 
 * @author xgy
 */
@Slf4j
@Component("pdfExportStrategyDocx4j")
public class PdfExportStrategy implements ExportStrategy {

    private static final String TRANSFORMER_FACTORY = "javax.xml.transform.TransformerFactory";
    private static final String TRANSFORMER_FACTORY_IMPL = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
    
    @Autowired
    private WordExportStrategy wordExportStrategy;

    @Override
    public void export(Integer year, HttpServletResponse response) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("开始PDF导出，年份: {}", year);
        
        // 设置系统属性
        setTransformerFactory();
        
        if (!checkFontLoaded(response)) {
            return;
        }
        
        File tempImageDir = createTempImageDir();
        File tempWordFile = null;
        XWPFDocument wordDocument = null;
        
        try {
            // 1. 生成Word文档（复用Word导出逻辑）
            long wordStartTime = System.currentTimeMillis();
            wordDocument = wordExportStrategy.createWordDocument(year);
            long wordEndTime = System.currentTimeMillis();
            log.info("Word文档生成耗时: {}ms", wordEndTime - wordStartTime);
            
            if (wordDocument == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "该年份暂无数据");
                return;
            }
            
            // 2. 保存Word文档到临时文件
            long saveStartTime = System.currentTimeMillis();
            tempWordFile = saveWordDocumentToFile(wordDocument);
            wordDocument.close();
            wordDocument = null;
            long saveEndTime = System.currentTimeMillis();
            log.info("Word文档保存到文件耗时: {}ms", saveEndTime - saveStartTime);
            
            // 3. 转换为PDF
            long pdfStartTime = System.currentTimeMillis();
            convertWordFileToPdf(tempWordFile, tempImageDir.getAbsolutePath(), tempImageDir, year, response);
            long pdfEndTime = System.currentTimeMillis();
            log.info("Word转PDF转换耗时: {}ms", pdfEndTime - pdfStartTime);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("PDF导出总耗时: {}ms (Word生成: {}ms, 文件保存: {}ms, PDF转换: {}ms)", 
                totalTime, wordEndTime - wordStartTime, saveEndTime - saveStartTime, pdfEndTime - pdfStartTime);
            
        } catch (IOException e) {
            log.error("PDF导出失败", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "PDF导出失败：" + e.getMessage());
        } finally {
            closeResource(wordDocument);
            deleteFile(tempWordFile);
            deleteDirectory(tempImageDir);
        }
    }
    
    /**
     * 设置TransformerFactory
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
            String status = XWPFUtils.getFontLoadStatus();
            log.warn("PDF导出：中文字体未加载完成，当前状态: {}", status);
            sendErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, 
                "导出失败：系统正在初始化中文字体，请稍候几秒后重试。当前状态: " + status);
            return false;
        }
        return true;
    }
    
    /**
     * 创建临时图片目录
     */
    private File createTempImageDir() {
        String tempImageDir = System.getProperty("java.io.tmpdir") + File.separator + 
            "pdf_images_" + System.currentTimeMillis();
        File imageDir = new File(tempImageDir);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        return imageDir;
    }
    
    /**
     * 保存Word文档到临时文件
     */
    private File saveWordDocumentToFile(XWPFDocument wordDocument) throws IOException {
        File tempWordFile = File.createTempFile("word_export_", ".docx");
        tempWordFile.deleteOnExit();
        
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempWordFile)) {
            wordDocument.write(fileOutputStream);
            fileOutputStream.flush();
        }
        
        if (!tempWordFile.exists() || tempWordFile.length() == 0) {
            throw new IOException("Word文档文件未创建或为空");
        }
        
        return tempWordFile;
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
        response.getWriter().flush();
    }
    
    /**
     * 关闭资源
     */
    private void closeResource(XWPFDocument document) {
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * 删除文件
     */
    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                log.warn("删除临时文件失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            try {
                deleteDirectoryRecursive(directory);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectoryRecursive(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
    
    /**
     * 将Word文档文件转换为PDF并输出
     */
    private void convertWordFileToPdf(File wordFile, String tempImageDir, File imageDir, 
                                       Integer year, HttpServletResponse response) throws IOException {
        WordprocessingMLPackage wordMLPackage = null;
        File tempFontDir = null;
        
        try {
            // 1. 创建临时字体目录（在加载Word文档之前，确保字体扫描设置生效）
            tempFontDir = new File(System.getProperty("java.io.tmpdir") + File.separator + 
                "pdf_fonts_" + System.currentTimeMillis());
            if (!tempFontDir.exists()) {
                tempFontDir.mkdirs();
            }
            
            // 2. 加载Word文档（此时系统字体扫描应该已被禁用）
            long loadStartTime = System.currentTimeMillis();
            wordMLPackage = WordprocessingMLPackage.load(wordFile);
            long loadEndTime = System.currentTimeMillis();
            log.info("Word文档加载到WordprocessingMLPackage耗时: {}ms", loadEndTime - loadStartTime);
            
            // 3. 注册字体（在Word文档加载之后，避免影响文档加载）
            long fontStartTime = System.currentTimeMillis();
            PdfFontUtils.registerFontsAndSetMapper(tempFontDir, wordMLPackage);
            long fontEndTime = System.currentTimeMillis();
            log.info("字体注册耗时: {}ms", fontEndTime - fontStartTime);
            
            // 3. 配置FO设置（优化性能配置）
            long foStartTime = System.currentTimeMillis();
            FOSettings foSettings = new FOSettings(wordMLPackage);
            foSettings.setApacheFopMime("application/pdf");
            foSettings.setImageDirPath(tempImageDir);
            foSettings.setFoDumpFile(null);
            // 性能优化：禁用FO文件输出（用于调试，生产环境不需要）
            // 性能优化：使用XSL转换（FLAG_EXPORT_PREFER_XSL）而不是MIMETYPE转换
            long foEndTime = System.currentTimeMillis();
            log.info("FO设置配置耗时: {}ms", foEndTime - foStartTime);
            
            // 5. 设置响应头
            String fileName = "北京林业大学" + year + "年度碳排放报告.pdf";
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setContentType(getContentType());
            
            // 6. 转换为PDF并输出（这是最耗时的操作）
            // Docx4J.toFO转换耗时约3-4秒，这是docx4j和FOP库本身的性能限制
            // 主要耗时在：XSL-FO转换、图表处理、PDF渲染
            long convertStartTime = System.currentTimeMillis();
            OutputStream outputStream = response.getOutputStream();
            Docx4J.toFO(foSettings, outputStream, Docx4J.FLAG_EXPORT_PREFER_XSL);
            outputStream.flush();
            long convertEndTime = System.currentTimeMillis();
            log.info("Docx4J.toFO转换耗时: {}ms (这是PDF导出的主要性能瓶颈，包含XSL-FO转换和PDF渲染)", 
                convertEndTime - convertStartTime);
            
        } catch (Exception e) {
            log.error("Word转PDF失败", e);
            throw new IOException("Word转PDF失败: " + e.getMessage(), e);
        } finally {
            cleanupResources(wordMLPackage, tempFontDir);
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanupResources(WordprocessingMLPackage wordMLPackage, File tempFontDir) {
        if (wordMLPackage != null) {
            try {
                if (wordMLPackage.getMainDocumentPart().getFontTablePart() != null) {
                    wordMLPackage.getMainDocumentPart().getFontTablePart().deleteEmbeddedFontTempFiles();
                }
            } catch (Exception ignored) {}
        }
        deleteDirectory(tempFontDir);
    }

    @Override
    public String getSupportedExtension() {
        return "pdf";
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }
}
