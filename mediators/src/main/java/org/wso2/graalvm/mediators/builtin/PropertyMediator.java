package org.wso2.graalvm.mediators.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

/**
 * Property mediator for setting, getting, or removing properties from the integration context.
 */
public class PropertyMediator implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(PropertyMediator.class);
    
    public enum Action {
        SET, GET, REMOVE
    }
    
    public enum Scope {
        DEFAULT, TRANSPORT, REGISTRY
    }
    
    private final String name;
    private Action action = Action.SET;
    private String propertyName;
    private Object propertyValue;
    private String expression;
    private Scope scope = Scope.DEFAULT;
    
    public PropertyMediator(String name) {
        this.name = name;
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        try {
            switch (action) {
                case SET:
                    return setProperty(context);
                case GET:
                    return getProperty(context);
                case REMOVE:
                    return removeProperty(context);
                default:
                    logger.warn("Unknown action: {}", action);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error in PropertyMediator", e);
            return false;
        }
    }
    
    private boolean setProperty(IntegrationContext context) {
        if (propertyName == null || propertyName.isEmpty()) {
            logger.error("Property name is required for SET action");
            return false;
        }
        
        Object valueToSet = propertyValue;
        
        // If expression is provided, evaluate it (simplified version)
        if (expression != null && !expression.isEmpty()) {
            valueToSet = evaluateExpression(expression, context);
        }
        
        switch (scope) {
            case DEFAULT:
                context.setProperty(propertyName, valueToSet);
                break;
            case TRANSPORT:
                context.setHeader(propertyName, valueToSet);
                break;
            case REGISTRY:
                // For registry scope, we might store in a different location
                context.setProperty("registry." + propertyName, valueToSet);
                break;
        }
        
        logger.debug("Set property {} = {} in scope {}", propertyName, valueToSet, scope);
        return true;
    }
    
    private boolean getProperty(IntegrationContext context) {
        if (propertyName == null || propertyName.isEmpty()) {
            logger.error("Property name is required for GET action");
            return false;
        }
        
        Object value = null;
        
        switch (scope) {
            case DEFAULT:
                value = context.getProperty(propertyName);
                break;
            case TRANSPORT:
                value = context.getHeader(propertyName);
                break;
            case REGISTRY:
                value = context.getProperty("registry." + propertyName);
                break;
        }
        
        logger.debug("Got property {} = {} from scope {}", propertyName, value, scope);
        
        // Store the retrieved value back in the context for further processing
        context.setProperty("retrieved." + propertyName, value);
        return true;
    }
    
    private boolean removeProperty(IntegrationContext context) {
        if (propertyName == null || propertyName.isEmpty()) {
            logger.error("Property name is required for REMOVE action");
            return false;
        }
        
        switch (scope) {
            case DEFAULT:
                context.removeProperty(propertyName);
                break;
            case TRANSPORT:
                context.removeHeader(propertyName);
                break;
            case REGISTRY:
                context.removeProperty("registry." + propertyName);
                break;
        }
        
        logger.debug("Removed property {} from scope {}", propertyName, scope);
        return true;
    }
    
    private Object evaluateExpression(String expression, IntegrationContext context) {
        // Simplified expression evaluation
        // In a full implementation, this would support XPath, JSONPath, etc.
        
        if (expression.startsWith("$ctx:")) {
            String propName = expression.substring(5);
            return context.getProperty(propName);
        } else if (expression.startsWith("$header:")) {
            String headerName = expression.substring(8);
            return context.getHeader(headerName);
        } else if (expression.equals("$body")) {
            return context.getPayload();
        } else {
            // Return the expression as literal value
            return expression;
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void initialize(MediatorConfig config) {
        if (config.hasProperty("action")) {
            try {
                this.action = Action.valueOf(config.getProperty("action").toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid action: {}, using SET", config.getProperty("action"));
                this.action = Action.SET;
            }
        }
        
        if (config.hasProperty("name")) {
            this.propertyName = config.getProperty("name");
        }
        
        if (config.hasProperty("value")) {
            this.propertyValue = config.getProperty("value");
        }
        
        if (config.hasProperty("expression")) {
            this.expression = config.getProperty("expression");
        }
        
        if (config.hasProperty("scope")) {
            try {
                this.scope = Scope.valueOf(config.getProperty("scope").toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid scope: {}, using DEFAULT", config.getProperty("scope"));
                this.scope = Scope.DEFAULT;
            }
        }
    }
    
    // Setters for programmatic configuration
    public PropertyMediator setAction(Action action) {
        this.action = action;
        return this;
    }
    
    public PropertyMediator setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }
    
    public PropertyMediator setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
        return this;
    }
    
    public PropertyMediator setExpression(String expression) {
        this.expression = expression;
        return this;
    }
    
    public PropertyMediator setScope(Scope scope) {
        this.scope = scope;
        return this;
    }
}
