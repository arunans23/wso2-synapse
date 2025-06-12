package org.wso2.graalvm.core.message;

import org.wso2.graalvm.core.annotation.MediatorComponent;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating MessageMetadata instances.
 */
@MediatorComponent
public class MessageMetadataFactory {
    
    /**
     * Create default metadata with generated message ID and current timestamp.
     * @return metadata builder
     */
    public MessageMetadata.Builder createDefault() {
        return new DefaultMessageMetadataBuilder()
            .withMessageId(generateMessageId())
            .withCreatedAt(Instant.now());
    }
    
    /**
     * Create metadata with specified message ID.
     * @param messageId the message ID
     * @return metadata builder
     */
    public MessageMetadata.Builder create(String messageId) {
        return new DefaultMessageMetadataBuilder()
            .withMessageId(messageId)
            .withCreatedAt(Instant.now());
    }
    
    /**
     * Create metadata from existing metadata (copy).
     * @param existing the existing metadata
     * @return metadata builder
     */
    public MessageMetadata.Builder from(MessageMetadata existing) {
        return existing.toBuilder();
    }
    
    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }
    
    // Default MessageMetadata Builder implementation
    private static class DefaultMessageMetadataBuilder implements MessageMetadata.Builder {
        private String messageId;
        private String correlationId;
        private Instant createdAt;
        private String source;
        private String target;
        private int priority = 0;
        private Instant expiresAt;
        private final Map<String, String> headers = new ConcurrentHashMap<>();
        private final Map<String, Object> properties = new ConcurrentHashMap<>();
        private final Map<String, Object> transportMetadata = new ConcurrentHashMap<>();
        
        @Override
        public MessageMetadata.Builder withMessageId(String messageId) {
            this.messageId = messageId;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withCorrelationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withSource(String source) {
            this.source = source;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withTarget(String target) {
            this.target = target;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withProperty(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withProperties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withTransportMetadata(Map<String, Object> transportMetadata) {
            this.transportMetadata.putAll(transportMetadata);
            return this;
        }
        
        @Override
        public MessageMetadata build() {
            return new DefaultMessageMetadata(
                messageId,
                correlationId,
                createdAt,
                source,
                target,
                priority,
                expiresAt,
                new ConcurrentHashMap<>(headers),
                new ConcurrentHashMap<>(properties),
                new ConcurrentHashMap<>(transportMetadata)
            );
        }

        @Override
        public MessageMetadata.Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder source(String source) {
            this.source = source;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder target(String target) {
            this.target = target;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder priority(Integer priority) {
            this.priority = priority != null ? priority : 0;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        @Override
        public MessageMetadata.Builder addHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder addHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder addProperty(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder addProperties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder traceId(String traceId) {
            this.properties.put("trace.id", traceId);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder spanId(String spanId) {
            this.properties.put("span.id", spanId);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder parentSpanId(String parentSpanId) {
            this.properties.put("parent.span.id", parentSpanId);
            return this;
        }
    }
    
    // Default MessageMetadata implementation
    private static class DefaultMessageMetadata implements MessageMetadata {
        private final String messageId;
        private final String correlationId;
        private final Instant createdAt;
        private final String source;
        private final String target;
        private final int priority;
        private final Instant expiresAt;
        private final Map<String, String> headers;
        private final Map<String, Object> properties;
        private final Map<String, Object> transportMetadata;
        
        public DefaultMessageMetadata(String messageId, String correlationId, Instant createdAt,
                                    String source, String target, int priority, Instant expiresAt,
                                    Map<String, String> headers, Map<String, Object> properties,
                                    Map<String, Object> transportMetadata) {
            this.messageId = messageId;
            this.correlationId = correlationId;
            this.createdAt = createdAt;
            this.source = source;
            this.target = target;
            this.priority = priority;
            this.expiresAt = expiresAt;
            this.headers = headers;
            this.properties = properties;
            this.transportMetadata = transportMetadata;
        }
        
        @Override
        public String getMessageId() { return messageId; }
        
        @Override
        public Optional<String> getCorrelationId() { return Optional.ofNullable(correlationId); }
        
        @Override
        public Instant getCreatedAt() { return createdAt; }
        
        @Override
        public Optional<String> getSource() { return Optional.ofNullable(source); }
        
        @Override
        public Optional<String> getTarget() { return Optional.ofNullable(target); }
        
        @Override
        public Optional<Integer> getPriority() { return Optional.ofNullable(priority); }
        
        @Override
        public Optional<Instant> getExpiresAt() { return Optional.ofNullable(expiresAt); }
        
        @Override
        public Map<String, String> getHeaders() { return headers; }
        
        @Override
        public Map<String, Object> getProperties() { return properties; }
        
        @Override
        public Map<String, Object> getTransportMetadata() { return transportMetadata; }
        
        @Override
        public MessageMetadata.Builder toBuilder() {
            return new DefaultMessageMetadataBuilder()
                .withMessageId(messageId)
                .withCorrelationId(correlationId)
                .withCreatedAt(createdAt)
                .withSource(source)
                .withTarget(target)
                .withPriority(priority)
                .withExpiresAt(expiresAt)
                .withHeaders(headers)
                .withProperties(properties)
                .withTransportMetadata(transportMetadata);
        }

        @Override
        public Optional<Object> getProperty(String name) { 
            return Optional.ofNullable(properties.get(name)); 
        }
        
        @Override
        public <T> Optional<T> getProperty(String name, Class<T> type) { 
            Object value = properties.get(name);
            if (value != null && type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            return Optional.empty();
        }

        @Override
        public Set<String> getHeaderNames() { 
            return headers.keySet(); 
        }

        @Override
        public Optional<String> getHeader(String name) { 
            return Optional.ofNullable(headers.get(name)); 
        }
    }
}
