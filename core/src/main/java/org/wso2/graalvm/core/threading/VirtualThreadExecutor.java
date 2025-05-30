package org.wso2.graalvm.core.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Virtual Thread-based executor service for the Micro Integrator.
 * Leverages Java 21 Virtual Threads for high-concurrency, low-resource integration scenarios.
 */
public class VirtualThreadExecutor implements ExecutorService {
    
    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadExecutor.class);
    
    private final String name;
    private final AtomicLong taskCounter = new AtomicLong(0);
    private final ExecutorService virtualExecutor;
    private volatile boolean shutdown = false;
    
    public VirtualThreadExecutor(String name) {
        this.name = name;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        logger.info("Initialized VirtualThreadExecutor: {}", name);
    }
    
    @Override
    public void execute(Runnable command) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        
        long taskId = taskCounter.incrementAndGet();
        Runnable wrappedTask = () -> {
            Thread.currentThread().setName(name + "-task-" + taskId);
            try {
                command.run();
            } catch (Exception e) {
                logger.error("Task {} failed in executor {}", taskId, name, e);
            }
        };
        
        virtualExecutor.execute(wrappedTask);
    }
    
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor has been shut down");
        }
        
        long taskId = taskCounter.incrementAndGet();
        Callable<T> wrappedTask = () -> {
            Thread.currentThread().setName(name + "-task-" + taskId);
            try {
                return task.call();
            } catch (Exception e) {
                logger.error("Task {} failed in executor {}", taskId, name, e);
                throw e;
            }
        };
        
        return virtualExecutor.submit(wrappedTask);
    }
    
    @Override
    public Future<?> submit(Runnable task) {
        return submit(Executors.callable(task));
    }
    
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(Executors.callable(task, result));
    }
    
    @Override
    public void shutdown() {
        shutdown = true;
        virtualExecutor.shutdown();
        logger.info("VirtualThreadExecutor {} shutdown initiated", name);
    }
    
    @Override
    public java.util.List<Runnable> shutdownNow() {
        shutdown = true;
        java.util.List<Runnable> pendingTasks = virtualExecutor.shutdownNow();
        logger.info("VirtualThreadExecutor {} shutdown immediately, {} pending tasks", name, pendingTasks.size());
        return pendingTasks;
    }
    
    @Override
    public boolean isShutdown() {
        return shutdown;
    }
    
    @Override
    public boolean isTerminated() {
        return virtualExecutor.isTerminated();
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return virtualExecutor.awaitTermination(timeout, unit);
    }
    
    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return virtualExecutor.invokeAll(tasks);
    }
    
    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return virtualExecutor.invokeAll(tasks, timeout, unit);
    }
    
    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return virtualExecutor.invokeAny(tasks);
    }
    
    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return virtualExecutor.invokeAny(tasks, timeout, unit);
    }
    
    public long getTaskCount() {
        return taskCounter.get();
    }
    
    public String getName() {
        return name;
    }
}
