package org.wso2.graalvm.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import org.wso2.graalvm.cli.MicroIntegratorCLI;
import org.wso2.graalvm.runtime.MicroIntegratorApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Start command to launch the Micro Integrator server
 */
@Command(
    name = "start",
    description = "Start the Micro Integrator server",
    mixinStandardHelpOptions = true
)
public class StartCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(StartCommand.class);
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Option(names = {"-p", "--port"}, description = "HTTP port (default: 8290)")
    private int port = 8290;
    
    @Option(names = {"-h", "--host"}, description = "Host address (default: 0.0.0.0)")
    private String host = "0.0.0.0";
    
    @Option(names = {"-d", "--deployment-dir"}, description = "Deployment directory")
    private String deploymentDir = "deployments";
    
    @Option(names = {"--background"}, description = "Run in background")
    private boolean background = false;
    
    @Option(names = {"--ssl"}, description = "Enable SSL/TLS")
    private boolean ssl = false;
    
    @Option(names = {"--ssl-port"}, description = "HTTPS port (default: 8243)")
    private int sslPort = 8243;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Starting WSO2 Micro Integrator...");
        
        // Validate deployment directory
        Path deployPath = Paths.get(deploymentDir);
        if (!Files.exists(deployPath)) {
            System.err.println("Warning: Deployment directory does not exist: " + deploymentDir);
            Files.createDirectories(deployPath);
            System.out.println("Created deployment directory: " + deploymentDir);
        }
        
        // Prepare configuration
        System.setProperty("mi.server.host", host);
        System.setProperty("mi.server.port", String.valueOf(port));
        System.setProperty("mi.deployment.dir", deploymentDir);
        
        if (ssl) {
            System.setProperty("mi.server.ssl.enabled", "true");
            System.setProperty("mi.server.ssl.port", String.valueOf(sslPort));
        }
        
        if (parent.getConfigFile() != null) {
            System.setProperty("mi.config.file", parent.getConfigFile());
        }
        
        if (parent.isVerbose()) {
            System.setProperty("logging.level.org.wso2.graalvm", "DEBUG");
        }
        
        try {
            if (background) {
                System.out.println("Starting server in background mode...");
                startInBackground();
            } else {
                System.out.println("Starting server in foreground mode...");
                startInForeground();
            }
            
            System.out.println("Server started successfully!");
            System.out.println("HTTP endpoint: http://" + host + ":" + port);
            
            if (ssl) {
                System.out.println("HTTPS endpoint: https://" + host + ":" + sslPort);
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private void startInForeground() throws Exception {
        // Start the main application
        MicroIntegratorApplication.main(new String[]{});
    }
    
    private void startInBackground() throws Exception {
        // Create a new thread for the server
        Thread serverThread = Thread.ofVirtual()
            .name("mi-server")
            .start(() -> {
                try {
                    MicroIntegratorApplication.main(new String[]{});
                } catch (Exception e) {
                    logger.error("Server failed in background", e);
                }
            });
        
        // Wait a bit to ensure server starts
        Thread.sleep(2000);
        
        if (serverThread.isAlive()) {
            System.out.println("Server is running in background (PID: " + ProcessHandle.current().pid() + ")");
            
            // Write PID file
            try {
                Path pidFile = Paths.get("mi.pid");
                Files.write(pidFile, String.valueOf(ProcessHandle.current().pid()).getBytes());
                System.out.println("PID file written: " + pidFile.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Warning: Could not write PID file: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Server failed to start");
        }
    }
}
