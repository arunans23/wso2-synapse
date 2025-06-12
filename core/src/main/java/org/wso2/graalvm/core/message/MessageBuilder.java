package org.wso2.graalvm.core.message;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Builder for creating and modifying Message instances.
 * Follows the builder pattern for immutable message creation.
 */
public interface MessageBuilder {
    
    /**
     * Set the message content type.
     * @param contentType the content type
     * @return this builder
     */
    MessageBuilder withContentType(ContentType contentType);
    
    /**
     * Set the message content type from a string.
     * @param contentType the content type string (e.g., "application/json")
     * @return this builder
     */
    MessageBuilder withContentType(String contentType);
    
    /**
     * Set the message charset.
     * @param charset the charset
     * @return this builder
     */
    MessageBuilder withCharset(Charset charset);
    
    /**
     * Set the payload from an InputStream.
     * @param payload the payload stream
     * @return this builder
     */
    MessageBuilder withPayload(InputStream payload);
    
    /**
     * Set the payload from a byte array.
     * @param payload the payload bytes
     * @return this builder
     */
    MessageBuilder withPayload(byte[] payload);
    
    /**
     * Set the payload from a string.
     * @param payload the payload string
     * @return this builder
     */
    MessageBuilder withPayload(String payload);
    
    /**
     * Set the payload from a string with specific charset.
     * @param payload the payload string
     * @param charset the charset to use
     * @return this builder
     */
    MessageBuilder withPayload(String payload, Charset charset);
    
    /**
     * Set the content length.
     * @param length the content length in bytes
     * @return this builder
     */
    MessageBuilder withContentLength(long length);
    
    /**
     * Add a header.
     * @param name the header name
     * @param value the header value
     * @return this builder
     */
    MessageBuilder withHeader(String name, String value);
    
    /**
     * Add multiple headers.
     * @param headers the headers map
     * @return this builder
     */
    MessageBuilder withHeaders(Map<String, String> headers);
    
    /**
     * Add a property.
     * @param name the property name
     * @param value the property value
     * @return this builder
     */
    MessageBuilder withProperty(String name, Object value);
    
    /**
     * Add multiple properties.
     * @param properties the properties map
     * @return this builder
     */
    MessageBuilder withProperties(Map<String, Object> properties);
    
    /**
     * Set the correlation ID.
     * @param correlationId the correlation ID
     * @return this builder
     */
    MessageBuilder withCorrelationId(String correlationId);
    
    /**
     * Set the message ID.
     * @param messageId the message ID
     * @return this builder
     */
    MessageBuilder withMessageId(String messageId);
    
    /**
     * Set the creation timestamp.
     * @param createdAt the creation timestamp
     * @return this builder
     */
    MessageBuilder withCreatedAt(Instant createdAt);
    
    /**
     * Set the source endpoint.
     * @param source the source endpoint
     * @return this builder
     */
    MessageBuilder withSource(String source);
    
    /**
     * Set the target endpoint.
     * @param target the target endpoint
     * @return this builder
     */
    MessageBuilder withTarget(String target);
    
    /**
     * Set the message priority.
     * @param priority the priority (higher numbers = higher priority)
     * @return this builder
     */
    MessageBuilder withPriority(int priority);
    
    /**
     * Set the expiration time.
     * @param expiresAt the expiration timestamp
     * @return this builder
     */
    MessageBuilder withExpiresAt(Instant expiresAt);
    
    /**
     * Add transport-specific metadata.
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder
     */
    MessageBuilder withTransportMetadata(String key, Object value);
    
    /**
     * Enable or disable lazy loading for structured content.
     * @param lazy true to enable lazy loading (default)
     * @return this builder
     */
    MessageBuilder withLazyLoading(boolean lazy);
    
    /**
     * Set buffer size for streaming operations.
     * @param bufferSize the buffer size in bytes
     * @return this builder
     */
    MessageBuilder withBufferSize(int bufferSize);
    
    /**
     * Set metadata from an existing MessageMetadata instance.
     * @param metadata the metadata to copy
     * @return this builder
     */
    MessageBuilder withMetadata(MessageMetadata metadata);
    
    /**
     * Build the message instance.
     * @return the constructed message
     * @throws MessageException if the message cannot be built
     */
    Message build() throws MessageException;
    
    /**
     * Validate the builder configuration without building.
     * @return validation result
     */
    ValidationResult validate();
}
