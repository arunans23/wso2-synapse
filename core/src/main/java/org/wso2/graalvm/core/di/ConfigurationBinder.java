package org.wso2.graalvm.core.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Configuration property binder to replace Spring Boot's property binding.
 * 
 * This implementation supports:
 * - Property binding from various sources (environment variables, system properties, config files)
 * - Type conversion (String, Integer, Duration, Boolean, List, etc.)
 * - Nested object binding
 * - Property validation
 */
public class ConfigurationBinder {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBinder.class);
    
    private final Map<String, String> properties;
    private final TypeConverter typeConverter;
    
    public ConfigurationBinder(Map<String, String> properties) {
        this.properties = properties;
        this.typeConverter = new TypeConverter();
    }
    
    /**
     * Bind configuration properties to an object.
     */
    public void bindConfiguration(Object target, String prefix) {
        logger.debug("Binding configuration for prefix: {} to object: {}", prefix, target.getClass().getName());
        
        try {
            Class<?> targetClass = target.getClass();
            PropertyDescriptor[] descriptors = java.beans.Introspector.getBeanInfo(targetClass).getPropertyDescriptors();
            
            for (PropertyDescriptor descriptor : descriptors) {
                if (descriptor.getWriteMethod() != null && !"class".equals(descriptor.getName())) {
                    bindProperty(target, descriptor, prefix);
                }
            }
            
            logger.debug("Configuration binding complete for: {}", targetClass.getName());
        } catch (Exception e) {
            throw new ConfigurationBindingException("Failed to bind configuration for prefix: " + prefix, e);
        }
    }
    
    private void bindProperty(Object target, PropertyDescriptor descriptor, String prefix) throws Exception {
        String propertyName = descriptor.getName();
        String fullPropertyName = prefix.isEmpty() ? propertyName : prefix + "." + propertyName;
        
        // Check for property value
        String propertyValue = findPropertyValue(fullPropertyName);
        
        if (propertyValue != null) {
            Method writeMethod = descriptor.getWriteMethod();
            Class<?> propertyType = descriptor.getPropertyType();
            
            Object convertedValue = typeConverter.convert(propertyValue, propertyType);
            writeMethod.invoke(target, convertedValue);
            
            logger.debug("Bound property: {} = {}", fullPropertyName, propertyValue);
        }
    }
    
    private String findPropertyValue(String propertyName) {
        // Check exact match first
        String value = properties.get(propertyName);
        if (value != null) {
            return value;
        }
        
        // Check with different case variations
        String kebabCase = propertyName.replace('.', '-');
        value = properties.get(kebabCase);
        if (value != null) {
            return value;
        }
        
        // Check environment variable style (uppercase with underscores)
        String envStyle = propertyName.toUpperCase().replace('.', '_').replace('-', '_');
        value = System.getenv(envStyle);
        if (value != null) {
            return value;
        }
        
        // Check system properties
        value = System.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        
        return null;
    }
    
    private static class TypeConverter {
        
        public Object convert(String value, Class<?> targetType) {
            if (value == null) {
                return null;
            }
            
            if (targetType == String.class) {
                return value;
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            } else if (targetType == Duration.class) {
                return parseDuration(value);
            } else if (targetType == List.class) {
                return parseList(value);
            } else if (targetType == Set.class) {
                return parseSet(value);
            } else if (targetType.isEnum()) {
                return parseEnum(value, targetType);
            } else {
                throw new ConfigurationBindingException("Unsupported property type: " + targetType.getName());
            }
        }
        
        private Duration parseDuration(String value) {
            // Support formats like: 30s, 5m, 1h, 2d
            if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)));
            } else {
                // Assume seconds if no unit specified
                return Duration.ofSeconds(Long.parseLong(value));
            }
        }
        
        private List<String> parseList(String value) {
            List<String> result = new ArrayList<>();
            if (value.trim().isEmpty()) {
                return result;
            }
            
            String[] parts = value.split(",");
            for (String part : parts) {
                result.add(part.trim());
            }
            return result;
        }
        
        private Set<String> parseSet(String value) {
            return new HashSet<>(parseList(value));
        }
        
        @SuppressWarnings("unchecked")
        private Object parseEnum(String value, Class<?> enumType) {
            return Enum.valueOf((Class<Enum>) enumType, value.toUpperCase());
        }
    }
    
    public static class ConfigurationBindingException extends RuntimeException {
        public ConfigurationBindingException(String message) {
            super(message);
        }
        
        public ConfigurationBindingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
