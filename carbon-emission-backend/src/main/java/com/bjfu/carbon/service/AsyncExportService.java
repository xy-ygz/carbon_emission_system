package com.bjfu.carbon.service;

import com.bjfu.carbon.strategy.ExportStrategy;
import com.bjfu.carbon.strategy.ExportStrategyFactory;
import com.bjfu.carbon.utils.FileCacheUtils;
import com.bjfu.carbon.vo.ExportTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 异步导出服务
 * 用于处理异步导出任务
 *
 * @author xgy
 */
@Slf4j
@Service
public class AsyncExportService {

    @Autowired
    private ExportStrategyFactory exportStrategyFactory;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private FileCacheUtils fileCacheUtils;

    /**
     * ObjectMapper配置，支持Java 8时间类型序列化
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化ObjectMapper
     */
    public AsyncExportService() {
        this.objectMapper = new ObjectMapper();
        // 注册Java 8时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳的功能，使用ISO-8601格式
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 任务信息在Redis中的过期时间（24小时）
     */
    private static final long TASK_EXPIRE_SECONDS = 86400;

    /**
     * 任务信息在Redis中的键前缀
     */
    private static final String TASK_KEY_PREFIX = "export:task:";

    /**
     * 创建导出任务并异步执行
     *
     * @param userId 用户ID
     * @param year 年份
     * @param format 格式（docx/pdf）
     * @return 任务ID
     */
    public String createExportTask(Long userId, Integer year, String format) {
        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 创建任务对象
        ExportTask task = ExportTask.create(taskId, userId, year, format);

        // 保存任务信息到Redis
        saveTask(task);

        // 异步执行导出任务
        executeExportTask(task);

        log.info("创建导出任务: taskId={}, userId={}, year={}, format={}",
            taskId, userId, year, format);

        return taskId;
    }

    /**
     * 异步执行导出任务
     *
     * @param task 导出任务
     */
    @Async("exportDataExecutor")
    public void executeExportTask(ExportTask task) {
        log.info("开始执行导出任务: taskId={}, year={}, format={}",
            task.getTaskId(), task.getYear(), task.getFormat());

        // 更新任务状态为处理中
        task.markProcessing();
        saveTask(task);

        try {
            // 先检查缓存
            String cacheKey = FileCacheUtils.generateCacheKey("export", task.getYear(), task.getFormat());
            FileCacheUtils.FileCacheMeta cachedMeta = fileCacheUtils.getCacheFile(cacheKey);

            if (cachedMeta != null) {
                // 缓存命中，直接使用缓存文件
                log.info("导出任务使用缓存: taskId={}, cacheKey={}", task.getTaskId(), cacheKey);
                task.markCompleted(cacheKey, cachedMeta.getFileName(), cachedMeta.getFileSize());
                saveTask(task);
                return;
            }

            // 缓存未命中，生成文件
            ExportStrategy strategy = exportStrategyFactory.getStrategy(task.getFormat());

            // 创建虚拟响应对象用于捕获导出数据
            VirtualHttpServletResponse virtualResponse = new VirtualHttpServletResponse();

            // 执行导出
            strategy.export(task.getYear(), virtualResponse);

            // 获取导出的文件数据
            byte[] fileData = virtualResponse.getData();
            if (fileData == null || fileData.length == 0) {
                throw new IOException("导出文件数据为空");
            }

            // 生成文件名
            String fileName = virtualResponse.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "北京林业大学" + task.getYear() + "年度碳排放报告." + task.getFormat();
            }
            String contentType = virtualResponse.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = strategy.getContentType();
            }

            // 保存到缓存
            fileCacheUtils.saveCacheFile(cacheKey, fileName, contentType, fileData, 86400);

            // 更新任务状态为完成
            task.markCompleted(cacheKey, fileName, (long) fileData.length);
            saveTask(task);

            log.info("导出任务完成: taskId={}, fileName={}, fileSize={} bytes",
                task.getTaskId(), fileName, fileData.length);

        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", task.getTaskId(), e);
            task.markFailed(e.getMessage());
            saveTask(task);
        }
    }

    /**
     * 获取任务信息
     *
     * @param taskId 任务ID
     * @return 任务信息，如果不存在返回null
     */
    public ExportTask getTask(String taskId) {
        try {
            String key = TASK_KEY_PREFIX + taskId;
            log.debug("查询任务信息: key={}", key);
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                log.warn("任务不存在: taskId={}, key={}", taskId, key);
                return null;
            }
            ExportTask task = objectMapper.readValue(json, ExportTask.class);
            log.debug("任务查询成功: taskId={}, status={}", taskId, task.getStatus());
            return task;
        } catch (Exception e) {
            log.error("获取任务信息失败: taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 保存任务信息到Redis
     *
     * @param task 任务对象
     */
    private void saveTask(ExportTask task) {
        try {
            String key = TASK_KEY_PREFIX + task.getTaskId();
            String json = objectMapper.writeValueAsString(task);
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, json, TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (result == null || !result) {
                // 如果key已存在，使用set更新
                stringRedisTemplate.opsForValue().set(key, json, TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
            log.info("任务信息已保存到Redis: taskId={}, key={}, status={}",
                task.getTaskId(), key, task.getStatus());
        } catch (Exception e) {
            log.error("保存任务信息失败: taskId={}", task.getTaskId(), e);
            // 不抛出异常，避免影响任务创建流程
            // 如果Redis保存失败，任务查询时会返回null，前端会处理
        }
    }

    /**
     * 虚拟HTTP响应对象
     * 用于捕获导出策略写入的数据
     * 使用HttpServletResponseWrapper来避免实现所有接口方法
     */
    private static class VirtualHttpServletResponse extends javax.servlet.http.HttpServletResponseWrapper {
        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private String contentType;
        private String fileName;
        private javax.servlet.ServletOutputStream servletOutputStream;

        public VirtualHttpServletResponse() {
            super(new DummyHttpServletResponse());
        }

        @Override
        public javax.servlet.ServletOutputStream getOutputStream() throws IOException {
            if (servletOutputStream == null) {
                servletOutputStream = new javax.servlet.ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        outputStream.write(b);
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        outputStream.write(b);
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        outputStream.write(b, off, len);
                    }

                    @Override
                    public void flush() throws IOException {
                        outputStream.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        outputStream.close();
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(javax.servlet.WriteListener listener) {
                        // 不支持异步写入
                    }
                };
            }
            return servletOutputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(outputStream);
        }

        @Override
        public void setContentType(String type) {
            this.contentType = type;
            super.setContentType(type);
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, value);
            if ("Content-Disposition".equalsIgnoreCase(name) && value != null) {
                // 提取文件名
                int filenameIndex = value.indexOf("filename=");
                if (filenameIndex >= 0) {
                    String filenamePart = value.substring(filenameIndex + 9);
                    if (filenamePart.startsWith("\"") && filenamePart.endsWith("\"")) {
                        filenamePart = filenamePart.substring(1, filenamePart.length() - 1);
                    }
                    this.fileName = filenamePart;
                }
            }
        }

        public byte[] getData() {
            return outputStream.toByteArray();
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileName() {
            return fileName;
        }

        /**
         * 虚拟的HttpServletResponse实现
         * 用于作为HttpServletResponseWrapper的包装对象
         */
        private static class DummyHttpServletResponse implements javax.servlet.http.HttpServletResponse {
            @Override
            public void addCookie(javax.servlet.http.Cookie cookie) {}

            @Override
            public boolean containsHeader(String name) { return false; }

            @Override
            public String encodeURL(String url) { return url; }

            @Override
            public String encodeRedirectURL(String url) { return url; }

            @Override
            public String encodeUrl(String url) { return url; }

            @Override
            public String encodeRedirectUrl(String url) { return url; }

            @Override
            public void sendError(int sc, String msg) throws IOException {}

            @Override
            public void sendError(int sc) throws IOException {}

            @Override
            public void sendRedirect(String location) throws IOException {}

            @Override
            public void setDateHeader(String name, long date) {}

            @Override
            public void addDateHeader(String name, long date) {}

            @Override
            public void setHeader(String name, String value) {}

            @Override
            public void addHeader(String name, String value) {}

            @Override
            public void setIntHeader(String name, int value) {}

            @Override
            public void addIntHeader(String name, int value) {}

            @Override
            public void setStatus(int sc) {}

            @Override
            public void setStatus(int sc, String sm) {}

            @Override
            public int getStatus() { return 200; }

            @Override
            public String getHeader(String name) { return null; }

            @Override
            public Collection<String> getHeaders(String name) { return Collections.emptyList(); }

            @Override
            public Collection<String> getHeaderNames() { return Collections.emptyList(); }

            @Override
            public String getCharacterEncoding() { return "UTF-8"; }

            @Override
            public String getContentType() { return null; }

            @Override
            public javax.servlet.ServletOutputStream getOutputStream() throws IOException {
                throw new UnsupportedOperationException("Use wrapper's getOutputStream");
            }

            @Override
            public PrintWriter getWriter() throws IOException {
                throw new UnsupportedOperationException("Use wrapper's getWriter");
            }

            @Override
            public void setCharacterEncoding(String charset) {}

            @Override
            public void setContentLength(int len) {}

            @Override
            public void setContentLengthLong(long length) {}

            @Override
            public void setContentType(String type) {}

            @Override
            public void setBufferSize(int size) {}

            @Override
            public int getBufferSize() { return 8192; }

            @Override
            public void flushBuffer() throws IOException {}

            @Override
            public void resetBuffer() {}

            @Override
            public boolean isCommitted() { return false; }

            @Override
            public void reset() {}

            @Override
            public void setLocale(Locale loc) {}

            @Override
            public Locale getLocale() { return Locale.getDefault(); }
        }
    }
}
