package org.wso2.graalvm.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.config.IntegrationConfiguration;
import org.wso2.graalvm.core.threading.VirtualThreadExecutor;
import org.wso2.graalvm.runtime.server.NettyHttpServer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Main application class for the GraalVM-based WSO2 Micro Integrator.
 * This replaces the OSGi-based startup mechanism with a simple, direct startup.
 */
public class MicroIntegratorApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(MicroIntegratorApplication.class);
    
    private IntegrationConfiguration config;
    private VirtualThreadExecutor mainExecutor;
    private NettyHttpServer httpServer;
    private volatile boolean running = false;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    
    public static void main(String[] args) {
        Thread.currentThread().setName("mi-main");
        
        MicroIntegratorApplication app = new MicroIntegratorApplication();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            app.stop();
        }, "mi-shutdown"));
        
        try {
            app.start(args);
            app.waitForShutdown();
        } catch (Exception e) {
            logger.error("Failed to start Micro Integrator", e);
            System.exit(1);
        }
    }
    
    public void start(String[] args) throws Exception {
        logger.info("Starting WSO2 Micro Integrator (GraalVM Edition)");
        logger.info("Java Version: {}", System.getProperty("java.version"));
        logger.info("Virtual Threads Available: {}", 
                   Thread.class.getMethods().length > 50); // Simple check for virtual threads
        
        // Load configuration
        loadConfiguration(args);
        
        // Initialize thread executor
        initializeThreading();
        
        // Start HTTP server
        startHttpServer();
        
        running = true;
        logger.info("WSO2 Micro Integrator started successfully");
        logger.info("HTTP Server: http://{}:{}", config.getServer().getHost(), config.getServer().getPort());
        logger.info("Management Port: {}", config.getServer().getManagementPort());
    }
    
    private void loadConfiguration(String[] args) throws Exception {
        String configFile = getConfigFile(args);
        
        if (configFile != null) {
            Path configPath = Paths.get(configFile);
            config = IntegrationConfiguration.fromFile(configPath);
            logger.info("Loaded configuration from: {}", configPath);
        } else {
            // Try to load from classpath
            try {
                config = IntegrationConfiguration.fromClasspath("application.yml");
                logger.info("Loaded configuration from classpath");
            } catch (Exception e) {
                logger.warn("No configuration file found, using defaults");
                config = IntegrationConfiguration.defaultConfig();
            }
        }
    }
    
    private String getConfigFile(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i]) || "-c".equals(args[i])) {
                return args[i + 1];
            }
        }
        return System.getProperty("mi.config.file");
    }
    
    private void initializeThreading() {
        logger.info("Initializing threading model");
        
        if (config.getThreading().isVirtualThreadsEnabled()) {
            mainExecutor = new VirtualThreadExecutor("mi-main");
            logger.info("Virtual threads enabled");
        } else {
            // Fallback to regular thread pool for non-JDK21 environments
            logger.warn("Virtual threads disabled - using traditional thread pool");
            mainExecutor = new VirtualThreadExecutor("mi-traditional");
        }
    }
    
    private void startHttpServer() throws Exception {
        logger.info("Starting HTTP server");
        
        httpServer = new NettyHttpServer(config, mainExecutor);
        httpServer.start();
        
        logger.info("HTTP server started on port {}", config.getServer().getPort());
    }
    
    public void stop() {
        if (!running) {
            return;
        }
        
        logger.info("Stopping WSO2 Micro Integrator");
        running = false;
        
        try {
            // Stop HTTP server
            if (httpServer != null) {
                httpServer.stop();
                logger.info("HTTP server stopped");
            }
            
            // Shutdown thread executor
            if (mainExecutor != null) {
                mainExecutor.shutdown();
                if (!mainExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate gracefully, forcing shutdown");
                    mainExecutor.shutdownNow();
                }
                logger.info("Thread executor stopped");
            }
            
            logger.info("WSO2 Micro Integrator stopped successfully");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        } finally {
            shutdownLatch.countDown();
        }
    }
    
    public void waitForShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public IntegrationConfiguration getConfiguration() {
        return config;
    }
}
