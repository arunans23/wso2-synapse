package org.wso2.graalvm.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import org.wso2.graalvm.cli.MicroIntegratorCLI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Stop command to shutdown the Micro Integrator server
 */
@Command(
    name = "stop",
    description = "Stop the Micro Integrator server",
    mixinStandardHelpOptions = true
)
public class StopCommand implements Callable<Integer> {
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Stopping WSO2 Micro Integrator...");
        
        try {
            // Try to read PID file
            Path pidFile = Paths.get("mi.pid");
            if (Files.exists(pidFile)) {
                String pidStr = Files.readString(pidFile);
                long pid = Long.parseLong(pidStr.trim());
                
                ProcessHandle process = ProcessHandle.of(pid).orElse(null);
                if (process != null && process.isAlive()) {
                    System.out.println("Terminating process " + pid + "...");
                    boolean terminated = process.destroy();
                    
                    if (terminated) {
                        // Wait for graceful shutdown
                        Thread.sleep(5000);
                        
                        if (process.isAlive()) {
                            System.out.println("Force killing process...");
                            process.destroyForcibly();
                        }
                        
                        System.out.println("Server stopped successfully!");
                    } else {
                        System.err.println("Failed to terminate process " + pid);
                        return 1;
                    }
                } else {
                    System.out.println("Process " + pid + " is not running");
                }
                
                // Clean up PID file
                Files.deleteIfExists(pidFile);
                
            } else {
                System.out.println("No PID file found. Server may not be running.");
                
                // Try to stop via shutdown port (if implemented)
                if (tryShutdownPort()) {
                    System.out.println("Server stopped via shutdown port");
                } else {
                    System.out.println("Could not connect to shutdown port");
                    return 1;
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private boolean tryShutdownPort() {
        try {
            // Implementation for shutdown via HTTP endpoint
            // This would send a shutdown signal to the running server
            // For now, return false as this requires HTTP client implementation
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
