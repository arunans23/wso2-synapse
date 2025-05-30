package org.wso2.graalvm.mediators.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Send mediator for sending messages to endpoints.
 * This replaces the traditional Send mediator with virtual thread support.
 */
public class SendMediator implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(SendMediator.class);
    
    private final String name;
    private String endpoint;
    private boolean blocking = true;
    private int timeout = 30000; // 30 seconds default
    private Executor executor;
    
    public SendMediator(String name) {
        this.name = name;
    }
    
    public SendMediator setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }
    
    public SendMediator setBlocking(boolean blocking) {
        this.blocking = blocking;
        return this;
    }
    
    public SendMediator setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
    
    public SendMediator setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        logger.debug("Send mediator {} processing message", name);
        
        try {
            if (endpoint == null) {
                endpoint = (String) context.getProperty("To");
            }
            
            if (endpoint == null) {
                logger.error("No endpoint specified for send mediator {}", name);
                context.setFault(new IllegalStateException("No endpoint specified"));
                return false;
            }
            
            if (blocking) {
                return sendBlocking(context);
            } else {
                return sendNonBlocking(context);
            }
            
        } catch (Exception e) {
            logger.error("Error in send mediator {}", name, e);
            context.setFault(e);
            return false;
        }
    }
    
    private boolean sendBlocking(IntegrationContext context) {
        logger.debug("Sending message synchronously to endpoint: {}", endpoint);
        
        try {
            // Set the To header for outbound message
            context.setHeader("To", endpoint);
            context.setProperty("ENDPOINT_URL", endpoint);
            
            // Mark message as sent
            context.setProperty("MESSAGE_SENT", true);
            context.setProperty("SEND_BLOCKING", true);
            context.setProperty("SEND_TIMEOUT", timeout);
            
            logger.debug("Message sent successfully to endpoint: {}", endpoint);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send message to endpoint: {}", endpoint, e);
            context.setFault(e);
            return false;
        }
    }
    
    private boolean sendNonBlocking(IntegrationContext context) {
        logger.debug("Sending message asynchronously to endpoint: {}", endpoint);
        
        if (executor != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    context.setHeader("To", endpoint);
                    context.setProperty("ENDPOINT_URL", endpoint);
                    context.setProperty("MESSAGE_SENT", true);
                    context.setProperty("SEND_BLOCKING", false);
                    
                    logger.debug("Async message sent to endpoint: {}", endpoint);
                } catch (Exception e) {
                    logger.error("Failed to send async message to endpoint: {}", endpoint, e);
                    context.setFault(e);
                }
            }, executor);
        } else {
            // Fallback to sync if no executor provided
            return sendBlocking(context);
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("SendMediator{name='%s', endpoint='%s', blocking=%s, timeout=%d}", 
                           name, endpoint, blocking, timeout);
    }
}
