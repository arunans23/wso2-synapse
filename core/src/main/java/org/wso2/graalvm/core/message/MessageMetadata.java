package org.wso2.graalvm.core.message;

import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.time.Instant;

/**
 * Contains metadata associated with a message including headers, properties, and system information.
 */
public interface MessageMetadata {
    
    /**
     * Get HTTP headers or message headers.
     * @return map of headers
     */
    Map<String, String> getHeaders();
    
    /**
     * Get a specific header value.
     * @param name the header name (case-insensitive)
     * @return the header value if present
     */
    Optional<String> getHeader(String name);
    
    /**
     * Get all header names.
     * @return set of header names
     */
    Set<String> getHeaderNames();
    
    /**
     * Get message properties set during processing.
     * @return map of properties
     */
    Map<String, Object> getProperties();
    
    /**
     * Get a specific property value.
     * @param name the property name
     * @return the property value if present
     */
    Optional<Object> getProperty(String name);
    
    /**
     * Get a typed property value.
     * @param name the property name
     * @param type the expected type
     * @return the typed property value if present and of correct type
     */
    <T> Optional<T> getProperty(String name, Class<T> type);
    
    /**
     * Get message correlation ID for tracing.
     * @return correlation ID if available
     */
    Optional<String> getCorrelationId();
    
    /**
     * Get unique message ID.
     * @return message ID
     */
    String getMessageId();
    
    /**
     * Get message creation timestamp.
     * @return creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Get the source endpoint or system that sent this message.
     * @return source endpoint information
     */
    Optional<String> getSource();
    
    /**
     * Get the target endpoint this message is intended for.
     * @return target endpoint information
     */
    Optional<String> getTarget();
    
    /**
     * Get message priority if specified.
     * @return message priority (higher numbers = higher priority)
     */
    Optional<Integer> getPriority();
    
    /**
     * Get message expiration time if specified.
     * @return expiration timestamp
     */
    Optional<Instant> getExpiresAt();
    
    /**
     * Check if the message has expired.
     * @return true if the message has expired
     */
    default boolean isExpired() {
        return getExpiresAt()
            .map(expires -> Instant.now().isAfter(expires))
            .orElse(false);
    }
    
    /**
     * Get transport-specific metadata.
     * @return transport metadata
     */
    Map<String, Object> getTransportMetadata();
    
    /**
     * Create a builder for modifying metadata.
     * @return metadata builder
     */
    Builder toBuilder();

    /**
     * Builder interface for creating MessageMetadata instances.
     */
    interface Builder {
        Builder messageId(String messageId);
        Builder withMessageId(String messageId);
        Builder correlationId(String correlationId);
        Builder withCorrelationId(String correlationId);
        Builder createdAt(Instant createdAt);
        Builder withCreatedAt(Instant createdAt);
        Builder source(String source);
        Builder withSource(String source);
        Builder target(String target);
        Builder withTarget(String target);
        Builder priority(Integer priority);
        Builder withPriority(int priority);
        Builder expiresAt(Instant expiresAt);
        Builder withExpiresAt(Instant expiresAt);
        Builder addHeader(String name, String value);
        Builder addHeaders(Map<String, String> headers);
        Builder withHeader(String name, String value);
        Builder withHeaders(Map<String, String> headers);
        Builder addProperty(String name, Object value);
        Builder addProperties(Map<String, Object> properties);
        Builder withProperty(String name, Object value);
        Builder withProperties(Map<String, Object> properties);
        Builder withTransportMetadata(Map<String, Object> transportMetadata);
        Builder traceId(String traceId);
        Builder spanId(String spanId);
        Builder parentSpanId(String parentSpanId);
        MessageMetadata build();
    }
}
