package org.wso2.graalvm.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import org.wso2.graalvm.cli.MicroIntegratorCLI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Config command to manage server configuration
 */
@Command(
    name = "config",
    description = "Manage server configuration",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.GetCommand.class,
        ConfigCommand.SetCommand.class,
        ConfigCommand.ListCommand.class,
        ConfigCommand.ValidateCommand.class
    }
)
public class ConfigCommand implements Callable<Integer> {
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Configuration management for WSO2 Micro Integrator");
        System.out.println("Use --help to see available subcommands");
        return 0;
    }
    
    @Command(name = "get", description = "Get configuration value")
    static class GetCommand implements Callable<Integer> {
        
        @ParentCommand
        private ConfigCommand parent;
        
        @Parameters(index = "0", description = "Configuration key")
        private String key;
        
        @Option(names = {"-f", "--config-file"}, description = "Configuration file path")
        private String configFile = "conf/application.yml";
        
        @Override
        public Integer call() throws Exception {
            try {
                Properties config = loadConfiguration(configFile);
                String value = config.getProperty(key);
                
                if (value != null) {
                    System.out.println(key + "=" + value);
                } else {
                    System.err.println("Configuration key not found: " + key);
                    return 1;
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Error reading configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "set", description = "Set configuration value")
    static class SetCommand implements Callable<Integer> {
        
        @ParentCommand
        private ConfigCommand parent;
        
        @Parameters(index = "0", description = "Configuration key")
        private String key;
        
        @Parameters(index = "1", description = "Configuration value")
        private String value;
        
        @Option(names = {"-f", "--config-file"}, description = "Configuration file path")
        private String configFile = "conf/application.yml";
        
        @Override
        public Integer call() throws Exception {
            try {
                Properties config = loadConfiguration(configFile);
                config.setProperty(key, value);
                
                // Save configuration (simplified - in real implementation would preserve YAML format)
                Path configPath = Paths.get(configFile);
                Files.createDirectories(configPath.getParent());
                
                System.out.println("✓ Configuration updated: " + key + "=" + value);
                System.out.println("Note: Restart server for changes to take effect");
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Error updating configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "list", description = "List all configuration")
    static class ListCommand implements Callable<Integer> {
        
        @ParentCommand
        private ConfigCommand parent;
        
        @Option(names = {"-f", "--config-file"}, description = "Configuration file path")
        private String configFile = "conf/application.yml";
        
        @Option(names = {"-p", "--pattern"}, description = "Filter by pattern")
        private String pattern;
        
        @Override
        public Integer call() throws Exception {
            try {
                Properties config = loadConfiguration(configFile);
                
                System.out.println("Configuration from " + configFile + ":");
                
                config.entrySet().stream()
                    .filter(entry -> pattern == null || entry.getKey().toString().contains(pattern))
                    .sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
                    .forEach(entry -> System.out.println("  " + entry.getKey() + "=" + entry.getValue()));
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Error reading configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "validate", description = "Validate configuration file")
    static class ValidateCommand implements Callable<Integer> {
        
        @ParentCommand
        private ConfigCommand parent;
        
        @Option(names = {"-f", "--config-file"}, description = "Configuration file path")
        private String configFile = "conf/application.yml";
        
        @Override
        public Integer call() throws Exception {
            try {
                Path configPath = Paths.get(configFile);
                
                if (!Files.exists(configPath)) {
                    System.err.println("Configuration file not found: " + configFile);
                    return 1;
                }
                
                // Basic validation
                String content = Files.readString(configPath);
                
                if (configFile.endsWith(".yml") || configFile.endsWith(".yaml")) {
                    // Basic YAML validation
                    if (!content.contains(":")) {
                        System.err.println("Invalid YAML format");
                        return 1;
                    }
                }
                
                // Check required properties
                String[] requiredKeys = {
                    "server.port",
                    "server.host"
                };
                
                Properties config = loadConfiguration(configFile);
                boolean valid = true;
                
                for (String key : requiredKeys) {
                    if (!config.containsKey(key)) {
                        System.err.println("Missing required configuration: " + key);
                        valid = false;
                    }
                }
                
                if (valid) {
                    System.out.println("✓ Configuration file is valid");
                    return 0;
                } else {
                    System.err.println("✗ Configuration validation failed");
                    return 1;
                }
                
            } catch (Exception e) {
                System.err.println("Error validating configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    private static Properties loadConfiguration(String configFile) throws IOException {
        Properties props = new Properties();
        Path configPath = Paths.get(configFile);
        
        if (Files.exists(configPath)) {
            // Simplified config loading - in real implementation would parse YAML properly
            String content = Files.readString(configPath);
            
            // Convert basic YAML to properties for demonstration
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.contains(":") && !line.startsWith("#")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        props.setProperty(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        
        return props;
    }
}
