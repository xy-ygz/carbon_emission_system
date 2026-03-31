package com.bjfu.carbon.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 将 /api/ai/qa/* 转发至 Python ai-qa-service，使 {@link com.bjfu.carbon.aspect.AuditLogAspect} 能记录访问审计。
 * 部署时需把 Nginx 中 /api/ai/qa/ 指向本后端，而非直连 Python。
 */
@Slf4j
@RestController
@RequestMapping("/ai/qa")
public class AiQaProxyController {

    @Value("${ai-qa.base-url:http://127.0.0.1:8000}")
    private String aiQaBaseUrl;

    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> ask(@RequestBody byte[] body, HttpServletRequest request) {
        try {
            return proxyBytes("POST", "/ask", body, request, true);
        } catch (IOException e) {
            log.warn("AI QA proxy /ask failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":502,\"data\":\"\",\"message\":\"智能问答服务不可用\"}".getBytes(StandardCharsets.UTF_8));
        }
    }

    @PostMapping(value = "/ask/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> askStream(@RequestBody byte[] body, HttpServletRequest request) {
        HttpURLConnection conn;
        try {
            conn = openPost("/ask/stream", body, request);
        } catch (IOException e) {
            log.warn("AI QA proxy /ask/stream open failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        int code;
        try {
            code = conn.getResponseCode();
        } catch (IOException e) {
            conn.disconnect();
            log.warn("AI QA proxy /ask/stream response code failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        String contentType = conn.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "text/markdown; charset=utf-8";
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(code)
                .contentType(MediaType.parseMediaType(contentType))
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no");

        // 分块写出并 flush，避免 Tomcat/缓冲区攒满整段才下发，前端 fetch 流式读不到增量
        StreamingResponseBody stream = outputStream -> {
            try {
                InputStream in;
                if (code >= 400) {
                    in = conn.getErrorStream();
                    if (in == null) {
                        in = conn.getInputStream();
                    }
                } else {
                    in = conn.getInputStream();
                }
                if (in != null) {
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, n);
                        outputStream.flush();
                    }
                }
            } finally {
                conn.disconnect();
            }
        };

        return builder.body(stream);
    }

    @GetMapping("/health")
    public ResponseEntity<byte[]> health() {
        try {
            return proxyBytes("GET", "/health", null, null, false);
        } catch (IOException e) {
            log.warn("AI QA proxy /health failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new byte[0]);
        }
    }

    private ResponseEntity<byte[]> proxyBytes(String method, String path, byte[] body,
                                              HttpServletRequest request, boolean jsonContentType) throws IOException {
        HttpURLConnection conn = open(method, path, body, request);
        try {
            int code = conn.getResponseCode();
            InputStream in = conn.getErrorStream();
            if (in == null) {
                in = conn.getInputStream();
            }
            byte[] bytes = in != null ? StreamUtils.copyToByteArray(in) : new byte[0];
            ResponseEntity.BodyBuilder b = ResponseEntity.status(code);
            if (jsonContentType) {
                b.contentType(MediaType.APPLICATION_JSON);
            } else if (conn.getContentType() != null) {
                b.contentType(MediaType.parseMediaType(conn.getContentType()));
            }
            return b.body(bytes);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection openPost(String path, byte[] body, HttpServletRequest request) throws IOException {
        return open("POST", path, body, request);
    }

    private HttpURLConnection open(String method, String path, byte[] body,
                                   HttpServletRequest request) throws IOException {
        URL url = new URL(normalizedBase() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(method.equals("POST") && path.contains("stream") ? 120_000 : 30_000);
        if ("POST".equals(method)) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (request != null) {
                String auth = request.getHeader("Authorization");
                if (auth != null) {
                    conn.setRequestProperty("Authorization", auth);
                }
            }
            try (OutputStream os = conn.getOutputStream()) {
                if (body != null && body.length > 0) {
                    os.write(body);
                }
            }
        }
        return conn;
    }

    private String normalizedBase() {
        String b = aiQaBaseUrl == null ? "" : aiQaBaseUrl.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b.isEmpty() ? "http://127.0.0.1:8000" : b;
    }
}
