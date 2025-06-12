package org.wso2.graalvm.core.message;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of the Message interface with streaming support and lazy evaluation.
 */
public class DefaultMessage implements Message {
    
    private final ContentType contentType;
    private final Charset charset;
    private final long contentLength;
    private final MessageMetadata metadata;
    
    // Streaming state
    private InputStream payloadStream;
    private byte[] cachedBytes;
    private String cachedText;
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    // Lazy-loaded components
    private volatile StructuredContent structuredContent;
    private volatile StreamingContent streamingContent;
    
    // Configuration
    private final boolean lazyLoading;
    private final int bufferSize;
    
    public DefaultMessage(ContentType contentType, InputStream payloadStream, MessageMetadata metadata) {
        this(contentType, payloadStream, metadata, -1, true, 8192);
    }
    
    public DefaultMessage(ContentType contentType, InputStream payloadStream, MessageMetadata metadata, 
                         long contentLength, boolean lazyLoading, int bufferSize) {
        this.contentType = contentType;
        this.charset = contentType.getCharset() != null ? 
            Charset.forName(contentType.getCharset()) : Charset.defaultCharset();
        this.payloadStream = payloadStream;
        this.metadata = metadata;
        this.contentLength = contentLength;
        this.lazyLoading = lazyLoading;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public ContentType getContentType() {
        return contentType;
    }
    
    @Override
    public Charset getCharset() {
        return charset;
    }
    
    @Override
    public long getContentLength() {
        return contentLength;
    }
    
    @Override
    public boolean isConsumed() {
        return consumed.get();
    }
    
    @Override
    public boolean isEmpty() {
        if (contentLength == 0) {
            return true;
        }
        
        // If we have cached content, check if it's empty
        if (cachedBytes != null) {
            return cachedBytes.length == 0;
        }
        
        if (cachedText != null) {
            return cachedText.isEmpty();
        }
        
        // Can't determine without consuming the stream
        return false;
    }
    
    @Override
    public synchronized InputStream getPayloadStream() throws MessageException {
        if (closed.get()) {
            throw new MessageException(MessageException.ErrorCode.IO_ERROR, "Message has been closed");
        }
        
        if (consumed.get()) {
            // Return cached content as stream
            if (cachedBytes != null) {
                return new ByteArrayInputStream(cachedBytes);
            } else {
                throw new MessageException(MessageException.ErrorCode.STREAM_CONSUMED, 
                    "Stream has been consumed and no cached content available");
            }
        }
        
        // For first access, cache the content if possible
        if (cachedBytes == null && payloadStream != null) {
            try {
                cacheContent();
                return new ByteArrayInputStream(cachedBytes);
            } catch (IOException e) {
                // If caching fails, return original stream but mark as consumed
                consumed.set(true);
                return payloadStream;
            }
        }
        
        return payloadStream;
    }
    
    private void cacheContent() throws IOException {
        if (payloadStream != null && cachedBytes == null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[bufferSize];
            int bytesRead;
            
            while ((bytesRead = payloadStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            
            cachedBytes = buffer.toByteArray();
            consumed.set(true);
            
            try {
                payloadStream.close();
            } catch (IOException e) {
                // Log but don't fail
            }
        }
    }
    
    @Override
    public synchronized byte[] getPayloadBytes() throws MessageException {
        // Allow access to cached content even if closed
        if (closed.get() && cachedBytes == null) {
            throw new MessageException(MessageException.ErrorCode.IO_ERROR, "Message has been closed");
        }
        
        if (cachedBytes != null) {
            return cachedBytes.clone();
        }
        
        try {
            cacheContent();
            return cachedBytes.clone();
        } catch (IOException e) {
            throw new MessageException(MessageException.ErrorCode.IO_ERROR, 
                "Failed to read message payload", e);
        }
    }
    
    @Override
    public String getPayloadText() throws MessageException {
        return getPayloadText(charset);
    }
    
    @Override
    public synchronized String getPayloadText(Charset charset) throws MessageException {
        // Allow access to cached content even if closed
        if (closed.get() && cachedText == null && cachedBytes == null) {
            throw new MessageException(MessageException.ErrorCode.IO_ERROR, "Message has been closed");
        }
        
        if (cachedText != null && charset.equals(this.charset)) {
            return cachedText;
        }
        
        byte[] bytes = getPayloadBytes();
        try {
            String text = new String(bytes, charset);
            
            // Cache if using the message's default charset
            if (charset.equals(this.charset)) {
                cachedText = text;
            }
            
            return text;
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.ENCODING_ERROR, 
                "Failed to decode message payload with charset: " + charset.name(), e);
        }
    }
    
    @Override
    public StructuredContent getStructuredContent() {
        if (!lazyLoading) {
            // Eager loading - create immediately
            StructuredContent content = createStructuredContent();
            // Mark as consumed since we accessed the content
            consumed.set(true);
            return content;
        }
        
        // Lazy loading with double-checked locking
        if (structuredContent == null) {
            synchronized (this) {
                if (structuredContent == null) {
                    structuredContent = createStructuredContent();
                    // Mark as consumed since we accessed the content
                    consumed.set(true);
                }
            }
        }
        return structuredContent;
    }
    
    @Override
    public StreamingContent getStreamingContent() {
        if (!lazyLoading) {
            return createStreamingContent();
        }
        
        if (streamingContent == null) {
            synchronized (this) {
                if (streamingContent == null) {
                    streamingContent = createStreamingContent();
                }
            }
        }
        return streamingContent;
    }
    
    @Override
    public MessageMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public ValidationResult validate(Schema schema) {
        if (!schema.isCompatibleWith(contentType)) {
            return ValidationResult.invalid("Schema is not compatible with content type: " + contentType);
        }
        
        try {
            String content = getPayloadText();
            return schema.validate(content);
        } catch (MessageException e) {
            return ValidationResult.invalid("Failed to read content for validation: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isValid() {
        try {
            // Basic structural validation based on content type
            if (contentType.isJson()) {
                // Try to parse as JSON
                getStructuredContent().asJson();
                return true;
            } else if (contentType.isXml()) {
                // Try to parse as XML
                getStructuredContent().asXml();
                return true;
            } else {
                // For other types, consider valid if we can read the content
                getPayloadText();
                return true;
            }
        } catch (MessageException e) {
            return false;
        }
    }
    
    @Override
    public MessageBuilder toBuilder() {
        return new DefaultMessageBuilder()
            .withContentType(contentType)
            .withCharset(charset)
            .withMetadata(metadata);
    }
    
    @Override
    public Message copy() throws MessageException {
        // Ensure content is cached
        byte[] bytes = getPayloadBytes();
        
        return new DefaultMessage(
            contentType,
            new ByteArrayInputStream(bytes),
            metadata,
            bytes.length,
            lazyLoading,
            bufferSize
        );
    }
    
    @Override
    public CompletableFuture<Message> transformTo(ContentType targetContentType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // This is a basic implementation - a full transformer would be more sophisticated
                MessageTransformer transformer = MessageTransformerRegistry.getTransformer(contentType, targetContentType);
                return transformer.transform(this, targetContentType);
            } catch (Exception e) {
                throw new RuntimeException(new MessageException(MessageException.ErrorCode.TRANSFORMATION_ERROR, 
                    "Failed to transform from " + contentType + " to " + targetContentType, e));
            }
        });
    }
    
    @Override
    public synchronized void close() {
        if (closed.get()) {
            return;
        }
        
        closed.set(true);
        
        if (payloadStream != null && !consumed.get()) {
            try {
                payloadStream.close();
            } catch (IOException e) {
                // Log but don't throw
            }
        }
        
        // Don't clear cached content - it should remain accessible after close
        // Only clear non-cached resources
        structuredContent = null;
        streamingContent = null;
    }
    
    private StructuredContent createStructuredContent() {
        return new DefaultStructuredContent(this);
    }
    
    private StreamingContent createStreamingContent() {
        return new DefaultStreamingContent(this);
    }
    
}
