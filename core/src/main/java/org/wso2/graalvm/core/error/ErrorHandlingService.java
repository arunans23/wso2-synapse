package org.wso2.graalvm.core.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive error handling framework for the Micro Integrator
 */
public class ErrorHandlingService {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingService.class);
    
    private final Map<String, ErrorHandler> errorHandlers = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final ErrorMetrics metrics = new ErrorMetrics();
    
    public ErrorHandlingService() {
        // Register default error handlers
        registerHandler("INTEGRATION_ERROR", new IntegrationErrorHandler());
        registerHandler("TRANSPORT_ERROR", new TransportErrorHandler());
        registerHandler("SECURITY_ERROR", new SecurityErrorHandler());
        registerHandler("SYSTEM_ERROR", new SystemErrorHandler());
    }
    
    /**
     * Handle an error with the appropriate handler
     */
    public ErrorResult handleError(String errorType, Throwable error, ErrorContext context) {
        ErrorHandler handler = errorHandlers.get(errorType);
        if (handler == null) {
            handler = errorHandlers.get("SYSTEM_ERROR");
        }
        
        // Record error metrics
        recordError(errorType, error);
        
        try {
            return handler.handleError(error, context);
        } catch (Exception e) {
            logger.error("Error handler failed for type: " + errorType, e);
            return ErrorResult.failure("Error handler failed: " + e.getMessage());
        }
    }
    
    /**
     * Register a custom error handler
     */
    public void registerHandler(String errorType, ErrorHandler handler) {
        errorHandlers.put(errorType, handler);
        logger.info("Registered error handler for type: {}", errorType);
    }
    
    /**
     * Record error for metrics
     */
    private void recordError(String errorType, Throwable error) {
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        metrics.recordError(errorType, error);
    }
    
    /**
     * Get error statistics
     */
    public ErrorStatistics getErrorStatistics() {
        return new ErrorStatistics(errorCounts, metrics);
    }
    
    /**
     * Error handler interface
     */
    public interface ErrorHandler {
        ErrorResult handleError(Throwable error, ErrorContext context);
    }
    
    /**
     * Integration error handler
     */
    private static class IntegrationErrorHandler implements ErrorHandler {
        @Override
        public ErrorResult handleError(Throwable error, ErrorContext context) {
            logger.warn("Integration error in flow: {}", context.getFlowName(), error);
            
            // Check if it's a recoverable error
            if (isRecoverable(error)) {
                return ErrorResult.retry("Recoverable integration error", 3);
            } else {
                return ErrorResult.failure("Integration failed: " + error.getMessage());
            }
        }
        
        private boolean isRecoverable(Throwable error) {
            return error instanceof java.net.ConnectException ||
                   error instanceof java.net.SocketTimeoutException ||
                   error.getMessage().contains("timeout");
        }
    }
    
    /**
     * Transport error handler
     */
    private static class TransportErrorHandler implements ErrorHandler {
        @Override
        public ErrorResult handleError(Throwable error, ErrorContext context) {
            logger.error("Transport error", error);
            
            if (error instanceof java.net.ConnectException) {
                return ErrorResult.retry("Connection failed - will retry", 5);
            } else if (error instanceof java.net.SocketTimeoutException) {
                return ErrorResult.retry("Request timeout - will retry", 3);
            } else {
                return ErrorResult.failure("Transport error: " + error.getMessage());
            }
        }
    }
    
    /**
     * Security error handler
     */
    private static class SecurityErrorHandler implements ErrorHandler {
        @Override
        public ErrorResult handleError(Throwable error, ErrorContext context) {
            logger.warn("Security error: {}", error.getMessage());
            
            // Security errors are generally not retryable
            if (error instanceof SecurityException) {
                return ErrorResult.failure("Security violation: " + error.getMessage());
            } else {
                return ErrorResult.failure("Authentication/Authorization failed");
            }
        }
    }
    
    /**
     * System error handler
     */
    private static class SystemErrorHandler implements ErrorHandler {
        @Override
        public ErrorResult handleError(Throwable error, ErrorContext context) {
            logger.error("System error", error);
            
            if (error instanceof OutOfMemoryError) {
                return ErrorResult.critical("Out of memory - system unstable");
            } else if (error instanceof StackOverflowError) {
                return ErrorResult.failure("Stack overflow - possible infinite recursion");
            } else {
                return ErrorResult.failure("System error: " + error.getMessage());
            }
        }
    }
    
    /**
     * Error context containing relevant information
     */
    public static class ErrorContext {
        private final String flowName;
        private final String mediatorName;
        private final Map<String, Object> properties;
        
        public ErrorContext(String flowName, String mediatorName, Map<String, Object> properties) {
            this.flowName = flowName;
            this.mediatorName = mediatorName;
            this.properties = properties != null ? properties : new ConcurrentHashMap<>();
        }
        
        public String getFlowName() { return flowName; }
        public String getMediatorName() { return mediatorName; }
        public Map<String, Object> getProperties() { return properties; }
    }
    
    /**
     * Error handling result
     */
    public static class ErrorResult {
        private final ResultType type;
        private final String message;
        private final int retryCount;
        
        private ErrorResult(ResultType type, String message, int retryCount) {
            this.type = type;
            this.message = message;
            this.retryCount = retryCount;
        }
        
        public static ErrorResult success(String message) {
            return new ErrorResult(ResultType.SUCCESS, message, 0);
        }
        
        public static ErrorResult failure(String message) {
            return new ErrorResult(ResultType.FAILURE, message, 0);
        }
        
        public static ErrorResult retry(String message, int retryCount) {
            return new ErrorResult(ResultType.RETRY, message, retryCount);
        }
        
        public static ErrorResult critical(String message) {
            return new ErrorResult(ResultType.CRITICAL, message, 0);
        }
        
        public ResultType getType() { return type; }
        public String getMessage() { return message; }
        public int getRetryCount() { return retryCount; }
        
        public enum ResultType {
            SUCCESS, FAILURE, RETRY, CRITICAL
        }
    }
    
    /**
     * Error metrics collection
     */
    private static class ErrorMetrics {
        private final AtomicLong totalErrors = new AtomicLong(0);
        private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
        private volatile Instant lastError;
        
        void recordError(String errorType, Throwable error) {
            totalErrors.incrementAndGet();
            errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
            lastError = Instant.now();
        }
        
        long getTotalErrors() { return totalErrors.get(); }
        Map<String, AtomicLong> getErrorsByType() { return errorsByType; }
        Instant getLastError() { return lastError; }
    }
    
    /**
     * Error statistics
     */
    public static class ErrorStatistics {
        private final Map<String, AtomicLong> errorCounts;
        private final ErrorMetrics metrics;
        
        ErrorStatistics(Map<String, AtomicLong> errorCounts, ErrorMetrics metrics) {
            this.errorCounts = errorCounts;
            this.metrics = metrics;
        }
        
        public long getTotalErrors() { return metrics.getTotalErrors(); }
        public Map<String, AtomicLong> getErrorCounts() { return errorCounts; }
        public Instant getLastError() { return metrics.getLastError(); }
    }
}
