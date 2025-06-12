package org.wso2.graalvm.core.message;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Generic message interface that supports streaming and lazy evaluation.
 * 
 * This interface provides a unified abstraction for all message types (JSON, XML, Binary, Text)
 * with support for:
 * - Lazy transformation (convert only when accessed)
 * - Streaming support for large payloads
 * - Memory-efficient processing
 * - Content-type detection and validation
 * - Immutable design with builder pattern for modifications
 */
public interface Message {
    
    /**
     * Get the content type of this message.
     * @return the content type
     */
    ContentType getContentType();
    
    /**
     * Get the charset encoding of this message.
     * @return the charset, defaults to UTF-8 if not specified
     */
    default Charset getCharset() {
        return StandardCharsets.UTF_8;
    }
    
    /**
     * Get the content length if known.
     * @return the content length in bytes, or -1 if unknown
     */
    long getContentLength();
    
    /**
     * Check if the message has been consumed (stream read).
     * @return true if the message stream has been consumed
     */
    boolean isConsumed();
    
    /**
     * Check if the message is empty.
     * @return true if the message has no content
     */
    boolean isEmpty();
    
    /**
     * Get the raw payload as an InputStream.
     * Warning: This consumes the stream if it hasn't been consumed yet.
     * @return the payload as an InputStream
     * @throws MessageException if the stream cannot be read
     */
    InputStream getPayloadStream() throws MessageException;
    
    /**
     * Get the payload as a byte array.
     * This will consume the entire stream and build the byte array.
     * @return the payload as bytes
     * @throws MessageException if the content cannot be read
     */
    byte[] getPayloadBytes() throws MessageException;
    
    /**
     * Get the payload as a string using the message's charset.
     * This will consume the stream if not already consumed.
     * @return the payload as a string
     * @throws MessageException if the content cannot be read or converted
     */
    String getPayloadText() throws MessageException;
    
    /**
     * Get the payload as a string using the specified charset.
     * @param charset the charset to use for conversion
     * @return the payload as a string
     * @throws MessageException if the content cannot be read or converted
     */
    String getPayloadText(Charset charset) throws MessageException;
    
    /**
     * Get structured content accessor for this message.
     * This provides lazy, type-specific access to the content.
     * @return the structured content accessor
     */
    StructuredContent getStructuredContent();
    
    /**
     * Create a streaming accessor for large content processing.
     * @return the streaming accessor
     */
    StreamingContent getStreamingContent();
    
    /**
     * Get message metadata (headers, properties, etc.).
     * @return the message metadata
     */
    MessageMetadata getMetadata();
    
    /**
     * Validate the message against the specified schema.
     * @param schema the schema to validate against
     * @return validation result
     */
    ValidationResult validate(Schema schema);
    
    /**
     * Check if the message is valid according to its content type.
     * @return true if the message is structurally valid
     */
    boolean isValid();
    
    /**
     * Create a new message builder based on this message.
     * This allows for immutable modifications.
     * @return a message builder
     */
    MessageBuilder toBuilder();
    
    /**
     * Create a copy of this message.
     * If the original stream was consumed, the copy will contain the cached content.
     * @return a copy of this message
     * @throws MessageException if the message cannot be copied
     */
    Message copy() throws MessageException;
    
    /**
     * Transform this message to another content type.
     * This is a convenience method for common transformations.
     * @param targetContentType the target content type
     * @return a future containing the transformed message
     */
    CompletableFuture<Message> transformTo(ContentType targetContentType);
    
    /**
     * Release any resources associated with this message.
     * Should be called when the message is no longer needed.
     */
    void close();
}
