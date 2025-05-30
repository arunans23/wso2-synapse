package org.wso2.graalvm.core.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integration context for message processing.
 * Replaces the OSGi-based MessageContext with a lightweight, thread-safe alternative.
 */
public class IntegrationContext {
    
    private final String contextId;
    private final long timestamp;
    private final Map<String, Object> properties;
    private final Map<String, Object> messageHeaders;
    private Object messagePayload;
    private Exception fault;
    
    private static final AtomicLong CONTEXT_COUNTER = new AtomicLong(0);
    
    public IntegrationContext() {
        this.contextId = "ctx-" + CONTEXT_COUNTER.incrementAndGet();
        this.timestamp = System.currentTimeMillis();
        this.properties = new ConcurrentHashMap<>();
        this.messageHeaders = new ConcurrentHashMap<>();
    }
    
    public IntegrationContext(String contextId) {
        this.contextId = contextId;
        this.timestamp = System.currentTimeMillis();
        this.properties = new ConcurrentHashMap<>();
        this.messageHeaders = new ConcurrentHashMap<>();
    }
    
    // Context identification
    public String getContextId() {
        return contextId;
    }
    
    public long getTimestamp() {
        return timestamp;
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
    
    public void removeProperty(String key) {
        properties.remove(key);
    }
    
    public Map<String, Object> getProperties() {
        return Map.copyOf(properties);
    }
    
    // Message header management
    public void setHeader(String name, Object value) {
        messageHeaders.put(name, value);
    }
    
    public Object getHeader(String name) {
        return messageHeaders.get(name);
    }
    
    public void removeHeader(String name) {
        messageHeaders.remove(name);
    }
    
    public Map<String, Object> getHeaders() {
        return Map.copyOf(messageHeaders);
    }
    
    // Message payload management
    public void setPayload(Object payload) {
        this.messagePayload = payload;
    }
    
    public Object getPayload() {
        return messagePayload;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> type) {
        if (messagePayload != null && type.isAssignableFrom(messagePayload.getClass())) {
            return (T) messagePayload;
        }
        return null;
    }
    
    // Fault management
    public void setFault(Exception fault) {
        this.fault = fault;
    }
    
    public Exception getFault() {
        return fault;
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
        copy.messageHeaders.putAll(this.messageHeaders);
        copy.messagePayload = this.messagePayload;
        copy.fault = this.fault;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("IntegrationContext{id='%s', timestamp=%d, properties=%d, headers=%d, hasFault=%s}", 
                contextId, timestamp, properties.size(), messageHeaders.size(), hasFault());
    }
}
