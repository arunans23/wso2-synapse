package org.wso2.graalvm.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import org.wso2.graalvm.cli.MicroIntegratorCLI;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Logs command to view server logs
 */
@Command(
    name = "logs",
    description = "View server logs",
    mixinStandardHelpOptions = true
)
public class LogsCommand implements Callable<Integer> {
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Option(names = {"-f", "--follow"}, description = "Follow log output")
    private boolean follow = false;
    
    @Option(names = {"-n", "--lines"}, description = "Number of lines to show (default: 100)")
    private int lines = 100;
    
    @Option(names = {"-l", "--log-file"}, description = "Log file path (default: logs/micro-integrator.log)")
    private String logFile = "logs/micro-integrator.log";
    
    @Option(names = {"--error"}, description = "Show only error logs")
    private boolean errorOnly = false;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Viewing logs from: " + logFile);
        
        Path logPath = Paths.get(logFile);
        if (!Files.exists(logPath)) {
            System.err.println("Log file not found: " + logFile);
            System.out.println("Server may not be running or logs may be in a different location");
            return 1;
        }
        
        try {
            if (follow) {
                followLogs(logPath);
            } else {
                showLogs(logPath);
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error reading logs: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private void showLogs(Path logPath) throws Exception {
        String content = Files.readString(logPath);
        String[] allLines = content.split("\n");
        
        // Get last N lines
        int startIndex = Math.max(0, allLines.length - lines);
        
        for (int i = startIndex; i < allLines.length; i++) {
            String line = allLines[i];
            
            if (errorOnly) {
                if (line.contains("ERROR") || line.contains("WARN")) {
                    System.out.println(line);
                }
            } else {
                System.out.println(line);
            }
        }
    }
    
    private void followLogs(Path logPath) throws Exception {
        System.out.println("Following logs (Press Ctrl+C to stop)...");
        
        // Show initial logs
        showLogs(logPath);
        
        // Simple implementation - in production would use proper file watching
        long lastSize = Files.size(logPath);
        
        while (true) {
            Thread.sleep(1000);
            
            if (Files.exists(logPath)) {
                long currentSize = Files.size(logPath);
                
                if (currentSize > lastSize) {
                    // File has grown, read new content
                    String content = Files.readString(logPath);
                    String[] lines = content.split("\n");
                    
                    // Calculate how many new lines there are
                    long linesToSkip = lastSize > 0 ? 
                        content.substring(0, (int)lastSize).split("\n").length : 0;
                    
                    for (int i = (int)linesToSkip; i < lines.length; i++) {
                        String line = lines[i];
                        
                        if (errorOnly) {
                            if (line.contains("ERROR") || line.contains("WARN")) {
                                System.out.println(line);
                            }
                        } else {
                            System.out.println(line);
                        }
                    }
                    
                    lastSize = currentSize;
                }
            }
        }
    }
}
