package com.bjfu.carbon.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * 文件缓存工具类
 * 用于缓存导出的文件（Word、PDF等）
 * 文件存储在文件系统，文件路径和元数据存储在Redis
 * 
 * @author xgy
 */
@Slf4j
@Component
public class FileCacheUtils {
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 文件缓存目录（系统临时目录下的子目录）
     */
    private static final String CACHE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "export_cache";
    
    /**
     * 文件缓存元数据
     */
    public static class FileCacheMeta {
        private String filePath;
        private String fileName;
        private String contentType;
        private long fileSize;
        private long createTime;
        
        public FileCacheMeta() {}
        
        public FileCacheMeta(String filePath, String fileName, String contentType, long fileSize) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.createTime = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }
    }
    
    /**
     * 初始化缓存目录
     */
    private void initCacheDir() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
        } catch (IOException e) {
            log.error("创建缓存目录失败: {}", CACHE_DIR, e);
        }
    }
    
    /**
     * 获取缓存文件
     * 
     * @param cacheKey 缓存键
     * @return 文件缓存元数据，如果不存在返回null
     */
    public FileCacheMeta getCacheFile(String cacheKey) {
        try {
            String metaJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (metaJson == null) {
                return null;
            }
            
            FileCacheMeta meta = objectMapper.readValue(metaJson, FileCacheMeta.class);
            
            // 检查文件是否还存在
            File file = new File(meta.getFilePath());
            if (!file.exists() || !file.isFile()) {
                log.warn("缓存文件不存在，删除缓存键: {}, 文件路径: {}", cacheKey, meta.getFilePath());
                stringRedisTemplate.delete(cacheKey);
                return null;
            }
            
            return meta;
        } catch (Exception e) {
            log.error("获取缓存文件失败，key: {}", cacheKey, e);
            return null;
        }
    }
    
    /**
     * 保存文件到缓存
     * 
     * @param cacheKey 缓存键
     * @param fileName 文件名
     * @param contentType 内容类型
     * @param fileData 文件数据（字节数组）
     * @param expireSeconds 过期时间（秒）
     * @return 是否保存成功
     */
    public boolean saveCacheFile(String cacheKey, String fileName, String contentType, 
                                 byte[] fileData, long expireSeconds) {
        try {
            initCacheDir();
            
            // 生成文件路径
            String fileExtension = getFileExtension(fileName);
            String cacheFileName = cacheKey.replace(":", "_") + "." + fileExtension;
            File cacheFile = new File(CACHE_DIR, cacheFileName);
            
            // 保存文件到文件系统
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(fileData);
                fos.flush();
            }
            
            // 创建元数据
            FileCacheMeta meta = new FileCacheMeta(
                cacheFile.getAbsolutePath(),
                fileName,
                contentType,
                fileData.length
            );
            
            // 保存元数据到Redis
            String metaJson = objectMapper.writeValueAsString(meta);
            if (expireSeconds > 0) {
                stringRedisTemplate.opsForValue().set(cacheKey, metaJson, expireSeconds, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(cacheKey, metaJson);
            }
            
            log.debug("文件已缓存，key: {}, 文件路径: {}, 大小: {} bytes", 
                cacheKey, cacheFile.getAbsolutePath(), fileData.length);
            
            return true;
        } catch (Exception e) {
            log.error("保存缓存文件失败，key: {}", cacheKey, e);
            return false;
        }
    }
    
    /**
     * 读取缓存文件内容
     * 
     * @param meta 文件缓存元数据
     * @return 文件字节数组，如果读取失败返回null
     */
    public byte[] readCacheFile(FileCacheMeta meta) {
        if (meta == null || meta.getFilePath() == null) {
            return null;
        }
        
        try {
            File file = new File(meta.getFilePath());
            if (!file.exists() || !file.isFile()) {
                log.warn("缓存文件不存在: {}", meta.getFilePath());
                return null;
            }
            
            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            }
            
            return data;
        } catch (IOException e) {
            log.error("读取缓存文件失败: {}", meta.getFilePath(), e);
            return null;
        }
    }
    
    /**
     * 删除缓存文件
     * 
     * @param cacheKey 缓存键
     */
    public void deleteCacheFile(String cacheKey) {
        try {
            FileCacheMeta meta = getCacheFile(cacheKey);
            if (meta != null && meta.getFilePath() != null) {
                File file = new File(meta.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            stringRedisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.error("删除缓存文件失败，key: {}", cacheKey, e);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "tmp";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    /**
     * 生成缓存键
     * 
     * @param prefix 前缀
     * @param year 年份
     * @param format 格式（docx/pdf）
     * @return 缓存键
     */
    public static String generateCacheKey(String prefix, Integer year, String format) {
        return prefix + ":" + year + ":" + format;
    }
}
