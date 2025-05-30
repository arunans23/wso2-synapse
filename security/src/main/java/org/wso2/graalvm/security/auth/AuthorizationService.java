package org.wso2.graalvm.security.auth;

import org.wso2.graalvm.core.context.IntegrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authorization service providing role-based access control (RBAC)
 * for API endpoints and integration flows.
 */
public class AuthorizationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    
    private final ConcurrentHashMap<String, Set<Permission>> rolePermissions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> resourceRoles = new ConcurrentHashMap<>();
    
    public AuthorizationService() {
        initializeDefaultPermissions();
    }
    
    /**
     * Initialize default role permissions
     */
    private void initializeDefaultPermissions() {
        // Admin permissions
        rolePermissions.put("ADMIN", Set.of(
            Permission.READ_ALL,
            Permission.WRITE_ALL,
            Permission.DELETE_ALL,
            Permission.MANAGE_USERS,
            Permission.MANAGE_SYSTEM,
            Permission.VIEW_METRICS,
            Permission.EXECUTE_INTEGRATION
        ));
        
        // User permissions
        rolePermissions.put("USER", Set.of(
            Permission.READ_INTEGRATION,
            Permission.WRITE_INTEGRATION,
            Permission.EXECUTE_INTEGRATION,
            Permission.VIEW_METRICS
        ));
        
        // Read-only permissions
        rolePermissions.put("READONLY", Set.of(
            Permission.READ_INTEGRATION,
            Permission.VIEW_METRICS
        ));
        
        // Default resource roles
        resourceRoles.put("/api/health", Set.of("ADMIN", "USER", "READONLY"));
        resourceRoles.put("/api/metrics", Set.of("ADMIN", "USER", "READONLY"));
        resourceRoles.put("/api/info", Set.of("ADMIN", "USER", "READONLY"));
        resourceRoles.put("/api/integrations", Set.of("ADMIN", "USER"));
        resourceRoles.put("/api/admin", Set.of("ADMIN"));
        
        logger.info("Initialized default authorization permissions");
    }
    
    /**
     * Check if user has permission for a specific action
     */
    public boolean hasPermission(String role, Permission permission) {
        if (role == null || permission == null) {
            return false;
        }
        
        Set<Permission> permissions = rolePermissions.get(role.toUpperCase());
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Check if user has access to a specific resource
     */
    public boolean hasAccess(String role, String resource) {
        if (role == null || resource == null) {
            return false;
        }
        
        // Check exact match first
        Set<String> allowedRoles = resourceRoles.get(resource);
        if (allowedRoles != null) {
            return allowedRoles.contains(role.toUpperCase());
        }
        
        // Check pattern matches
        for (String resourcePattern : resourceRoles.keySet()) {
            if (resource.startsWith(resourcePattern)) {
                allowedRoles = resourceRoles.get(resourcePattern);
                return allowedRoles != null && allowedRoles.contains(role.toUpperCase());
            }
        }
        
        return false;
    }
    
    /**
     * Authorize HTTP request
     */
    public AuthorizationResult authorizeRequest(String role, String method, String path) {
        if (role == null) {
            return AuthorizationResult.deny("No role provided");
        }
        
        // Check resource access
        if (!hasAccess(role, path)) {
            return AuthorizationResult.deny("Access denied to resource: " + path);
        }
        
        // Check method permissions
        Permission requiredPermission = getRequiredPermission(method, path);
        if (requiredPermission != null && !hasPermission(role, requiredPermission)) {
            return AuthorizationResult.deny("Insufficient permissions for: " + method + " " + path);
        }
        
        return AuthorizationResult.allow();
    }
    
    /**
     * Get required permission based on HTTP method and path
     */
    private Permission getRequiredPermission(String method, String path) {
        if (method == null) {
            return null;
        }
        
        switch (method.toUpperCase()) {
            case "GET":
                return path.contains("/admin") ? Permission.MANAGE_SYSTEM : Permission.READ_INTEGRATION;
            case "POST":
            case "PUT":
                return path.contains("/admin") ? Permission.MANAGE_SYSTEM : Permission.WRITE_INTEGRATION;
            case "DELETE":
                return path.contains("/admin") ? Permission.MANAGE_SYSTEM : Permission.DELETE_INTEGRATION;
            default:
                return Permission.READ_INTEGRATION;
        }
    }
    
    /**
     * Add permission to role
     */
    public void addPermissionToRole(String role, Permission permission) {
        rolePermissions.computeIfAbsent(role.toUpperCase(), k -> ConcurrentHashMap.newKeySet())
                      .add(permission);
        logger.info("Added permission {} to role {}", permission, role);
    }
    
    /**
     * Remove permission from role
     */
    public void removePermissionFromRole(String role, Permission permission) {
        Set<Permission> permissions = rolePermissions.get(role.toUpperCase());
        if (permissions != null) {
            permissions.remove(permission);
            logger.info("Removed permission {} from role {}", permission, role);
        }
    }
    
    /**
     * Add role access to resource
     */
    public void addResourceAccess(String resource, String role) {
        resourceRoles.computeIfAbsent(resource, k -> ConcurrentHashMap.newKeySet())
                    .add(role.toUpperCase());
        logger.info("Added role {} access to resource {}", role, resource);
    }
    
    /**
     * Remove role access from resource
     */
    public void removeResourceAccess(String resource, String role) {
        Set<String> roles = resourceRoles.get(resource);
        if (roles != null) {
            roles.remove(role.toUpperCase());
            logger.info("Removed role {} access from resource {}", role, resource);
        }
    }
    
    /**
     * Get all permissions for a role
     */
    public Set<Permission> getPermissions(String role) {
        return rolePermissions.getOrDefault(role.toUpperCase(), Set.of());
    }
    
    /**
     * Get all roles that have access to a resource
     */
    public Set<String> getResourceRoles(String resource) {
        return resourceRoles.getOrDefault(resource, Set.of());
    }
    
    // Permission enum
    public enum Permission {
        READ_ALL,
        WRITE_ALL,
        DELETE_ALL,
        READ_INTEGRATION,
        WRITE_INTEGRATION,
        DELETE_INTEGRATION,
        EXECUTE_INTEGRATION,
        MANAGE_USERS,
        MANAGE_SYSTEM,
        VIEW_METRICS,
        VIEW_LOGS
    }
    
    // Authorization result
    public static class AuthorizationResult {
        private final boolean allowed;
        private final String message;
        
        private AuthorizationResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
        
        public static AuthorizationResult allow() {
            return new AuthorizationResult(true, "Access granted");
        }
        
        public static AuthorizationResult deny(String message) {
            return new AuthorizationResult(false, message);
        }
        
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
    }
}
