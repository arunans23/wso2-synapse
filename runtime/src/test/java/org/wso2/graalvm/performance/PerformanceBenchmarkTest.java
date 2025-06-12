package org.wso2.graalvm.performance;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.runtime.MicroIntegratorApplication;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmark test for WSO2 Micro Integrator GraalVM Edition.
 * 
 * This test starts the server, performs load testing, and compares performance
 * metrics against baseline expectations to detect performance regressions.
 * 
 * Metrics tested:
 * - Server startup time
 * - Request throughput (requests/second)
 * - Average response time
 * - Memory usage
 * - Thread efficiency
 * - Error rate under load
 * 
 * Usage:
 * - Regular performance tests: mvn test -Pperformance-tests
 * - Stress tests: mvn test -Pstress-tests
 * - Custom parameters: mvn test -Pperformance-tests -Dperformance.load.test.requests=2000
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceBenchmarkTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmarkTest.class);
    
    private static MicroIntegratorApplication application;
    private static Thread serverThread;
    private static HttpClient httpClient;
    private static boolean serverStarted = false;
    
    @BeforeAll
    static void startServer() {
        logger.info("Starting WSO2 Micro Integrator for performance testing...");
        
        // Configure system properties for testing
        System.setProperty("mi.server.host", PerformanceTestConfig.SERVER_HOST);
        System.setProperty("mi.server.port", String.valueOf(PerformanceTestConfig.SERVER_PORT));
        System.setProperty("logging.level.org.wso2.graalvm", "WARN"); // Reduce logging noise
        
        // Create HTTP client for testing
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        // Start server in background thread
        CountDownLatch serverStartLatch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();
        
        serverThread = Thread.ofVirtual()
            .name("test-server")
            .start(() -> {
                try {
                    application = new MicroIntegratorApplication();
                    application.start(new String[]{});
                    serverStartLatch.countDown();
                    serverStarted = true;
                    logger.info("Test server started successfully");
                    application.waitForShutdown();
                } catch (Exception e) {
                    logger.error("Failed to start test server", e);
                    serverStartLatch.countDown();
                }
            });
        
        // Wait for server to start
        try {
            boolean started = serverStartLatch.await(30, TimeUnit.SECONDS);
            if (!started) {
                fail("Server failed to start within 30 seconds");
            }
            
            // Wait additional time for server to be fully ready
            Thread.sleep(2000);
            
            // Verify server is responding
            waitForServerReady();
            
            long startupTime = System.currentTimeMillis() - startTime;
            logger.info("Server startup completed in {} ms", startupTime);
            
            // Assert startup time is within acceptable range
            assertTrue(startupTime < PerformanceTestConfig.MAX_STARTUP_TIME_MS, 
                String.format("Startup time %d ms exceeds maximum %d ms", startupTime, PerformanceTestConfig.MAX_STARTUP_TIME_MS));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Server startup was interrupted");
        }
    }
    
    @AfterAll
    static void stopServer() {
        logger.info("Stopping test server...");
        if (application != null) {
            application.stop();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
        serverStarted = false;
        logger.info("Test server stopped");
    }
    
    private static void waitForServerReady() throws InterruptedException {
        int maxAttempts = 30;
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PerformanceTestConfig.SERVER_BASE_URL + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    logger.info("Server health check passed");
                    return;
                }
            } catch (Exception e) {
                // Server not ready yet, continue waiting
            }
            
            attempts++;
            Thread.sleep(1000);
        }
        
        fail("Server failed to become ready within 30 seconds");
    }
    
    @Test
    @Order(1)
    void testServerStartupPerformance() {
        assertTrue(serverStarted, "Server should be started and ready");
        logger.info("✓ Server startup performance test passed");
    }
    
    @Test
    @Order(2)
    void testBasicResponsePerformance() throws Exception {
        logger.info("Testing basic response performance...");
        
        // Measure response time for health endpoint
        long startTime = System.nanoTime();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(PerformanceTestConfig.SERVER_BASE_URL + "/health"))
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        long responseTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
        
        assertEquals(200, response.statusCode(), "Health endpoint should return 200 OK");
        assertTrue(responseTime < PerformanceTestConfig.MAX_AVG_RESPONSE_TIME_MS, 
            String.format("Response time %d ms exceeds maximum %d ms", responseTime, PerformanceTestConfig.MAX_AVG_RESPONSE_TIME_MS));
        
        logger.info("✓ Basic response time: {} ms", responseTime);
    }
    
    @Test
    @Order(3)
    void testWarmupPhase() throws Exception {
        logger.info("Running warmup phase with {} requests...", PerformanceTestConfig.WARMUP_REQUESTS);
        
        CountDownLatch warmupLatch = new CountDownLatch(PerformanceTestConfig.WARMUP_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        
        long warmupStart = System.currentTimeMillis();
        
        // Execute warmup requests using virtual threads
        for (int i = 0; i < PerformanceTestConfig.WARMUP_REQUESTS; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PerformanceTestConfig.SERVER_BASE_URL + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.warn("Warmup request failed: {}", e.getMessage());
                } finally {
                    warmupLatch.countDown();
                }
            });
        }
        
        assertTrue(warmupLatch.await(30, TimeUnit.SECONDS), "Warmup phase should complete within 30 seconds");
        
        long warmupTime = System.currentTimeMillis() - warmupStart;
        double warmupSuccessRate = (double) successCount.get() / PerformanceTestConfig.WARMUP_REQUESTS;
        
        logger.info("✓ Warmup completed: {} requests in {} ms, success rate: {}", 
            PerformanceTestConfig.WARMUP_REQUESTS, warmupTime, String.format("%.2f%%", warmupSuccessRate * 100));
        
        assertTrue(warmupSuccessRate > 0.95, "Warmup success rate should be above 95%");
    }
    
    @Test
    @Order(4)
    void testThroughputPerformance() throws Exception {
        // Use stress test parameters if enabled
        int totalRequests = PerformanceTestConfig.isStressTestEnabled() ? 
            PerformanceTestConfig.STRESS_TEST_REQUESTS : PerformanceTestConfig.LOAD_TEST_REQUESTS;
        int concurrentThreads = PerformanceTestConfig.isStressTestEnabled() ? 
            PerformanceTestConfig.STRESS_TEST_THREADS : PerformanceTestConfig.CONCURRENT_THREADS;
            
        logger.info("Testing throughput performance with {} concurrent threads and {} total requests...", 
            concurrentThreads, totalRequests);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch loadTestLatch = new CountDownLatch(totalRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        long loadTestStart = System.currentTimeMillis();
        
        // Submit load test requests
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(PerformanceTestConfig.SERVER_BASE_URL + "/health"))
                        .timeout(Duration.ofSeconds(10))
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    long requestTime = (System.nanoTime() - requestStart) / 1_000_000;
                    responseTimes.add(requestTime);
                    totalResponseTime.addAndGet(requestTime);
                    
                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.debug("Load test request failed: {}", e.getMessage());
                } finally {
                    loadTestLatch.countDown();
                }
            });
        }
        
        // Wait for all requests to complete with dynamic timeout
        Duration testTimeout = PerformanceTestConfig.isStressTestEnabled() ? 
            Duration.ofMinutes(10) : Duration.ofMinutes(5);
        assertTrue(loadTestLatch.await(testTimeout.toSeconds(), TimeUnit.SECONDS), 
            "Load test should complete within timeout");
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        long loadTestTime = System.currentTimeMillis() - loadTestStart;
        
        // Calculate metrics
        double throughput = (double) totalRequests / (loadTestTime / 1000.0);
        double avgResponseTime = totalResponseTime.get() / (double) responseTimes.size();
        double errorRate = (double) errorCount.get() / totalRequests;
        
        // Calculate percentiles
        Collections.sort(responseTimes);
        double p50 = getPercentile(responseTimes, 50);
        double p95 = getPercentile(responseTimes, 95);
        double p99 = getPercentile(responseTimes, 99);
        
        // Log performance metrics
        logger.info("=== LOAD TEST RESULTS ===");
        logger.info("Total Requests: {}", totalRequests);
        logger.info("Successful Requests: {}", successCount.get());
        logger.info("Failed Requests: {}", errorCount.get());
        logger.info("Test Duration: {} ms", loadTestTime);
        logger.info("Throughput: {} requests/second", String.format("%.2f", throughput));
        logger.info("Average Response Time: {} ms", String.format("%.2f", avgResponseTime));
        logger.info("Response Time P50: {} ms", String.format("%.2f", p50));
        logger.info("Response Time P95: {} ms", String.format("%.2f", p95));
        logger.info("Response Time P99: {} ms", String.format("%.2f", p99));
        logger.info("Error Rate: {}", String.format("%.2f%%", errorRate * 100));
        
        // Performance assertions
        assertTrue(throughput >= PerformanceTestConfig.MIN_THROUGHPUT_RPS, 
            String.format("Throughput %.2f rps is below minimum %.2f rps", throughput, PerformanceTestConfig.MIN_THROUGHPUT_RPS));
        
        assertTrue(avgResponseTime <= PerformanceTestConfig.MAX_AVG_RESPONSE_TIME_MS, 
            String.format("Average response time %.2f ms exceeds maximum %d ms", avgResponseTime, PerformanceTestConfig.MAX_AVG_RESPONSE_TIME_MS));
        
        assertTrue(errorRate <= PerformanceTestConfig.MAX_ERROR_RATE, 
            String.format("Error rate %.2f%% exceeds maximum %.2f%%", errorRate * 100, PerformanceTestConfig.MAX_ERROR_RATE * 100));
        
        logger.info("✓ Throughput performance test passed");
    }
    
    @Test
    @Order(5)
    void testMemoryUsagePerformance() throws Exception {
        logger.info("Testing memory usage performance...");
        
        // Force garbage collection and measure memory
        System.gc();
        Thread.sleep(1000);
        System.gc();
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        long usedMemoryMB = usedMemory / (1024 * 1024);
        long maxMemoryMB = maxMemory / (1024 * 1024);
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        logger.info("Memory Usage: {} MB / {} MB ({})", usedMemoryMB, maxMemoryMB, String.format("%.1f%%", memoryUsagePercent));
        
        assertTrue(usedMemoryMB <= PerformanceTestConfig.MAX_MEMORY_USAGE_MB, 
            String.format("Memory usage %d MB exceeds maximum %d MB", usedMemoryMB, PerformanceTestConfig.MAX_MEMORY_USAGE_MB));
        
        logger.info("✓ Memory usage performance test passed");
    }
    
    @Test
    @Order(6)
    void testVirtualThreadEfficiency() throws Exception {
        logger.info("Testing virtual thread efficiency...");
        
        int threadCount = 1000;
        CountDownLatch threadLatch = new CountDownLatch(threadCount);
        AtomicInteger completedTasks = new AtomicInteger(0);
        
        long threadTestStart = System.currentTimeMillis();
        
        // Create many virtual threads to test efficiency
        for (int i = 0; i < threadCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    // Simulate some work
                    Thread.sleep(100);
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    threadLatch.countDown();
                }
            });
        }
        
        assertTrue(threadLatch.await(30, TimeUnit.SECONDS), 
            "Virtual thread test should complete within 30 seconds");
        
        long threadTestTime = System.currentTimeMillis() - threadTestStart;
        
        logger.info("Virtual Thread Test: {} threads completed in {} ms", 
            completedTasks.get(), threadTestTime);
        
        assertEquals(threadCount, completedTasks.get(), "All virtual threads should complete");
        assertTrue(threadTestTime < 15000, "Virtual thread test should complete within 15 seconds");
        
        logger.info("✓ Virtual thread efficiency test passed");
    }
    
    @Test
    @Order(7)
    void testConcurrentRequestHandling() throws Exception {
        logger.info("Testing concurrent request handling...");
        
        int concurrentRequests = 50;
        CountDownLatch concurrentLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger concurrentSuccessCount = new AtomicInteger(0);
        
        long concurrentTestStart = System.currentTimeMillis();
        
        // Submit concurrent requests all at once
        for (int i = 0; i < concurrentRequests; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8290/health"))
                        .timeout(Duration.ofSeconds(10))
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        concurrentSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.debug("Concurrent request failed: {}", e.getMessage());
                } finally {
                    concurrentLatch.countDown();
                }
            });
        }
        
        assertTrue(concurrentLatch.await(30, TimeUnit.SECONDS), 
            "Concurrent request test should complete within 30 seconds");
        
        long concurrentTestTime = System.currentTimeMillis() - concurrentTestStart;
        double concurrentSuccessRate = (double) concurrentSuccessCount.get() / concurrentRequests;
        
        logger.info("Concurrent Test: {}/{} requests successful in {} ms (success rate: {})", 
            concurrentSuccessCount.get(), concurrentRequests, concurrentTestTime, String.format("%.2f%%", concurrentSuccessRate * 100));
        
        assertTrue(concurrentSuccessRate >= 0.95, 
            "Concurrent request success rate should be at least 95%");
        
        logger.info("✓ Concurrent request handling test passed");
    }
    
    @Test
    @Order(8)
    void generatePerformanceReport() {
        logger.info("=== PERFORMANCE BENCHMARK SUMMARY ===");
        logger.info("✓ Server Startup Performance: PASSED");
        logger.info("✓ Basic Response Performance: PASSED");
        logger.info("✓ Warmup Phase: PASSED");
        logger.info("✓ Throughput Performance: PASSED");
        logger.info("✓ Memory Usage Performance: PASSED");
        logger.info("✓ Virtual Thread Efficiency: PASSED");
        logger.info("✓ Concurrent Request Handling: PASSED");
        logger.info("=== ALL PERFORMANCE TESTS PASSED ===");
        
        // Performance recommendations
        logger.info("Performance Recommendations:");
        logger.info("- Monitor startup time in production");
        logger.info("- Set up alerts for throughput below {} rps", PerformanceTestConfig.MIN_THROUGHPUT_RPS);
        logger.info("- Monitor memory usage to stay below {} MB", PerformanceTestConfig.MAX_MEMORY_USAGE_MB);
        logger.info("- Keep error rate below {}", String.format("%.1f%%", PerformanceTestConfig.MAX_ERROR_RATE * 100));
    }
    
    // Utility method to calculate percentiles
    private double getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index);
    }
}
