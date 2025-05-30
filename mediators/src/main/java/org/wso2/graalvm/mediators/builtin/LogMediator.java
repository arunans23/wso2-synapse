package org.wso2.graalvm.mediators.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

/**
 * Log mediator that logs message information.
 * This is one of the most commonly used mediators in integration flows.
 */
public class LogMediator implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(LogMediator.class);
    
    public enum LogLevel {
        SIMPLE, HEADERS, FULL, CUSTOM
    }
    
    private final String name;
    private LogLevel logLevel = LogLevel.SIMPLE;
    private String logMessage;
    private String category = "INFO";
    
    public LogMediator(String name) {
        this.name = name;
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        try {
            String logOutput = buildLogMessage(context);
            
            switch (category.toUpperCase()) {
                case "DEBUG":
                    logger.debug(logOutput);
                    break;
                case "INFO":
                    logger.info(logOutput);
                    break;
                case "WARN":
                    logger.warn(logOutput);
                    break;
                case "ERROR":
                    logger.error(logOutput);
                    break;
                default:
                    logger.info(logOutput);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error in LogMediator", e);
            return false;
        }
    }
    
    private String buildLogMessage(IntegrationContext context) {
        StringBuilder logBuilder = new StringBuilder();
        
        if (logMessage != null && !logMessage.isEmpty()) {
            logBuilder.append(logMessage).append(" - ");
        }
        
        logBuilder.append("Context: ").append(context.getContextId());
        
        switch (logLevel) {
            case SIMPLE:
                logBuilder.append(", Payload: ");
                if (context.getPayload() != null) {
                    logBuilder.append(context.getPayload().toString());
                } else {
                    logBuilder.append("null");
                }
                break;
                
            case HEADERS:
                logBuilder.append(", Headers: ").append(context.getHeaders());
                break;
                
            case FULL:
                logBuilder.append(", Headers: ").append(context.getHeaders());
                logBuilder.append(", Properties: ").append(context.getProperties());
                logBuilder.append(", Payload: ");
                if (context.getPayload() != null) {
                    logBuilder.append(context.getPayload().toString());
                } else {
                    logBuilder.append("null");
                }
                break;
                
            case CUSTOM:
                // For custom logging, just use the log message
                break;
        }
        
        return logBuilder.toString();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void initialize(MediatorConfig config) {
        if (config.hasProperty("level")) {
            try {
                this.logLevel = LogLevel.valueOf(config.getProperty("level").toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid log level: {}, using SIMPLE", config.getProperty("level"));
                this.logLevel = LogLevel.SIMPLE;
            }
        }
        
        if (config.hasProperty("message")) {
            this.logMessage = config.getProperty("message");
        }
        
        if (config.hasProperty("category")) {
            this.category = config.getProperty("category");
        }
    }
    
    // Setters for programmatic configuration
    public LogMediator setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }
    
    public LogMediator setLogMessage(String logMessage) {
        this.logMessage = logMessage;
        return this;
    }
    
    public LogMediator setCategory(String category) {
        this.category = category;
        return this;
    }
}
