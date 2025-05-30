package org.wso2.graalvm.cli.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client utility for CLI operations
 */
public class HttpClientUtil {
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    /**
     * Perform GET request to management API
     */
    public static ApiResponse get(String url, String username, String password) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();
            
            if (username != null && password != null) {
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder()
                    .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            return new ApiResponse(response.statusCode(), response.body());
            
        } catch (IOException | InterruptedException e) {
            return new ApiResponse(-1, "Connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform POST request to management API
     */
    public static ApiResponse post(String url, String body, String username, String password) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
            
            if (username != null && password != null) {
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder()
                    .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            }
            
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            return new ApiResponse(response.statusCode(), response.body());
            
        } catch (IOException | InterruptedException e) {
            return new ApiResponse(-1, "Connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Check if server is accessible
     */
    public static boolean isServerAccessible(String host, int port) {
        try {
            String url = "http://" + host + ":" + port + "/api/health";
            ApiResponse response = get(url, null, null);
            return response.statusCode >= 200 && response.statusCode < 300;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * API response wrapper
     */
    public static class ApiResponse {
        private final int statusCode;
        private final String body;
        
        public ApiResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
    }
}
