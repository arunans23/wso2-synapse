package org.wso2.graalvm.management.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.config.IntegrationConfiguration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Health check service for monitoring application health.
 * Provides REST endpoints for health status and readiness checks.
 */
public class HealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final Instant startTime = Instant.now();
    private IntegrationConfiguration config;
    
    public HealthCheckService(IntegrationConfiguration config) {
        this.config = config;
    }
    
    /**
     * Get basic health status
     */
    public String getHealthStatus() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", healthy.get() ? "UP" : "DOWN");
            health.put("timestamp", Instant.now().toString());
            health.put("uptime", getUptimeSeconds());
            health.put("version", "1.0.0-SNAPSHOT");
            health.put("javaVersion", System.getProperty("java.version"));
            
            return objectMapper.writeValueAsString(health);
        } catch (Exception e) {
            logger.error("Failed to generate health status", e);
            return "{\"status\":\"ERROR\",\"message\":\"Failed to generate health status\"}";
        }
    }
    
    /**
     * Get detailed health check with components
     */
    public String getDetailedHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", healthy.get() ? "UP" : "DOWN");
            health.put("timestamp", Instant.now().toString());
            health.put("uptime", getUptimeSeconds());
            
            // Component health checks
            Map<String, Object> components = new HashMap<>();
            components.put("runtime", checkRuntimeHealth());
            components.put("memory", checkMemoryHealth());
            components.put("threads", checkThreadHealth());
            components.put("configuration", checkConfigurationHealth());
            
            health.put("components", components);
            
            return objectMapper.writeValueAsString(health);
        } catch (Exception e) {
            logger.error("Failed to generate detailed health status", e);
            return "{\"status\":\"ERROR\",\"message\":\"Failed to generate detailed health status\"}";
        }
    }
    
    /**
     * Get readiness status (for Kubernetes readiness probes)
     */
    public String getReadinessStatus() {
        try {
            Map<String, Object> readiness = new HashMap<>();
            boolean ready = healthy.get() && isApplicationReady();
            
            readiness.put("status", ready ? "READY" : "NOT_READY");
            readiness.put("timestamp", Instant.now().toString());
            readiness.put("uptime", getUptimeSeconds());
            
            return objectMapper.writeValueAsString(readiness);
        } catch (Exception e) {
            logger.error("Failed to generate readiness status", e);
            return "{\"status\":\"ERROR\",\"message\":\"Failed to generate readiness status\"}";
        }
    }
    
    /**
     * Get liveness status (for Kubernetes liveness probes)
     */
    public String getLivenessStatus() {
        try {
            Map<String, Object> liveness = new HashMap<>();
            liveness.put("status", healthy.get() ? "ALIVE" : "DEAD");
            liveness.put("timestamp", Instant.now().toString());
            liveness.put("uptime", getUptimeSeconds());
            
            return objectMapper.writeValueAsString(liveness);
        } catch (Exception e) {
            logger.error("Failed to generate liveness status", e);
            return "{\"status\":\"ERROR\",\"message\":\"Failed to generate liveness status\"}";
        }
    }
    
    private Map<String, Object> checkRuntimeHealth() {
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("status", "UP");
        runtime.put("javaVersion", System.getProperty("java.version"));
        runtime.put("javaVendor", System.getProperty("java.vendor"));
        runtime.put("osName", System.getProperty("os.name"));
        runtime.put("osVersion", System.getProperty("os.version"));
        
        return runtime;
    }
    
    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> memory = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        memory.put("status", usagePercent < 90 ? "UP" : "WARNING");
        memory.put("totalMemory", totalMemory);
        memory.put("freeMemory", freeMemory);
        memory.put("usedMemory", usedMemory);
        memory.put("maxMemory", maxMemory);
        memory.put("usagePercent", Math.round(usagePercent * 100.0) / 100.0);
        
        return memory;
    }
    
    private Map<String, Object> checkThreadHealth() {
        Map<String, Object> threads = new HashMap<>();
        
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        int activeThreads = rootGroup.activeCount();
        threads.put("status", activeThreads < 1000 ? "UP" : "WARNING");
        threads.put("activeThreads", activeThreads);
        threads.put("virtualThreadsSupported", isVirtualThreadsSupported());
        
        return threads;
    }
    
    private Map<String, Object> checkConfigurationHealth() {
        Map<String, Object> configHealth = new HashMap<>();
        configHealth.put("status", config != null ? "UP" : "DOWN");
        
        if (config != null) {
            configHealth.put("serverPort", config.getServer().getPort());
            configHealth.put("virtualThreadsEnabled", config.getThreading().isVirtualThreadsEnabled());
        }
        
        return configHealth;
    }
    
    private boolean isApplicationReady() {
        // Check if application has been running for at least 5 seconds
        return getUptimeSeconds() >= 5;
    }
    
    private boolean isVirtualThreadsSupported() {
        try {
            // Check if virtual threads are available (Java 21+)
            Class.forName("java.lang.VirtualThread");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
    
    public void setHealthy(boolean healthy) {
        this.healthy.set(healthy);
        logger.info("Application health status changed to: {}", healthy ? "UP" : "DOWN");
    }
    
    public boolean isHealthy() {
        return healthy.get();
    }
}
