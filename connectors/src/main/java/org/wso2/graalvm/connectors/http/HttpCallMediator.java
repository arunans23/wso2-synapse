package org.wso2.graalvm.connectors.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;
import org.wso2.graalvm.transports.http.HttpClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP Call mediator for making outbound HTTP requests.
 * This replaces the traditional Call mediator with virtual thread support.
 */
public class HttpCallMediator implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpCallMediator.class);
    
    private final String name;
    private final HttpClient httpClient;
    private String endpoint;
    private String method = "POST";
    private boolean blocking = true;
    
    public HttpCallMediator(String name, Executor executor) {
        this.name = name;
        this.httpClient = new HttpClient(executor);
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        try {
            String targetUrl = resolveEndpoint(context);
            if (targetUrl == null) {
                logger.error("No endpoint specified for HTTP call");
                return false;
            }
            
            logger.debug("Making HTTP {} request to {}", method, targetUrl);
            
            // Create HTTP request from context
            HttpClient.HttpRequest request = HttpClient.fromContext(context, targetUrl, method);
            
            if (blocking) {
                // Synchronous call
                HttpClient.HttpResponse response = httpClient.sendSync(request);
                updateContextWithResponse(context, response);
                return response.isSuccessful();
            } else {
                // Asynchronous call - fire and forget
                CompletableFuture<HttpClient.HttpResponse> future = httpClient.sendAsync(request);
                future.whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        logger.error("Async HTTP call failed", throwable);
                    } else {
                        logger.debug("Async HTTP call completed with status {}", response.getStatusCode());
                    }
                });
                return true;
            }
            
        } catch (Exception e) {
            logger.error("HTTP call mediator failed", e);
            context.setFault(e);
            return false;
        }
    }
    
    private String resolveEndpoint(IntegrationContext context) {
        if (endpoint != null) {
            // Support property replacement in endpoint URL
            if (endpoint.contains("$ctx:")) {
                return endpoint.replace("$ctx:", "")
                              .replace("{", "")
                              .replace("}", "");
            }
            return endpoint;
        }
        
        // Try to get endpoint from context
        return context.getProperty("endpoint", String.class);
    }
    
    private void updateContextWithResponse(IntegrationContext context, HttpClient.HttpResponse response) {
        // Update context with response
        context.setPayload(response.getBodyAsString());
        
        // Set response headers as properties
        response.getHeaders().forEach((key, value) -> 
            context.setProperty("response.header." + key, value));
        
        // Set status code
        context.setProperty("response.status", response.getStatusCode());
        context.setProperty("response.status.message", response.getStatusMessage());
        
        logger.debug("Updated context with HTTP response: status={}, headers={}", 
                    response.getStatusCode(), response.getHeaders().size());
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void initialize(MediatorConfig config) {
        if (config.hasProperty("endpoint")) {
            this.endpoint = config.getProperty("endpoint");
        }
        
        if (config.hasProperty("method")) {
            this.method = config.getProperty("method", "POST").toUpperCase();
        }
        
        if (config.hasProperty("blocking")) {
            this.blocking = Boolean.parseBoolean(config.getProperty("blocking", "true"));
        }
    }
    
    // Setters for programmatic configuration
    public HttpCallMediator setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }
    
    public HttpCallMediator setMethod(String method) {
        this.method = method;
        return this;
    }
    
    public HttpCallMediator setBlocking(boolean blocking) {
        this.blocking = blocking;
        return this;
    }
}
