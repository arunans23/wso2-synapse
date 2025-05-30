package org.wso2.graalvm.management.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Metrics collection service for monitoring application performance.
 * Provides basic metrics for request counting, timing, and resource usage.
 */
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Instant startTime = Instant.now();
    
    // Request metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final DoubleAdder totalResponseTime = new DoubleAdder();
    
    // Integration flow metrics
    private final AtomicLong totalIntegrationsExecuted = new AtomicLong(0);
    private final AtomicLong failedIntegrations = new AtomicLong(0);
    private final DoubleAdder totalIntegrationTime = new DoubleAdder();
    
    // Mediator metrics
    private final Map<String, AtomicLong> mediatorExecutionCount = new HashMap<>();
    private final Map<String, DoubleAdder> mediatorExecutionTime = new HashMap<>();
    
    public MetricsService() {
        logger.info("Metrics service initialized");
    }
    
    /**
     * Record a request
     */
    public void recordRequest(boolean successful, long responseTimeMs) {
        totalRequests.incrementAndGet();
        totalResponseTime.add(responseTimeMs);
        
        if (successful) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
    }
    
    /**
     * Record integration execution
     */
    public void recordIntegration(boolean successful, long executionTimeMs) {
        totalIntegrationsExecuted.incrementAndGet();
        totalIntegrationTime.add(executionTimeMs);
        
        if (!successful) {
            failedIntegrations.incrementAndGet();
        }
    }
    
    /**
     * Record mediator execution
     */
    public void recordMediatorExecution(String mediatorName, long executionTimeMs) {
        mediatorExecutionCount.computeIfAbsent(mediatorName, k -> new AtomicLong(0))
                              .incrementAndGet();
        mediatorExecutionTime.computeIfAbsent(mediatorName, k -> new DoubleAdder())
                             .add(executionTimeMs);
    }
    
    /**
     * Get all metrics as JSON
     */
    public String getAllMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // System metrics
            metrics.put("system", getSystemMetrics());
            
            // Request metrics
            metrics.put("requests", getRequestMetrics());
            
            // Integration metrics
            metrics.put("integrations", getIntegrationMetrics());
            
            // Mediator metrics
            metrics.put("mediators", getMediatorMetrics());
            
            // Runtime metrics
            metrics.put("runtime", getRuntimeMetrics());
            
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            logger.error("Failed to generate metrics", e);
            return "{\"error\":\"Failed to generate metrics\"}";
        }
    }
    
    /**
     * Get request-specific metrics
     */
    public String getRequestMetrics() {
        try {
            return objectMapper.writeValueAsString(getRequestMetricsMap());
        } catch (Exception e) {
            logger.error("Failed to generate request metrics", e);
            return "{\"error\":\"Failed to generate request metrics\"}";
        }
    }
    
    /**
     * Get integration-specific metrics
     */
    public String getIntegrationMetrics() {
        try {
            return objectMapper.writeValueAsString(getIntegrationMetricsMap());
        } catch (Exception e) {
            logger.error("Failed to generate integration metrics", e);
            return "{\"error\":\"Failed to generate integration metrics\"}";
        }
    }
    
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> system = new HashMap<>();
        system.put("timestamp", Instant.now().toString());
        system.put("uptime", getUptimeSeconds());
        system.put("startTime", startTime.toString());
        
        return system;
    }
    
    private Map<String, Object> getRequestMetricsMap() {
        Map<String, Object> requests = new HashMap<>();
        long total = totalRequests.get();
        
        requests.put("total", total);
        requests.put("successful", successfulRequests.get());
        requests.put("failed", failedRequests.get());
        requests.put("successRate", total > 0 ? (double) successfulRequests.get() / total : 0.0);
        requests.put("averageResponseTime", total > 0 ? totalResponseTime.sum() / total : 0.0);
        requests.put("requestsPerSecond", getRequestsPerSecond());
        
        return requests;
    }
    
    private Map<String, Object> getIntegrationMetricsMap() {
        Map<String, Object> integrations = new HashMap<>();
        long total = totalIntegrationsExecuted.get();
        
        integrations.put("total", total);
        integrations.put("failed", failedIntegrations.get());
        integrations.put("successful", total - failedIntegrations.get());
        integrations.put("successRate", total > 0 ? (double) (total - failedIntegrations.get()) / total : 0.0);
        integrations.put("averageExecutionTime", total > 0 ? totalIntegrationTime.sum() / total : 0.0);
        
        return integrations;
    }
    
    private Map<String, Object> getMediatorMetrics() {
        Map<String, Object> mediators = new HashMap<>();
        
        for (String mediatorName : mediatorExecutionCount.keySet()) {
            Map<String, Object> mediatorStats = new HashMap<>();
            long executions = mediatorExecutionCount.get(mediatorName).get();
            double totalTime = mediatorExecutionTime.get(mediatorName).sum();
            
            mediatorStats.put("executions", executions);
            mediatorStats.put("totalTime", totalTime);
            mediatorStats.put("averageTime", executions > 0 ? totalTime / executions : 0.0);
            
            mediators.put(mediatorName, mediatorStats);
        }
        
        return mediators;
    }
    
    private Map<String, Object> getRuntimeMetrics() {
        Map<String, Object> runtime = new HashMap<>();
        Runtime rt = Runtime.getRuntime();
        
        runtime.put("memoryUsed", rt.totalMemory() - rt.freeMemory());
        runtime.put("memoryTotal", rt.totalMemory());
        runtime.put("memoryMax", rt.maxMemory());
        runtime.put("processors", rt.availableProcessors());
        runtime.put("threads", Thread.activeCount());
        
        return runtime;
    }
    
    private double getRequestsPerSecond() {
        long uptimeSeconds = getUptimeSeconds();
        return uptimeSeconds > 0 ? (double) totalRequests.get() / uptimeSeconds : 0.0;
    }
    
    private long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
    
    /**
     * Reset all metrics (useful for testing)
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.reset();
        totalIntegrationsExecuted.set(0);
        failedIntegrations.set(0);
        totalIntegrationTime.reset();
        mediatorExecutionCount.clear();
        mediatorExecutionTime.clear();
        
        logger.info("All metrics have been reset");
    }
}
