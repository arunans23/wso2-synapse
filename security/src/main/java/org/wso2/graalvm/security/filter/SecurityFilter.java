package org.wso2.graalvm.security.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.wso2.graalvm.security.auth.AuthenticationService;
import org.wso2.graalvm.security.auth.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Netty-based security filter for HTTP requests providing authentication and authorization
 */
public class SecurityFilter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    private final SecurityConfiguration config;
    
    public SecurityFilter(AuthenticationService authService, 
                         AuthorizationService authzService,
                         SecurityConfiguration config) {
        this.authService = authService;
        this.authzService = authzService;
        this.config = config;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            
            // Check if security is enabled for this path
            String path = request.uri();
            if (!requiresAuthentication(path)) {
                // Pass through without authentication
                super.channelRead(ctx, msg);
                return;
            }
            
            // Authenticate request
            AuthenticationService.AuthenticationResult authResult = authenticateRequest(request);
            if (!authResult.isSuccess()) {
                sendUnauthorizedResponse(ctx, authResult.getMessage());
                return;
            }
            
            // Authorize request
            String method = request.method().name();
            AuthorizationService.AuthorizationResult authzResult = 
                authzService.authorizeRequest(authResult.getRole().name(), method, path);
            
            if (!authzResult.isAllowed()) {
                sendForbiddenResponse(ctx, authzResult.getMessage());
                return;
            }
            
            // Add user context to request headers
            request.headers().set("X-User", authResult.getUsername());
            request.headers().set("X-Role", authResult.getRole().name());
            
            logger.debug("Request authorized for user: {} with role: {} to access: {} {}", 
                authResult.getUsername(), authResult.getRole(), method, path);
            
            // Continue with the request
            super.channelRead(ctx, msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }
    
    /**
     * Check if path requires authentication
     */
    private boolean requiresAuthentication(String path) {
        // Public endpoints that don't require authentication
        if (path.equals("/") || 
            path.startsWith("/public/") ||
            path.equals("/api/health") && config.isHealthCheckPublic()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Authenticate HTTP request using various methods
     */
    private AuthenticationService.AuthenticationResult authenticateRequest(FullHttpRequest request) {
        // Try Bearer token first
        String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return authService.validateToken(token);
        }
        
        // Try Basic authentication
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return authService.authenticateBasic(authHeader);
        }
        
        // Try API Key in header
        String apiKey = request.headers().get("X-API-Key");
        if (apiKey != null) {
            return authService.authenticateApiKey(apiKey);
        }
        
        // Try API Key in query parameter
        QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
        if (queryDecoder.parameters().containsKey("api_key")) {
            String queryApiKey = queryDecoder.parameters().get("api_key").get(0);
            return authService.authenticateApiKey(queryApiKey);
        }
        
        return AuthenticationService.AuthenticationResult.failure("No valid authentication provided");
    }
    
    /**
     * Send 401 Unauthorized response
     */
    private void sendUnauthorizedResponse(ChannelHandlerContext ctx, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.UNAUTHORIZED,
            ctx.alloc().buffer().writeBytes(message.getBytes(StandardCharsets.UTF_8))
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"Micro Integrator\"");
        
        ctx.writeAndFlush(response);
        logger.warn("Unauthorized access attempt: {}", message);
    }
    
    /**
     * Send 403 Forbidden response
     */
    private void sendForbiddenResponse(ChannelHandlerContext ctx, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.FORBIDDEN,
            ctx.alloc().buffer().writeBytes(message.getBytes(StandardCharsets.UTF_8))
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response);
        logger.warn("Forbidden access attempt: {}", message);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Security filter error", cause);
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            ctx.alloc().buffer().writeBytes("Internal server error".getBytes(StandardCharsets.UTF_8))
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response);
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
