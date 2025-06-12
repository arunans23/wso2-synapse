package org.wso2.graalvm.security.filter;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.security.auth.AuthenticationService;
import org.wso2.graalvm.security.auth.AuthorizationService;

import java.util.List;

/**
 * Helidon-based security filter for HTTP requests providing authentication and authorization
 */
public class HelidonSecurityFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(HelidonSecurityFilter.class);
    
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    private final SecurityConfiguration config;
    
    public HelidonSecurityFilter(AuthenticationService authService, 
                                AuthorizationService authzService,
                                SecurityConfiguration config) {
        this.authService = authService;
        this.authzService = authzService;
        this.config = config;
    }
    
    @Override
    public void filter(FilterChain chain, RoutingRequest request, RoutingResponse response) {
        String path = request.path().absolute().path();
        String method = request.prologue().method().text();
        
        // Check if security is enabled for this path
        if (!requiresAuthentication(path)) {
            // Pass through without authentication
            chain.proceed();
            return;
        }
        
        try {
            // Authenticate request
            AuthenticationService.AuthenticationResult authResult = authenticateRequest(request);
            if (!authResult.isSuccess()) {
                sendUnauthorizedResponse(response, authResult.getMessage());
                return;
            }
            
            // Authorize request
            AuthorizationService.AuthorizationResult authzResult = 
                authzService.authorizeRequest(authResult.getRole().name(), method, path);
            
            if (!authzResult.isAllowed()) {
                sendForbiddenResponse(response, authzResult.getMessage());
                return;
            }
            
            // Add user context to request for downstream processing
            // Note: In Helidon, we can't modify request headers, so we use request context instead
            request.context().register("auth.username", authResult.getUsername());
            request.context().register("auth.role", authResult.getRole().name());
            
            logger.debug("Request authorized for user: {} with role: {} to access: {} {}", 
                authResult.getUsername(), authResult.getRole(), method, path);
            
            // Continue with the request
            chain.proceed();
            
        } catch (Exception e) {
            logger.error("Security filter error", e);
            sendErrorResponse(response, "Internal server error");
        }
    }
    
    /**
     * Check if path requires authentication
     */
    private boolean requiresAuthentication(String path) {
        // Public endpoints that don't require authentication
        if (path.equals("/") || 
            path.startsWith("/public/") ||
            path.equals("/health") ||
            (path.equals("/api/health") && config.isHealthCheckPublic())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Authenticate HTTP request using various methods
     */
    private AuthenticationService.AuthenticationResult authenticateRequest(RoutingRequest request) {
        // Try Bearer token first
        String authHeader = request.headers().first(HeaderNames.AUTHORIZATION).orElse(null);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return authService.validateToken(token);
        }
        
        // Try Basic authentication
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return authService.authenticateBasic(authHeader);
        }
        
        // Try API Key in header - using simple string access
        String apiKey = null;
        try {
            // Check if headers contains the API key
            for (var header : request.headers()) {
                if ("X-API-Key".equalsIgnoreCase(header.name())) {
                    apiKey = header.allValues().get(0);
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback - no API key
            apiKey = null;
        }
        if (apiKey != null) {
            return authService.authenticateApiKey(apiKey);
        }
        
        // Try API Key in query parameter
        List<String> apiKeyParams = request.query().all("api_key");
        if (!apiKeyParams.isEmpty()) {
            return authService.authenticateApiKey(apiKeyParams.get(0));
        }
        
        return AuthenticationService.AuthenticationResult.failure("No valid authentication provided");
    }
    
    /**
     * Send 401 Unauthorized response
     */
    private void sendUnauthorizedResponse(RoutingResponse response, String message) {
        response.status(Status.UNAUTHORIZED_401);
        response.headers().set(HeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HeaderNames.WWW_AUTHENTICATE, "Basic realm=\"Micro Integrator\"");
        response.send(message);
        logger.warn("Unauthorized access attempt: {}", message);
    }
    
    /**
     * Send 403 Forbidden response
     */
    private void sendForbiddenResponse(RoutingResponse response, String message) {
        response.status(Status.FORBIDDEN_403);
        response.headers().set(HeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.send(message);
        logger.warn("Forbidden access attempt: {}", message);
    }
    
    /**
     * Send 500 Internal Server Error response
     */
    private void sendErrorResponse(RoutingResponse response, String message) {
        response.status(Status.INTERNAL_SERVER_ERROR_500);
        response.headers().set(HeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.send(message);
    }
    
    /**
     * Security configuration for the filter
     */
    public static class SecurityConfiguration {
        private boolean healthCheckPublic = true;
        private boolean metricsSecured = true;
        private boolean adminSecured = true;
        
        public boolean isHealthCheckPublic() { return healthCheckPublic; }
        public void setHealthCheckPublic(boolean healthCheckPublic) { this.healthCheckPublic = healthCheckPublic; }
        
        public boolean isMetricsSecured() { return metricsSecured; }
        public void setMetricsSecured(boolean metricsSecured) { this.metricsSecured = metricsSecured; }
        
        public boolean isAdminSecured() { return adminSecured; }
        public void setAdminSecured(boolean adminSecured) { this.adminSecured = adminSecured; }
    }
}
