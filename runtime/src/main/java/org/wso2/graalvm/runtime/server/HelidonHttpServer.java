package org.wso2.graalvm.runtime.server;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.config.IntegrationConfiguration;
import org.wso2.graalvm.core.threading.VirtualThreadExecutor;
import org.wso2.graalvm.management.api.ManagementApiHandler;
import org.wso2.graalvm.management.api.ManagementApiHandler.ManagementResponse;
import org.wso2.graalvm.security.auth.AuthenticationService;
import org.wso2.graalvm.security.auth.AuthorizationService;
import org.wso2.graalvm.security.filter.HelidonSecurityFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Helidon-based HTTP server for the Micro Integrator.
 * Handles HTTP requests and routes them to the integration engine.
 */
public class HelidonHttpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(HelidonHttpServer.class);
    
    private final IntegrationConfiguration config;
    private final VirtualThreadExecutor executor;
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    private final ManagementApiHandler managementHandler;
    private WebServer webServer;
    
    public HelidonHttpServer(IntegrationConfiguration config, VirtualThreadExecutor executor) {
        this.config = config;
        this.executor = executor;
        this.authService = new AuthenticationService();
        this.authzService = new AuthorizationService();
        this.managementHandler = new ManagementApiHandler(config);
    }
    
    public void start() throws Exception {
        logger.info("Starting Helidon HTTP server");
        
        try {
            // Create security filter
            HelidonSecurityFilter.SecurityConfiguration securityConfig = 
                new HelidonSecurityFilter.SecurityConfiguration();
            HelidonSecurityFilter securityFilter = new HelidonSecurityFilter(
                authService, authzService, securityConfig);
            
            webServer = WebServer.builder()
                    .host(config.getServer().getHost())
                    .port(config.getServer().getPort())
                    .routing(builder -> builder
                        .addFilter(securityFilter)
                        .register(this::setupRoutes))
                    .build()
                    .start();
            
            logger.info("Helidon HTTP server started on {}:{}", 
                       config.getServer().getHost(), config.getServer().getPort());
            
        } catch (Exception e) {
            logger.error("Failed to start Helidon HTTP server", e);
            throw e;
        }
    }
    
    public void stop() {
        logger.info("Stopping Helidon HTTP server");
        if (webServer != null) {
            webServer.stop();
        }
    }
    
    private void setupRoutes(HttpRules rules) {
        // Health endpoint (public)
        rules.get("/health", this::handleHealthCheck);
        
        // Management API endpoints (with security)
        rules.any("/api/health", this::handleManagementRequest);
        rules.any("/api/metrics", this::handleManagementRequest);
        rules.any("/api/info", this::handleManagementRequest);
        rules.any("/api/admin/*", this::handleManagementRequest);
        
        // Integration endpoints
        rules.any("/services/*", this::handleIntegrationRequest);
        rules.any("/api/*", this::handleIntegrationRequest);
        
        // Catch-all for 404
        rules.any("/*", this::handleNotFound);
    }
    
    private void handleHealthCheck(ServerRequest request, ServerResponse response) {
        String healthResponse = "{\"status\":\"UP\",\"service\":\"micro-integrator\"}";
        response.headers().set(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_JSON.text());
        response.send(healthResponse);
    }
    
    private void handleManagementRequest(ServerRequest request, ServerResponse response) {
        // Process management request asynchronously using virtual threads
        CompletableFuture.supplyAsync(() -> {
            try {
                String path = request.path().absolute().path();
                String method = request.prologue().method().text();
                Map<String, String> queryParams = extractQueryParams(request);
                
                logger.info("Processing management {} request to {}", method, path);
                
                // Delegate to management handler
                ManagementResponse mgmtResponse = managementHandler.handleRequest(path, method, queryParams);
                return mgmtResponse;
                
            } catch (Exception e) {
                logger.error("Error processing management request", e);
                return new ManagementResponse(500, "application/json", "{\"error\":\"Internal server error\"}");
            }
        }, executor).whenComplete((mgmtResponse, throwable) -> {
            if (throwable != null) {
                logger.error("Error in management request processing", throwable);
                sendErrorResponse(response, Status.INTERNAL_SERVER_ERROR_500, "Internal server error");
            } else {
                sendManagementResponse(response, mgmtResponse);
            }
        });
    }
    
    private void handleIntegrationRequest(ServerRequest request, ServerResponse response) {
        // Process integration request asynchronously using virtual threads
        CompletableFuture.supplyAsync(() -> {
            try {
                String path = request.path().absolute().path();
                String method = request.prologue().method().text();
                
                logger.info("Processing integration {} request to {}", method, path);
                
                // Placeholder for integration engine processing
                // This would delegate to the integration engine in a real implementation
                String responseBody = "{\"message\":\"Integration request processed\",\"path\":\"" + path + "\"}";
                return responseBody;
                
            } catch (Exception e) {
                logger.error("Error processing integration request", e);
                return "{\"error\":\"Internal server error\"}";
            }
        }, executor).whenComplete((responseBody, throwable) -> {
            if (throwable != null) {
                logger.error("Error in integration request processing", throwable);
                sendErrorResponse(response, Status.INTERNAL_SERVER_ERROR_500, "Internal server error");
            } else {
                sendJsonResponse(response, Status.OK_200, responseBody);
            }
        });
    }
    
    private void handleNotFound(ServerRequest request, ServerResponse response) {
        sendErrorResponse(response, Status.NOT_FOUND_404, "Resource not found");
    }
    
    private Map<String, String> extractQueryParams(ServerRequest request) {
        Map<String, String> params = new HashMap<>();
        for (String paramName : request.query().names()) {
            String value = request.query().first(paramName).orElse(null);
            if (value != null) {
                params.put(paramName, value);
            }
        }
        return params;
    }
    
    private void sendManagementResponse(ServerResponse response, ManagementResponse mgmtResponse) {
        Status status = Status.create(mgmtResponse.getStatusCode());
        sendJsonResponse(response, status, mgmtResponse.getBody());
    }
    
    private void sendJsonResponse(ServerResponse response, Status status, String content) {
        response.status(status);
        response.headers().set(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_JSON.text());
        response.send(content);
    }
    
    private void sendErrorResponse(ServerResponse response, Status status, String message) {
        String errorJson = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.code());
        sendJsonResponse(response, status, errorJson);
    }
}
