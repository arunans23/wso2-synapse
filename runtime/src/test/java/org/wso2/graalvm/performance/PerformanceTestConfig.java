package org.wso2.graalvm.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for performance benchmark tests.
 * Allows customization of test parameters through system properties.
 */
public class PerformanceTestConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestConfig.class);
    
    // Default baseline values
    public static final long DEFAULT_MAX_STARTUP_TIME_MS = 5000;
    public static final double DEFAULT_MIN_THROUGHPUT_RPS = 100.0;
    public static final long DEFAULT_MAX_AVG_RESPONSE_TIME_MS = 50;
    public static final long DEFAULT_MAX_MEMORY_USAGE_MB = 200;
    public static final double DEFAULT_MAX_ERROR_RATE = 0.05;
    
    // Default test parameters
    public static final int DEFAULT_WARMUP_REQUESTS = 100;
    public static final int DEFAULT_LOAD_TEST_REQUESTS = 1000;
    public static final int DEFAULT_CONCURRENT_THREADS = 20;
    
    // Performance baselines (can be overridden via system properties)
    public static final long MAX_STARTUP_TIME_MS = getLongProperty("performance.max.startup.time.ms", DEFAULT_MAX_STARTUP_TIME_MS);
    public static final double MIN_THROUGHPUT_RPS = getDoubleProperty("performance.min.throughput.rps", DEFAULT_MIN_THROUGHPUT_RPS);
    public static final long MAX_AVG_RESPONSE_TIME_MS = getLongProperty("performance.max.avg.response.time.ms", DEFAULT_MAX_AVG_RESPONSE_TIME_MS);
    public static final long MAX_MEMORY_USAGE_MB = getLongProperty("performance.max.memory.usage.mb", DEFAULT_MAX_MEMORY_USAGE_MB);
    public static final double MAX_ERROR_RATE = getDoubleProperty("performance.max.error.rate", DEFAULT_MAX_ERROR_RATE);
    
    // Test configuration (can be overridden via system properties)
    public static final int WARMUP_REQUESTS = getIntProperty("performance.warmup.requests", DEFAULT_WARMUP_REQUESTS);
    public static final int LOAD_TEST_REQUESTS = getIntProperty("performance.load.test.requests", DEFAULT_LOAD_TEST_REQUESTS);
    public static final int CONCURRENT_THREADS = getIntProperty("performance.concurrent.threads", DEFAULT_CONCURRENT_THREADS);
    
    // Stress test configuration
    public static final int STRESS_TEST_REQUESTS = getIntProperty("stress.test.requests", 5000);
    public static final int STRESS_TEST_THREADS = getIntProperty("stress.test.threads", 50);
    
    // Server configuration
    public static final String SERVER_HOST = getStringProperty("performance.server.host", "localhost");
    public static final int SERVER_PORT = getIntProperty("performance.server.port", 8290);
    public static final String SERVER_BASE_URL = "http://" + SERVER_HOST + ":" + SERVER_PORT;
    
    static {
        logger.info("Performance Test Configuration:");
        logger.info("  Max Startup Time: {} ms", MAX_STARTUP_TIME_MS);
        logger.info("  Min Throughput: {} rps", MIN_THROUGHPUT_RPS);
        logger.info("  Max Average Response Time: {} ms", MAX_AVG_RESPONSE_TIME_MS);
        logger.info("  Max Memory Usage: {} MB", MAX_MEMORY_USAGE_MB);
        logger.info("  Max Error Rate: {}%", MAX_ERROR_RATE * 100);
        logger.info("  Warmup Requests: {}", WARMUP_REQUESTS);
        logger.info("  Load Test Requests: {}", LOAD_TEST_REQUESTS);
        logger.info("  Concurrent Threads: {}", CONCURRENT_THREADS);
        logger.info("  Server: {}", SERVER_BASE_URL);
    }
    
    private static long getLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value for property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    private static double getDoubleProperty(String key, double defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid double value for property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid int value for property {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
    
    private static String getStringProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }
    
    /**
     * Check if stress testing is enabled
     */
    public static boolean isStressTestEnabled() {
        return "true".equals(System.getProperty("stress.test.enabled"));
    }
    
    /**
     * Check if performance testing is enabled
     */
    public static boolean isPerformanceTestEnabled() {
        return "true".equals(System.getProperty("performance.test.enabled"));
    }
}
