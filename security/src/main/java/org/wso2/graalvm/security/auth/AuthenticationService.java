package org.wso2.graalvm.security.auth;

import org.wso2.graalvm.core.context.IntegrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service providing multiple authentication mechanisms
 * including Basic Auth, API Key, and JWT token validation.
 */
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final Map<String, UserCredentials> users = new ConcurrentHashMap<>();
    private final Map<String, String> apiKeys = new ConcurrentHashMap<>();
    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    public AuthenticationService() {
        // Initialize with default admin user
        addUser("admin", "admin123", UserRole.ADMIN);
        addApiKey("default-api-key", "admin");
    }
    
    /**
     * Add a new user with hashed password
     */
    public void addUser(String username, String password, UserRole role) {
        try {
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            users.put(username, new UserCredentials(username, hashedPassword, salt, role));
            logger.info("Added user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to add user: {}", username, e);
        }
    }
    
    /**
     * Add API key for a user
     */
    public void addApiKey(String apiKey, String username) {
        apiKeys.put(apiKey, username);
        logger.info("Added API key for user: {}", username);
    }
    
    /**
     * Authenticate using Basic Authentication
     */
    public AuthenticationResult authenticateBasic(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return AuthenticationResult.failure("Invalid Basic Auth header");
        }
        
        try {
            String credentials = new String(Base64.getDecoder()
                .decode(authHeader.substring(6)), StandardCharsets.UTF_8);
            String[] parts = credentials.split(":", 2);
            
            if (parts.length != 2) {
                return AuthenticationResult.failure("Invalid credentials format");
            }
            
            return authenticateUser(parts[0], parts[1]);
        } catch (Exception e) {
            logger.error("Basic authentication failed", e);
            return AuthenticationResult.failure("Authentication failed");
        }
    }
    
    /**
     * Authenticate using API Key
     */
    public AuthenticationResult authenticateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AuthenticationResult.failure("API key is required");
        }
        
        String username = apiKeys.get(apiKey);
        if (username == null) {
            return AuthenticationResult.failure("Invalid API key");
        }
        
        UserCredentials user = users.get(username);
        if (user == null) {
            return AuthenticationResult.failure("User not found");
        }
        
        return AuthenticationResult.success(user.username, user.role);
    }
    
    /**
     * Generate JWT-like token
     */
    public String generateToken(String username) {
        try {
            UserCredentials user = users.get(username);
            if (user == null) {
                throw new SecurityException("User not found");
            }
            
            // Simple token format: base64(username:timestamp:signature)
            long timestamp = Instant.now().toEpochMilli();
            String payload = username + ":" + timestamp;
            String signature = hashPassword(payload, user.salt);
            String token = Base64.getEncoder()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
            
            tokens.put(token, new TokenInfo(username, user.role, 
                Instant.now().plus(24, ChronoUnit.HOURS)));
            
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate token for user: {}", username, e);
            throw new SecurityException("Token generation failed");
        }
    }
    
    /**
     * Validate token
     */
    public AuthenticationResult validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return AuthenticationResult.failure("Token is required");
        }
        
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo == null) {
            return AuthenticationResult.failure("Invalid token");
        }
        
        if (Instant.now().isAfter(tokenInfo.expiresAt)) {
            tokens.remove(token);
            return AuthenticationResult.failure("Token expired");
        }
        
        return AuthenticationResult.success(tokenInfo.username, tokenInfo.role);
    }
    
    /**
     * Authenticate with username and password
     */
    private AuthenticationResult authenticateUser(String username, String password) {
        UserCredentials user = users.get(username);
        if (user == null) {
            return AuthenticationResult.failure("Invalid credentials");
        }
        
        try {
            String hashedPassword = hashPassword(password, user.salt);
            if (!user.hashedPassword.equals(hashedPassword)) {
                return AuthenticationResult.failure("Invalid credentials");
            }
            
            return AuthenticationResult.success(username, user.role);
        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", username, e);
            return AuthenticationResult.failure("Authentication failed");
        }
    }
    
    /**
     * Generate salt for password hashing
     */
    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Hash password with salt
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        md.update(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(md.digest());
    }
    
    /**
     * Clean up expired tokens
     */
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }
    
    // Inner classes
    public enum UserRole {
        ADMIN, USER, READONLY
    }
    
    public static class UserCredentials {
        final String username;
        final String hashedPassword;
        final String salt;
        final UserRole role;
        
        public UserCredentials(String username, String hashedPassword, String salt, UserRole role) {
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.salt = salt;
            this.role = role;
        }
    }
    
    public static class TokenInfo {
        final String username;
        final UserRole role;
        final Instant expiresAt;
        
        public TokenInfo(String username, UserRole role, Instant expiresAt) {
            this.username = username;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }
    
    public static class AuthenticationResult {
        private final boolean success;
        private final String username;
        private final UserRole role;
        private final String message;
        
        private AuthenticationResult(boolean success, String username, UserRole role, String message) {
            this.success = success;
            this.username = username;
            this.role = role;
            this.message = message;
        }
        
        public static AuthenticationResult success(String username, UserRole role) {
            return new AuthenticationResult(true, username, role, "Authentication successful");
        }
        
        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, null, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public String getUsername() { return username; }
        public UserRole getRole() { return role; }
        public String getMessage() { return message; }
    }
}
