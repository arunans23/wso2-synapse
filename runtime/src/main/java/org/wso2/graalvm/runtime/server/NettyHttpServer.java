package org.wso2.graalvm.runtime.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.config.IntegrationConfiguration;
import org.wso2.graalvm.core.threading.VirtualThreadExecutor;
import org.wso2.graalvm.security.auth.AuthenticationService;
import org.wso2.graalvm.security.auth.AuthorizationService;
import org.wso2.graalvm.security.filter.SecurityFilter;
import org.wso2.graalvm.management.api.ManagementApiHandler;
import org.wso2.graalvm.management.api.ManagementApiHandler.ManagementResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Netty-based HTTP server for the Micro Integrator.
 * Handles HTTP requests and routes them to the integration engine.
 */
public class NettyHttpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);
    
    private final IntegrationConfiguration config;
    private final VirtualThreadExecutor executor;
    private final AuthenticationService authService;
    private final AuthorizationService authzService;
    private final ManagementApiHandler managementHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    public NettyHttpServer(IntegrationConfiguration config, VirtualThreadExecutor executor) {
        this.config = config;
        this.executor = executor;
        this.authService = new AuthenticationService();
        this.authzService = new AuthorizationService();
        this.managementHandler = new ManagementApiHandler(config);
    }
    
    public void start() throws Exception {
        logger.info("Starting Netty HTTP server");
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(1048576)); // 1MB max
                            
                            // Add security filter
                            SecurityFilter.SecurityConfiguration securityConfig = 
                                new SecurityFilter.SecurityConfiguration();
                            pipeline.addLast(new SecurityFilter(authService, authzService, securityConfig));
                            
                            pipeline.addLast(new HttpServerHandler(executor, managementHandler));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Bind to the port
            ChannelFuture future = bootstrap.bind(config.getServer().getHost(), config.getServer().getPort()).sync();
            serverChannel = future.channel();
            
            logger.info("Netty HTTP server started on {}:{}", 
                       config.getServer().getHost(), config.getServer().getPort());
            
        } catch (Exception e) {
            logger.error("Failed to start Netty HTTP server", e);
            shutdown();
            throw e;
        }
    }
    
    public void stop() {
        logger.info("Stopping Netty HTTP server");
        shutdown();
    }
    
    private void shutdown() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while closing server channel", e);
            Thread.currentThread().interrupt();
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
        }
    }
    
    /**
     * HTTP request handler that processes integration requests using virtual threads.
     */
    private static class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        
        private final VirtualThreadExecutor executor;
        private final ManagementApiHandler managementHandler;
        
        public HttpServerHandler(VirtualThreadExecutor executor, ManagementApiHandler managementHandler) {
            this.executor = executor;
            this.managementHandler = managementHandler;
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            // Process the request asynchronously using virtual threads
            CompletableFuture<HttpResponse> responseFuture = CompletableFuture.supplyAsync(() -> {
                return processRequest(request);
            }, executor);
            
            responseFuture.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    logger.error("Error processing request", throwable);
                    response = createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                                                 "Internal server error");
                }
                
                // Send response back to client
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            });
        }
        
        private HttpResponse processRequest(FullHttpRequest request) {
            String uri = request.uri();
            HttpMethod method = request.method();
            
            logger.info("Processing {} request to {}", method, uri);
            
            // Parse query parameters
            Map<String, String> queryParams = parseQueryParams(uri);
            String path = uri.split("\\?")[0]; // Remove query parameters from path
            
            // Route to management API for admin endpoints
            if (path.startsWith("/api/health") || path.startsWith("/api/metrics") || 
                path.startsWith("/api/info") || path.startsWith("/api/admin")) {
                ManagementApiHandler.ManagementResponse mgmtResponse = 
                    managementHandler.handleRequest(path, method.name(), queryParams);
                return convertManagementResponse(mgmtResponse);
            }
            
            // Basic health endpoint (public)
            if (path.equals("/health")) {
                return createJsonResponse(HttpResponseStatus.OK, "{\"status\":\"UP\",\"service\":\"micro-integrator\"}");
            }
            
            // Integration endpoints
            if (path.startsWith("/services/") || path.startsWith("/api/")) {
                return processIntegrationRequest(request);
            }
            
            return createErrorResponse(HttpResponseStatus.NOT_FOUND, "Resource not found");
        }
        
        private HttpResponse processIntegrationRequest(FullHttpRequest request) {
            // Placeholder for integration engine processing
            // This would delegate to the integration engine in a real implementation
            String responseBody = "{\"message\":\"Integration request processed\",\"path\":\"" + request.uri() + "\"}";
            return createJsonResponse(HttpResponseStatus.OK, responseBody);
        }
        
        private HttpResponse convertManagementResponse(ManagementResponse mgmtResponse) {
            HttpResponseStatus status = HttpResponseStatus.valueOf(mgmtResponse.getStatusCode());
            return createJsonResponse(status, mgmtResponse.getBody());
        }
        
        private Map<String, String> parseQueryParams(String uri) {
            Map<String, String> params = new HashMap<>();
            if (uri.contains("?")) {
                String queryString = uri.substring(uri.indexOf("?") + 1);
                String[] pairs = queryString.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length == 2) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            return params;
        }
        
        private HttpResponse createJsonResponse(HttpResponseStatus status, String content) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, 
                io.netty.buffer.Unpooled.copiedBuffer(content, io.netty.util.CharsetUtil.UTF_8));
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            
            return response;
        }
        
        private HttpResponse createErrorResponse(HttpResponseStatus status, String message) {
            String errorJson = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.code());
            return createJsonResponse(status, errorJson);
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Exception in HTTP handler", cause);
            ctx.close();
        }
    }
}
