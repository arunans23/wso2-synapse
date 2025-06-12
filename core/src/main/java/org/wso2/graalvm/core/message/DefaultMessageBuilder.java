package org.wso2.graalvm.core.message;

import org.wso2.graalvm.core.annotation.MediatorComponent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of MessageBuilder for constructing Message instances.
 */
@MediatorComponent
public class DefaultMessageBuilder implements MessageBuilder {
    
    private ContentType contentType;
    private Charset charset = StandardCharsets.UTF_8;
    private InputStream payload;
    private long contentLength = -1;
    private boolean lazyLoading = true;
    private int bufferSize = 8192;
    
    // Metadata fields
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
    public MessageBuilder withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }
    
    @Override
    public MessageBuilder withContentType(String contentType) {
        this.contentType = ContentType.parse(contentType);
        return this;
    }
    
    @Override
    public MessageBuilder withCharset(Charset charset) {
        this.charset = charset;
        return this;
    }
    
    @Override
    public MessageBuilder withPayload(InputStream payload) {
        this.payload = payload;
        return this;
    }
    
    @Override
    public MessageBuilder withPayload(byte[] payload) {
        this.payload = new ByteArrayInputStream(payload);
        this.contentLength = payload.length;
        return this;
    }
    
    @Override
    public MessageBuilder withPayload(String payload) {
        return withPayload(payload, charset);
    }
    
    @Override
    public MessageBuilder withPayload(String payload, Charset charset) {
        byte[] bytes = payload.getBytes(charset);
        this.payload = new ByteArrayInputStream(bytes);
        this.contentLength = bytes.length;
        this.charset = charset;
        return this;
    }
    
    @Override
    public MessageBuilder withContentLength(long length) {
        this.contentLength = length;
        return this;
    }
    
    @Override
    public MessageBuilder withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }
    
    @Override
    public MessageBuilder withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }
    
    @Override
    public MessageBuilder withProperty(String name, Object value) {
        properties.put(name, value);
        return this;
    }
    
    @Override
    public MessageBuilder withProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
        return this;
    }
    
    @Override
    public MessageBuilder withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
    
    @Override
    public MessageBuilder withMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
    
    @Override
    public MessageBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    @Override
    public MessageBuilder withSource(String source) {
        this.source = source;
        return this;
    }
    
    @Override
    public MessageBuilder withTarget(String target) {
        this.target = target;
        return this;
    }
    
    @Override
    public MessageBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }
    
    @Override
    public MessageBuilder withExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }
    
    @Override
    public MessageBuilder withTransportMetadata(String key, Object value) {
        transportMetadata.put(key, value);
        return this;
    }
    
    @Override
    public MessageBuilder withLazyLoading(boolean lazy) {
        this.lazyLoading = lazy;
        return this;
    }
    
    @Override
    public MessageBuilder withBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }
    
    @Override
    public Message build() throws MessageException {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            throw new MessageException(MessageException.ErrorCode.VALIDATION_ERROR, 
                "Message validation failed: " + validation.getErrors());
        }
        
        // Apply charset to content type if specified
        ContentType finalContentType = contentType;
        if (charset != null && !charset.equals(StandardCharsets.UTF_8)) {
            finalContentType = contentType.withCharset(charset.name());
        }
        
        // Build metadata
        MessageMetadata metadata = createMetadata();
        
        return new DefaultMessage(
            finalContentType,
            payload,
            metadata,
            contentLength,
            lazyLoading,
            bufferSize
        );
    }
    
    @Override
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (contentType == null) {
            result.addError("Content type is required");
        }
        
        if (payload == null) {
            result.addError("Payload is required");
        }
        
        if (messageId == null || messageId.trim().isEmpty()) {
            result.addWarning("Message ID is not set - will be auto-generated");
        }
        
        if (createdAt == null) {
            result.addWarning("Creation timestamp is not set - will be auto-generated");
        }
        
        return result.build();
    }
    
    // Helper method to create metadata
    public MessageBuilder withMetadata(MessageMetadata metadata) {
        if (metadata != null) {
            this.messageId = metadata.getMessageId();
            this.correlationId = metadata.getCorrelationId().orElse(null);
            this.createdAt = metadata.getCreatedAt();
            this.source = metadata.getSource().orElse(null);
            this.target = metadata.getTarget().orElse(null);
            this.priority = metadata.getPriority().orElse(null);
            this.expiresAt = metadata.getExpiresAt().orElse(null);
            this.headers.putAll(metadata.getHeaders());
            this.properties.putAll(metadata.getProperties());
            this.transportMetadata.putAll(metadata.getTransportMetadata());
        }
        return this;
    }
    
    private MessageMetadata createMetadata() {
        return new DefaultMessageMetadata(
            messageId != null ? messageId : generateMessageId(),
            correlationId,
            createdAt != null ? createdAt : Instant.now(),
            source,
            target,
            priority,
            expiresAt,
            new ConcurrentHashMap<>(headers),
            new ConcurrentHashMap<>(properties),
            new ConcurrentHashMap<>(transportMetadata)
        );
    }
    
    private String generateMessageId() {
        return "msg-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
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
            headers.put(name, value);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        @Override
        public MessageMetadata.Builder withProperty(String name, Object value) {
            properties.put(name, value);
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
}
