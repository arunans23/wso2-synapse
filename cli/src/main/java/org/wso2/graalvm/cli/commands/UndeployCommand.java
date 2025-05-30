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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Undeploy command to remove integration artifacts
 */
@Command(
    name = "undeploy",
    description = "Remove integration artifacts from the server",
    mixinStandardHelpOptions = true
)
public class UndeployCommand implements Callable<Integer> {
    
    @ParentCommand
    private MicroIntegratorCLI parent;
    
    @Parameters(index = "0", description = "Name of the artifact to undeploy", arity = "0..1")
    private String artifactName;
    
    @Option(names = {"-d", "--deployment-dir"}, description = "Deployment directory (default: deployments)")
    private String deploymentDir = "deployments";
    
    @Option(names = {"-l", "--list"}, description = "List deployed artifacts")
    private boolean list = false;
    
    @Option(names = {"--all"}, description = "Undeploy all artifacts")
    private boolean all = false;
    
    @Override
    public Integer call() throws Exception {
        Path deployPath = Paths.get(deploymentDir);
        
        if (!Files.exists(deployPath)) {
            System.err.println("Error: Deployment directory not found: " + deploymentDir);
            return 1;
        }
        
        if (list) {
            return listArtifacts(deployPath);
        }
        
        if (all) {
            return undeployAll(deployPath);
        }
        
        if (artifactName == null) {
            System.err.println("Error: Artifact name is required");
            System.err.println("Use --list to see deployed artifacts or --all to undeploy everything");
            return 1;
        }
        
        return undeployArtifact(deployPath, artifactName);
    }
    
    private int listArtifacts(Path deployPath) throws IOException {
        System.out.println("Deployed artifacts in " + deployPath + ":");
        
        List<Path> artifacts = Files.list(deployPath)
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());
        
        if (artifacts.isEmpty()) {
            System.out.println("  No artifacts deployed");
        } else {
            for (Path artifact : artifacts) {
                String name = artifact.getFileName().toString();
                long size = Files.size(artifact);
                String lastModified = Files.getLastModifiedTime(artifact).toString();
                
                System.out.printf("  %-30s %8d bytes  %s%n", name, size, lastModified);
            }
        }
        
        return 0;
    }
    
    private int undeployArtifact(Path deployPath, String artifactName) {
        try {
            Path artifactPath = deployPath.resolve(artifactName);
            
            if (!Files.exists(artifactPath)) {
                System.err.println("Error: Artifact not found: " + artifactName);
                return 1;
            }
            
            Files.delete(artifactPath);
            System.out.println("✓ Artifact undeployed successfully: " + artifactName);
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error undeploying artifact: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private int undeployAll(Path deployPath) {
        try {
            List<Path> artifacts = Files.list(deployPath)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
            
            if (artifacts.isEmpty()) {
                System.out.println("No artifacts to undeploy");
                return 0;
            }
            
            int success = 0;
            int failed = 0;
            
            for (Path artifact : artifacts) {
                try {
                    String name = artifact.getFileName().toString();
                    Files.delete(artifact);
                    System.out.println("✓ Undeployed: " + name);
                    success++;
                } catch (IOException e) {
                    System.err.println("✗ Failed to undeploy: " + artifact.getFileName());
                    failed++;
                }
            }
            
            System.out.println("Undeploy complete: " + success + " successful, " + failed + " failed");
            return failed > 0 ? 1 : 0;
            
        } catch (IOException e) {
            System.err.println("Error listing artifacts: " + e.getMessage());
            if (parent.isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }
}
