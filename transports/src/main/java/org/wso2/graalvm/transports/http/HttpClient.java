package org.wso2.graalvm.transports.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP client for making outbound HTTP requests.
 * Uses virtual threads for non-blocking operations.
 */
public class HttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    
    private final Executor executor;
    
    public HttpClient(Executor executor) {
        this.executor = executor;
    }
    
    /**
     * Sends an HTTP request asynchronously.
     */
    public CompletableFuture<HttpResponse> sendAsync(HttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendSync(request);
            } catch (Exception e) {
                logger.error("Failed to send HTTP request", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Sends an HTTP request synchronously.
     */
    public HttpResponse sendSync(HttpRequest request) throws Exception {
        logger.debug("Sending HTTP {} request to {}", request.getMethod(), request.getUrl());
        
        // In a real implementation, this would use a proper HTTP client
        // For now, we'll create a mock response
        return createMockResponse(request);
    }
    
    private HttpResponse createMockResponse(HttpRequest request) {
        // This is a placeholder implementation
        // In a real scenario, you'd use an HTTP client library
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Server", "WSO2-MI-GraalVM");
        
        String responseBody = String.format(
            "{\"message\":\"Mock response\",\"originalUrl\":\"%s\",\"method\":\"%s\"}", 
            request.getUrl(), request.getMethod());
        
        return new HttpResponse(200, "OK", headers, responseBody.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Creates an HTTP request from an IntegrationContext.
     */
    public static HttpRequest fromContext(IntegrationContext context, String url, String method) {
        Map<String, String> headers = new HashMap<>();
        context.getHeaders().forEach((key, value) -> {
            if (value instanceof String) {
                headers.put(key, (String) value);
            }
        });
        
        byte[] body = null;
        if (context.getPayload() != null) {
            if (context.getPayload() instanceof String) {
                body = ((String) context.getPayload()).getBytes(StandardCharsets.UTF_8);
            } else if (context.getPayload() instanceof byte[]) {
                body = (byte[]) context.getPayload();
            }
        }
        
        return new HttpRequest(method, url, headers, body);
    }
    
    /**
     * HTTP Request representation.
     */
    public static class HttpRequest {
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final byte[] body;
        
        public HttpRequest(String method, String url, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.url = url;
            this.headers = headers != null ? headers : new HashMap<>();
            this.body = body;
        }
        
        public String getMethod() { return method; }
        public String getUrl() { return url; }
        public Map<String, String> getHeaders() { return headers; }
        public byte[] getBody() { return body; }
        
        public String getBodyAsString() {
            return body != null ? new String(body, StandardCharsets.UTF_8) : null;
        }
    }
    
    /**
     * HTTP Response representation.
     */
    public static class HttpResponse {
        private final int statusCode;
        private final String statusMessage;
        private final Map<String, String> headers;
        private final byte[] body;
        
        public HttpResponse(int statusCode, String statusMessage, Map<String, String> headers, byte[] body) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.headers = headers != null ? headers : new HashMap<>();
            this.body = body;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getStatusMessage() { return statusMessage; }
        public Map<String, String> getHeaders() { return headers; }
        public byte[] getBody() { return body; }
        
        public String getBodyAsString() {
            return body != null ? new String(body, StandardCharsets.UTF_8) : null;
        }
        
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
