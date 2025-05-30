package org.wso2.graalvm.management.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.config.IntegrationConfiguration;
import org.wso2.graalvm.management.health.HealthCheckService;
import org.wso2.graalvm.management.metrics.MetricsService;

import java.util.HashMap;
import java.util.Map;

/**
 * Management API handler for exposing health checks, metrics, and management endpoints.
 * This provides REST endpoints for monitoring and managing the Micro Integrator.
 */
public class ManagementApiHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ManagementApiHandler.class);
    
    private final HealthCheckService healthCheckService;
    private final MetricsService metricsService;
    private final IntegrationConfiguration config;
    
    public ManagementApiHandler(IntegrationConfiguration config) {
        this.config = config;
        this.healthCheckService = new HealthCheckService(config);
        this.metricsService = new MetricsService();
        logger.info("Management API handler initialized");
    }
    
    /**
     * Handle management API requests
     */
    public ManagementResponse handleRequest(String path, String method, Map<String, String> queryParams) {
        logger.debug("Handling management request: {} {}", method, path);
        
        try {
            return switch (path) {
                case "/health" -> handleHealth(method, queryParams);
                case "/health/detailed" -> handleDetailedHealth(method, queryParams);
                case "/health/ready" -> handleReadiness(method, queryParams);
                case "/health/live" -> handleLiveness(method, queryParams);
                case "/metrics" -> handleMetrics(method, queryParams);
                case "/metrics/requests" -> handleRequestMetrics(method, queryParams);
                case "/metrics/integrations" -> handleIntegrationMetrics(method, queryParams);
                case "/info" -> handleInfo(method, queryParams);
                case "/env" -> handleEnvironment(method, queryParams);
                default -> handleNotFound(path);
            };
        } catch (Exception e) {
            logger.error("Error handling management request: {} {}", method, path, e);
            return new ManagementResponse(500, "application/json", 
                "{\"error\":\"Internal server error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private ManagementResponse handleHealth(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = healthCheckService.getHealthStatus();
        int statusCode = healthCheckService.isHealthy() ? 200 : 503;
        
        return new ManagementResponse(statusCode, "application/json", response);
    }
    
    private ManagementResponse handleDetailedHealth(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = healthCheckService.getDetailedHealth();
        int statusCode = healthCheckService.isHealthy() ? 200 : 503;
        
        return new ManagementResponse(statusCode, "application/json", response);
    }
    
    private ManagementResponse handleReadiness(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = healthCheckService.getReadinessStatus();
        return new ManagementResponse(200, "application/json", response);
    }
    
    private ManagementResponse handleLiveness(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = healthCheckService.getLivenessStatus();
        int statusCode = healthCheckService.isHealthy() ? 200 : 503;
        
        return new ManagementResponse(statusCode, "application/json", response);
    }
    
    private ManagementResponse handleMetrics(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = metricsService.getAllMetrics();
        return new ManagementResponse(200, "application/json", response);
    }
    
    private ManagementResponse handleRequestMetrics(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = metricsService.getRequestMetrics();
        return new ManagementResponse(200, "application/json", response);
    }
    
    private ManagementResponse handleIntegrationMetrics(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        String response = metricsService.getIntegrationMetrics();
        return new ManagementResponse(200, "application/json", response);
    }
    
    private ManagementResponse handleInfo(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", "WSO2 Micro Integrator - GraalVM Edition");
        info.put("version", "1.0.0-SNAPSHOT");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("virtualThreadsSupported", isVirtualThreadsSupported());
        info.put("graalVm", isGraalVM());
        
        try {
            String response = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(info);
            return new ManagementResponse(200, "application/json", response);
        } catch (Exception e) {
            return new ManagementResponse(500, "application/json", 
                "{\"error\":\"Failed to generate info\"}");
        }
    }
    
    private ManagementResponse handleEnvironment(String method, Map<String, String> queryParams) {
        if (!"GET".equals(method)) {
            return new ManagementResponse(405, "application/json", 
                "{\"error\":\"Method not allowed\",\"allowed\":[\"GET\"]}");
        }
        
        Map<String, Object> env = new HashMap<>();
        env.put("javaHome", System.getProperty("java.home"));
        env.put("javaVersion", System.getProperty("java.version"));
        env.put("osName", System.getProperty("os.name"));
        env.put("osVersion", System.getProperty("os.version"));
        env.put("userDir", System.getProperty("user.dir"));
        env.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        
        // Add configuration info (non-sensitive)
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("serverPort", config.getServer().getPort());
        configInfo.put("managementPort", config.getServer().getManagementPort());
        configInfo.put("virtualThreadsEnabled", config.getThreading().isVirtualThreadsEnabled());
        env.put("configuration", configInfo);
        
        try {
            String response = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(env);
            return new ManagementResponse(200, "application/json", response);
        } catch (Exception e) {
            return new ManagementResponse(500, "application/json", 
                "{\"error\":\"Failed to generate environment info\"}");
        }
    }
    
    private ManagementResponse handleNotFound(String path) {
        String response = "{\"error\":\"Not found\",\"path\":\"" + path + "\"," +
                         "\"availableEndpoints\":[" +
                         "\"/health\",\"/health/detailed\",\"/health/ready\",\"/health/live\"," +
                         "\"/metrics\",\"/metrics/requests\",\"/metrics/integrations\"," +
                         "\"/info\",\"/env\"]}";
        return new ManagementResponse(404, "application/json", response);
    }
    
    private boolean isVirtualThreadsSupported() {
        try {
            Class.forName("java.lang.VirtualThread");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private boolean isGraalVM() {
        return System.getProperty("java.vm.name", "").contains("GraalVM");
    }
    
    // Getters for services (for external access)
    public HealthCheckService getHealthCheckService() {
        return healthCheckService;
    }
    
    public MetricsService getMetricsService() {
        return metricsService;
    }
    
    /**
     * Response object for management API
     */
    public static class ManagementResponse {
        private final int statusCode;
        private final String contentType;
        private final String body;
        
        public ManagementResponse(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public String getBody() {
            return body;
        }
    }
}
