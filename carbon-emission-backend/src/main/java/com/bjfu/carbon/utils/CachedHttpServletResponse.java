package com.bjfu.carbon.utils;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * 可缓存的HttpServletResponse包装类
 * 用于拦截响应输出，同时写入响应流和字节数组（用于缓存）
 * 
 * @author xgy
 */
public class CachedHttpServletResponse extends HttpServletResponseWrapper {
    
    private ByteArrayOutputStream cachedOutputStream;
    private ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;
    
    public CachedHttpServletResponse(HttpServletResponse response) {
        super(response);
        this.cachedOutputStream = new ByteArrayOutputStream();
    }
    
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (servletOutputStream == null) {
            servletOutputStream = new CachedServletOutputStream(
                getResponse().getOutputStream(), 
                cachedOutputStream
            );
        }
        return servletOutputStream;
    }
    
    @Override
    public PrintWriter getWriter() throws IOException {
        if (printWriter == null) {
            printWriter = new PrintWriter(
                new OutputStreamWriter(getOutputStream(), getCharacterEncoding())
            );
        }
        return printWriter;
    }
    
    /**
     * 获取缓存的响应数据
     */
    public byte[] getCachedData() {
        return cachedOutputStream.toByteArray();
    }
    
    /**
     * 内部类：可缓存的ServletOutputStream
     */
    private static class CachedServletOutputStream extends ServletOutputStream {
        private final ServletOutputStream originalStream;
        private final ByteArrayOutputStream cachedStream;
        
        public CachedServletOutputStream(ServletOutputStream originalStream, 
                                        ByteArrayOutputStream cachedStream) {
            this.originalStream = originalStream;
            this.cachedStream = cachedStream;
        }
        
        @Override
        public void write(int b) throws IOException {
            originalStream.write(b);
            cachedStream.write(b);
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            originalStream.write(b);
            cachedStream.write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            originalStream.write(b, off, len);
            cachedStream.write(b, off, len);
        }
        
        @Override
        public void flush() throws IOException {
            originalStream.flush();
            cachedStream.flush();
        }
        
        @Override
        public void close() throws IOException {
            originalStream.close();
            cachedStream.close();
        }
        
        @Override
        public boolean isReady() {
            return originalStream.isReady();
        }
        
        @Override
        public void setWriteListener(WriteListener listener) {
            originalStream.setWriteListener(listener);
        }
    }
}
