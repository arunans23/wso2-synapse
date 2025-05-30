package org.wso2.graalvm.security.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * SSL/TLS configuration service for secure communication
 */
public class SslConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(SslConfigurationService.class);
    
    private SSLContext sslContext;
    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;
    
    private String keystorePath;
    private String keystorePassword;
    private String truststorePath;
    private String truststorePassword;
    private String keyPassword;
    private boolean clientAuthRequired = false;
    
    public SslConfigurationService() {
        // Default configuration
        this.keystorePath = "conf/keystore.jks";
        this.keystorePassword = "password";
        this.truststorePath = "conf/truststore.jks";
        this.truststorePassword = "password";
        this.keyPassword = "password";
    }
    
    /**
     * Initialize SSL context with keystore and truststore
     */
    public void initialize() throws SslConfigurationException {
        try {
            logger.info("Initializing SSL configuration...");
            
            // Initialize KeyManager
            initializeKeyManager();
            
            // Initialize TrustManager
            initializeTrustManager();
            
            // Create SSL Context
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                null
            );
            
            logger.info("SSL configuration initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize SSL configuration", e);
            throw new SslConfigurationException("SSL initialization failed", e);
        }
    }
    
    /**
     * Initialize key manager from keystore
     */
    private void initializeKeyManager() throws KeyStoreException, IOException, 
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        
        if (keystorePath != null) {
            KeyStore keystore = loadKeyStore(keystorePath, keystorePassword);
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keyPassword.toCharArray());
            logger.info("KeyManager initialized with keystore: {}", keystorePath);
        }
    }
    
    /**
     * Initialize trust manager from truststore
     */
    private void initializeTrustManager() throws KeyStoreException, IOException, 
            NoSuchAlgorithmException, CertificateException {
        
        if (truststorePath != null) {
            KeyStore truststore = loadKeyStore(truststorePath, truststorePassword);
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            logger.info("TrustManager initialized with truststore: {}", truststorePath);
        } else {
            // Use default trust manager
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            logger.info("TrustManager initialized with system default");
        }
    }
    
    /**
     * Load keystore from file
     */
    private KeyStore loadKeyStore(String path, String password) throws KeyStoreException, 
            IOException, NoSuchAlgorithmException, CertificateException {
        
        KeyStore keyStore = KeyStore.getInstance("JKS");
        
        try (InputStream inputStream = new FileInputStream(path)) {
            keyStore.load(inputStream, password.toCharArray());
        } catch (IOException e) {
            logger.warn("Could not load keystore from {}, using empty keystore", path);
            keyStore.load(null, password.toCharArray());
        }
        
        return keyStore;
    }
    
    /**
     * Get SSL context
     */
    public SSLContext getSslContext() {
        if (sslContext == null) {
            try {
                initialize();
            } catch (SslConfigurationException e) {
                logger.error("Failed to initialize SSL context", e);
                throw new RuntimeException(e);
            }
        }
        return sslContext;
    }
    
    /**
     * Create SSL server socket factory
     */
    public SSLServerSocketFactory createServerSocketFactory() {
        SSLServerSocketFactory factory = getSslContext().getServerSocketFactory();
        return new ConfiguredSSLServerSocketFactory(factory);
    }
    
    /**
     * Create SSL socket factory for clients
     */
    public SSLSocketFactory createClientSocketFactory() {
        return getSslContext().getSocketFactory();
    }
    
    /**
     * Create a trusting SSL context (for development/testing)
     */
    public static SSLContext createTrustingContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new TrustingTrustManager()}, null);
        return context;
    }
    
    /**
     * Validate SSL certificate chain
     */
    public boolean validateCertificateChain(X509Certificate[] chain) {
        if (chain == null || chain.length == 0) {
            return false;
        }
        
        try {
            // Basic validation - check if not expired
            for (X509Certificate cert : chain) {
                cert.checkValidity();
            }
            return true;
        } catch (Exception e) {
            logger.warn("Certificate validation failed", e);
            return false;
        }
    }
    
    // Configuration setters
    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }
    
    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }
    
    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }
    
    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }
    
    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }
    
    public void setClientAuthRequired(boolean clientAuthRequired) {
        this.clientAuthRequired = clientAuthRequired;
    }
    
    public boolean isClientAuthRequired() {
        return clientAuthRequired;
    }
    
    /**
     * Custom SSL server socket factory with additional configuration
     */
    private class ConfiguredSSLServerSocketFactory extends SSLServerSocketFactory {
        private final SSLServerSocketFactory delegate;
        
        public ConfiguredSSLServerSocketFactory(SSLServerSocketFactory delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public SSLServerSocket createServerSocket() throws IOException {
            SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket();
            configureServerSocket(socket);
            return socket;
        }
        
        @Override
        public SSLServerSocket createServerSocket(int port) throws IOException {
            SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(port);
            configureServerSocket(socket);
            return socket;
        }
        
        @Override
        public SSLServerSocket createServerSocket(int port, int backlog) throws IOException {
            SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(port, backlog);
            configureServerSocket(socket);
            return socket;
        }
        
        @Override
        public SSLServerSocket createServerSocket(int port, int backlog, 
                java.net.InetAddress ifAddress) throws IOException {
            SSLServerSocket socket = (SSLServerSocket) delegate.createServerSocket(port, backlog, ifAddress);
            configureServerSocket(socket);
            return socket;
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }
        
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        
        private void configureServerSocket(SSLServerSocket socket) {
            if (clientAuthRequired) {
                socket.setNeedClientAuth(true);
            }
            
            // Set secure protocols
            socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            
            // Set secure cipher suites (example - adjust based on security requirements)
            String[] secureCiphers = {
                "TLS_AES_256_GCM_SHA384",
                "TLS_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
            };
            socket.setEnabledCipherSuites(secureCiphers);
        }
    }
    
    /**
     * Trust manager that trusts all certificates (for development only)
     */
    private static class TrustingTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Trust all
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Trust all
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    /**
     * SSL configuration exception
     */
    public static class SslConfigurationException extends Exception {
        public SslConfigurationException(String message) {
            super(message);
        }
        
        public SslConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
