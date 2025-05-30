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
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

/**
 * Deploy command to deploy integration artifacts
 */
@Command(
    name = "deploy",
    description = "Deploy integration artifacts to the server",
    mixinStandardHelpOptions = true
)
public class DeployCommand implements Callable<Integer> {
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Parameters(index = "0", description = "Path to the artifact file to deploy")
    private String artifactPath;
    
    @Option(names = {"-d", "--deployment-dir"}, description = "Deployment directory (default: deployments)")
    private String deploymentDir = "deployments";
    
    @Option(names = {"-f", "--force"}, description = "Force deployment (overwrite existing)")
    private boolean force = false;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Deploying artifact: " + artifactPath);
        
        Path sourcePath = Paths.get(artifactPath);
        if (!Files.exists(sourcePath)) {
            System.err.println("Error: Artifact file not found: " + artifactPath);
            return 1;
        }
        
        // Create deployment directory if it doesn't exist
        Path deployPath = Paths.get(deploymentDir);
        if (!Files.exists(deployPath)) {
            Files.createDirectories(deployPath);
            System.out.println("Created deployment directory: " + deploymentDir);
        }
        
        // Determine target file name
        String fileName = sourcePath.getFileName().toString();
        Path targetPath = deployPath.resolve(fileName);
        
        try {
            if (Files.exists(targetPath) && !force) {
                System.err.println("Error: Artifact already exists: " + targetPath);
                System.err.println("Use --force to overwrite");
                return 1;
            }
            
            // Copy the artifact
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✓ Artifact deployed successfully: " + targetPath);
            
            // Validate the artifact
            if (validateArtifact(targetPath)) {
                System.out.println("✓ Artifact validation passed");
            } else {
                System.err.println("⚠ Artifact validation failed - deployment may not work correctly");
            }
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error deploying artifact: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private boolean validateArtifact(Path artifactPath) {
        try {
            String fileName = artifactPath.getFileName().toString();
            
            // Basic validation based on file extension
            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                // Validate YAML syntax
                String content = Files.readString(artifactPath);
                return content.contains("integration:") || content.contains("flows:");
            } else if (fileName.endsWith(".json")) {
                // Validate JSON syntax
                String content = Files.readString(artifactPath);
                return content.trim().startsWith("{") && content.trim().endsWith("}");
            }
            
            return true; // Assume valid for other file types
            
        } catch (Exception e) {
            return false;
        }
    }
}
