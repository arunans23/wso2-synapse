package org.wso2.graalvm.core.threading;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadExecutorTest {
    
    private VirtualThreadExecutor executor;
    
    @BeforeEach
    void setUp() {
        executor = new VirtualThreadExecutor("test-executor");
    }
    
    @AfterEach
    void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    @Test
    void testBasicExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger result = new AtomicInteger(0);
        
        executor.execute(() -> {
            result.set(42);
            latch.countDown();
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(42, result.get());
    }
    
    @Test
    void testSubmitCallable() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executor.submit(() -> "Hello Virtual Threads!").get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        String result = future.get(5, TimeUnit.SECONDS);
        assertEquals("Hello Virtual Threads!", result);
    }
    
    @Test
    void testConcurrentExecution() throws InterruptedException {
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(taskCount, counter.get());
        assertTrue(executor.getTaskCount() >= taskCount);
    }
    
    @Test
    void testShutdown() throws InterruptedException {
        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());
        
        executor.shutdown();
        
        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
    }
    
    @Test
    void testExecutorName() {
        assertEquals("test-executor", executor.getName());
    }
}
