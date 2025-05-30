package org.wso2.graalvm.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.wso2.graalvm.cli.commands.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Main CLI application for WSO2 Micro Integrator GraalVM
 */
@Command(
    name = "wso2mi",
    description = "WSO2 Micro Integrator GraalVM Edition CLI",
    version = "1.0.0-SNAPSHOT",
    mixinStandardHelpOptions = true,
    subcommands = {
        StartCommand.class,
        StopCommand.class,
        StatusCommand.class,
        DeployCommand.class,
        UndeployCommand.class,
        ConfigCommand.class,
        LogsCommand.class,
        SecurityCommand.class,
        MetricsCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class MicroIntegratorCLI implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(MicroIntegratorCLI.class);
    
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;
    
    @Option(names = {"-c", "--config"}, description = "Configuration file path")
    private String configFile;
    
    public static void main(String[] args) {
        System.setProperty("picocli.ansi", "auto");
        
        int exitCode = new CommandLine(new MicroIntegratorCLI())
                .setExecutionExceptionHandler(new CLIExceptionHandler())
                .execute(args);
        
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() {
        System.out.println("WSO2 Micro Integrator GraalVM Edition CLI");
        System.out.println("Use --help to see available commands");
        return 0;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public String getConfigFile() {
        return configFile;
    }
    
    /**
     * Custom exception handler for CLI
     */
    private static class CLIExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception ex, CommandLine cmd, CommandLine.ParseResult parseResult) {
            cmd.getErr().println("Error: " + ex.getMessage());
            
            if (cmd.getCommandSpec().findOption("--verbose") != null) {
                ex.printStackTrace(cmd.getErr());
            }
            
            return 1;
        }
    }
}
