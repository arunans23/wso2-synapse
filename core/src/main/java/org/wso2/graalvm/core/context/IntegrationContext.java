package org.wso2.graalvm.core.context;

import org.wso2.graalvm.core.message.Message;
import org.wso2.graalvm.core.message.ContentType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Instant;
import java.util.Optional;

/**
 * Integration context for message processing.
 * Replaces the OSGi-based MessageContext with a lightweight, thread-safe alternative.
 * Now uses the generic Message interface for payload handling.
 */
public class IntegrationContext {
    
    private final String contextId;
    private final Instant timestamp;
    private final Map<String, Object> properties;
    private Message message;
    private Exception fault;
    
    private static final AtomicLong CONTEXT_COUNTER = new AtomicLong(0);
    
    public IntegrationContext() {
        this.contextId = "ctx-" + CONTEXT_COUNTER.incrementAndGet();
        this.timestamp = Instant.now();
        this.properties = new ConcurrentHashMap<>();
    }
    
    public IntegrationContext(String contextId) {
        this.contextId = contextId;
        this.timestamp = Instant.now();
        this.properties = new ConcurrentHashMap<>();
    }
    
    public IntegrationContext(Message message) {
        this();
        this.message = message;
    }
    
    // Context identification
    public String getContextId() {
        return contextId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    // Message management - now uses the generic Message interface
    public void setMessage(Message message) {
        this.message = message;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public Optional<Message> getMessageOptional() {
        return Optional.ofNullable(message);
    }
    
    // Property management
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    public Optional<Object> getPropertyOptional(String key) {
        return Optional.ofNullable(properties.get(key));
    }
    
    public <T> Optional<T> getPropertyOptional(String key, Class<T> type) {
        return Optional.ofNullable(getProperty(key, type));
    }
    
    public void removeProperty(String key) {
        properties.remove(key);
    }
    
    public Map<String, Object> getProperties() {
        return Map.copyOf(properties);
    }
    
    // Header management
    private final Map<String, Object> headers = new ConcurrentHashMap<>();
    
    public void setHeader(String key, Object value) {
        headers.put(key, value);
    }
    
    public Object getHeader(String key) {
        return headers.get(key);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getHeader(String key, Class<T> type) {
        Object value = headers.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    public void removeHeader(String key) {
        headers.remove(key);
    }
    
    public Map<String, Object> getHeaders() {
        return Map.copyOf(headers);
    }
    
    // Payload convenience methods
    public Object getPayload() {
        if (message != null) {
            try {
                // Return the payload as text for string/JSON/XML content types
                ContentType contentType = message.getContentType();
                if (contentType.isText() || contentType.isJson() || contentType.isXml()) {
                    return message.getPayloadText();
                } else {
                    // Return as byte array for binary content
                    return message.getPayloadBytes();
                }
            } catch (Exception e) {
                // If message can't be read, check if we have it stored as property
                return getProperty("__payload");
            }
        }
        // Fallback to stored payload property
        return getProperty("__payload");
    }
    
    public void setPayload(Object payload) {
        // Store the payload as a property for compatibility
        setProperty("__payload", payload);
        
        // Try to create a new message if we have a MessageFactory available
        // For now, we'll just store it as a property
        // TODO: Implement proper message creation when MessageFactory is available
    }
    
    public void setPayload(String payload) {
        setProperty("__payload", payload);
    }
    
    public void setPayload(byte[] payload) {
        setProperty("__payload", payload);
    }
    
    public void setPayload(java.io.Serializable payload) {
        setProperty("__payload", payload);
    }
    
    // Convenience methods for common message operations
    public String getCorrelationId() {
        return message != null ? 
            message.getMetadata().getCorrelationId().orElse(contextId) : 
            contextId;
    }
    
    public String getMessageId() {
        return message != null ? 
            message.getMetadata().getMessageId() : 
            contextId;
    }
    
    // Fault management
    public void setFault(Exception fault) {
        this.fault = fault;
    }
    
    public Exception getFault() {
        return fault;
    }
    
    public Optional<Exception> getFaultOptional() {
        return Optional.ofNullable(fault);
    }
    
    public boolean hasFault() {
        return fault != null;
    }
    
    public void clearFault() {
        this.fault = null;
    }
    
    // Utility methods
    public IntegrationContext copy() {
        IntegrationContext copy = new IntegrationContext(this.contextId + "-copy");
        copy.properties.putAll(this.properties);
        copy.headers.putAll(this.headers);
        
        // Copy message if present
        if (this.message != null) {
            try {
                copy.message = this.message.copy();
            } catch (Exception e) {
                // If copy fails, use original (may have implications for streaming)
                copy.message = this.message;
            }
        }
        
        copy.fault = this.fault;
        return copy;
    }
    
    // Resource cleanup
    public void close() {
        if (message != null) {
            message.close();
        }
        properties.clear();
        headers.clear();
    }
    
    @Override
    public String toString() {
        return String.format("IntegrationContext{id='%s', timestamp=%s, properties=%d, hasMessage=%s, hasFault=%s}", 
                contextId, timestamp, properties.size(), message != null, hasFault());
    }
}
