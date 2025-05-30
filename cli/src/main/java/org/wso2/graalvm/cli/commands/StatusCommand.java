package org.wso2.graalvm.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import org.wso2.graalvm.cli.MicroIntegratorCLI;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Status command to check the server status
 */
@Command(
    name = "status",
    description = "Check the status of the Micro Integrator server",
    mixinStandardHelpOptions = true
)
public class StatusCommand implements Callable<Integer> {
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Option(names = {"-p", "--port"}, description = "HTTP port to check (default: 8290)")
    private int port = 8290;
    
    @Option(names = {"-h", "--host"}, description = "Host address to check (default: localhost)")
    private String host = "localhost";
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Checking WSO2 Micro Integrator status...");
        
        boolean isRunning = false;
        String statusMessage = "";
        
        // Check PID file
        Path pidFile = Paths.get("mi.pid");
        if (Files.exists(pidFile)) {
            try {
                String pidStr = Files.readString(pidFile);
                long pid = Long.parseLong(pidStr.trim());
                
                ProcessHandle process = ProcessHandle.of(pid).orElse(null);
                if (process != null && process.isAlive()) {
                    isRunning = true;
                    statusMessage = "Server is running (PID: " + pid + ")";
                } else {
                    statusMessage = "PID file exists but process " + pid + " is not running";
                    // Clean up stale PID file
                    Files.deleteIfExists(pidFile);
                }
            } catch (Exception e) {
                statusMessage = "Invalid PID file: " + e.getMessage();
            }
        }
        
        // Check if port is accessible
        boolean portAccessible = isPortAccessible(host, port);
        
        if (isRunning && portAccessible) {
            System.out.println("✓ " + statusMessage);
            System.out.println("✓ HTTP endpoint is accessible on " + host + ":" + port);
            
            // Try to get additional health information
            printHealthInfo();
            
        } else if (isRunning && !portAccessible) {
            System.out.println("✓ " + statusMessage);
            System.out.println("✗ HTTP endpoint is not accessible on " + host + ":" + port);
            System.out.println("  The server process is running but may be starting up or has issues");
            
        } else if (!isRunning && portAccessible) {
            System.out.println("✗ No PID file found");
            System.out.println("? HTTP endpoint is accessible on " + host + ":" + port);
            System.out.println("  A server might be running but not managed by this CLI");
            
        } else {
            System.out.println("✗ Server is not running");
            System.out.println("✗ HTTP endpoint is not accessible on " + host + ":" + port);
        }
        
        return isRunning && portAccessible ? 0 : 1;
    }
    
    private boolean isPortAccessible(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 3000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    private void printHealthInfo() {
        try {
            // Try to get health information from the management API
            // This would require HTTP client implementation
            System.out.println("Health Information:");
            System.out.println("  Status: Running");
            System.out.println("  Uptime: Available via /api/health endpoint");
            System.out.println("  Memory: Available via /api/metrics endpoint");
            
        } catch (Exception e) {
            if (parent.isVerbose()) {
                System.out.println("Could not retrieve health information: " + e.getMessage());
            }
        }
    }
}
