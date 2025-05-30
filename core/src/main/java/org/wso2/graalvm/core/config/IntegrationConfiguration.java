package org.wso2.graalvm.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration management for the Micro Integrator.
 * Replaces OSGi configuration with a simple YAML-based configuration system.
 */
public class IntegrationConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrationConfiguration.class);
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    @JsonProperty("server")
    private ServerConfig server = new ServerConfig();
    
    @JsonProperty("threading")
    private ThreadingConfig threading = new ThreadingConfig();
    
    @JsonProperty("transports")
    private Map<String, TransportConfig> transports = new HashMap<>();
    
    @JsonProperty("mediators")
    private MediatorConfig mediators = new MediatorConfig();
    
    @JsonProperty("security")
    private SecurityConfig security = new SecurityConfig();
    
    @JsonProperty("management")
    private ManagementConfig management = new ManagementConfig();
    
    public static class ServerConfig {
        @JsonProperty("host")
        private String host = "localhost";
        
        @JsonProperty("port")
        private int port = 8290;
        
        @JsonProperty("httpsPort")
        private int httpsPort = 8253;
        
        @JsonProperty("managementPort")
        private int managementPort = 9164;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getHttpsPort() { return httpsPort; }
        public void setHttpsPort(int httpsPort) { this.httpsPort = httpsPort; }
        public int getManagementPort() { return managementPort; }
        public void setManagementPort(int managementPort) { this.managementPort = managementPort; }
    }
    
    public static class ThreadingConfig {
        @JsonProperty("virtualThreads")
        private boolean virtualThreadsEnabled = true;
        
        @JsonProperty("maxWorkerThreads")
        private int maxWorkerThreads = 100;
        
        @JsonProperty("taskQueueSize")
        private int taskQueueSize = 1000;
        
        // Getters and setters
        public boolean isVirtualThreadsEnabled() { return virtualThreadsEnabled; }
        public void setVirtualThreadsEnabled(boolean virtualThreadsEnabled) { this.virtualThreadsEnabled = virtualThreadsEnabled; }
        public int getMaxWorkerThreads() { return maxWorkerThreads; }
        public void setMaxWorkerThreads(int maxWorkerThreads) { this.maxWorkerThreads = maxWorkerThreads; }
        public int getTaskQueueSize() { return taskQueueSize; }
        public void setTaskQueueSize(int taskQueueSize) { this.taskQueueSize = taskQueueSize; }
    }
    
    public static class TransportConfig {
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("properties")
        private Map<String, Object> properties = new HashMap<>();
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }
    
    public static class MediatorConfig {
        @JsonProperty("enabledMediators")
        private List<String> enabledMediators = List.of("log", "property", "send", "respond", "call");
        
        @JsonProperty("customMediators")
        private Map<String, String> customMediators = new HashMap<>();
        
        // Getters and setters
        public List<String> getEnabledMediators() { return enabledMediators; }
        public void setEnabledMediators(List<String> enabledMediators) { this.enabledMediators = enabledMediators; }
        public Map<String, String> getCustomMediators() { return customMediators; }
        public void setCustomMediators(Map<String, String> customMediators) { this.customMediators = customMediators; }
    }
    
    public static class SecurityConfig {
        @JsonProperty("enableSecurity")
        private boolean enableSecurity = false;
        
        @JsonProperty("keyStore")
        private String keyStore;
        
        @JsonProperty("trustStore")
        private String trustStore;
        
        // Getters and setters
        public boolean isEnableSecurity() { return enableSecurity; }
        public void setEnableSecurity(boolean enableSecurity) { this.enableSecurity = enableSecurity; }
        public String getKeyStore() { return keyStore; }
        public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
        public String getTrustStore() { return trustStore; }
        public void setTrustStore(String trustStore) { this.trustStore = trustStore; }
    }
    
    public static class ManagementConfig {
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("endpoints")
        private List<String> endpoints = List.of("health", "metrics", "info");
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getEndpoints() { return endpoints; }
        public void setEndpoints(List<String> endpoints) { this.endpoints = endpoints; }
    }
    
    // Static factory methods
    public static IntegrationConfiguration fromFile(Path configPath) throws IOException {
        logger.info("Loading configuration from: {}", configPath);
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            return yamlMapper.readValue(inputStream, IntegrationConfiguration.class);
        }
    }
    
    public static IntegrationConfiguration fromClasspath(String resourcePath) throws IOException {
        logger.info("Loading configuration from classpath: {}", resourcePath);
        try (InputStream inputStream = IntegrationConfiguration.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Configuration resource not found: " + resourcePath);
            }
            return yamlMapper.readValue(inputStream, IntegrationConfiguration.class);
        }
    }
    
    public static IntegrationConfiguration defaultConfig() {
        logger.info("Using default configuration");
        return new IntegrationConfiguration();
    }
    
    public void saveToFile(Path configPath) throws IOException {
        logger.info("Saving configuration to: {}", configPath);
        yamlMapper.writeValue(configPath.toFile(), this);
    }
    
    // Getters and setters for main config sections
    public ServerConfig getServer() { return server; }
    public void setServer(ServerConfig server) { this.server = server; }
    
    public ThreadingConfig getThreading() { return threading; }
    public void setThreading(ThreadingConfig threading) { this.threading = threading; }
    
    public Map<String, TransportConfig> getTransports() { return transports; }
    public void setTransports(Map<String, TransportConfig> transports) { this.transports = transports; }
    
    public MediatorConfig getMediators() { return mediators; }
    public void setMediators(MediatorConfig mediators) { this.mediators = mediators; }
    
    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }
    
    public ManagementConfig getManagement() { return management; }
    public void setManagement(ManagementConfig management) { this.management = management; }
}
