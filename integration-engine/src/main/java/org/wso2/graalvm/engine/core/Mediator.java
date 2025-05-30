package org.wso2.graalvm.engine.core;

import org.wso2.graalvm.core.context.IntegrationContext;

/**
 * Base interface for all mediators in the integration engine.
 * Replaces the OSGi-based mediator framework with a simple interface.
 */
public interface Mediator {
    
    /**
     * Mediates the message contained in the integration context.
     * 
     * @param context The integration context containing the message
     * @return true if mediation was successful, false otherwise
     */
    boolean mediate(IntegrationContext context);
    
    /**
     * Gets the name of this mediator.
     * 
     * @return The mediator name
     */
    String getName();
    
    /**
     * Gets the type of this mediator.
     * 
     * @return The mediator type
     */
    default String getType() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Initializes the mediator with configuration.
     * 
     * @param config The mediator configuration
     */
    default void initialize(MediatorConfig config) {
        // Default implementation does nothing
    }
    
    /**
     * Destroys the mediator and cleans up resources.
     */
    default void destroy() {
        // Default implementation does nothing
    }
    
    /**
     * Configuration interface for mediators.
     */
    interface MediatorConfig {
        
        /**
         * Gets a configuration property value.
         * 
         * @param key The property key
         * @return The property value, or null if not found
         */
        String getProperty(String key);
        
        /**
         * Gets a configuration property value with a default.
         * 
         * @param key The property key
         * @param defaultValue The default value
         * @return The property value, or the default if not found
         */
        default String getProperty(String key, String defaultValue) {
            String value = getProperty(key);
            return value != null ? value : defaultValue;
        }
        
        /**
         * Checks if a property exists.
         * 
         * @param key The property key
         * @return true if the property exists
         */
        default boolean hasProperty(String key) {
            return getProperty(key) != null;
        }
    }
}
