package org.wso2.graalvm.core.message;

import org.wso2.graalvm.core.annotation.MediatorComponent;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.time.Instant;

/**
 * Factory for creating Message instances from various sources.
 * Supports auto-detection of content types and optimized creation for different scenarios.
 */
@MediatorComponent
public class MessageFactory {
    
    private final ContentTypeDetector contentTypeDetector;
    private final MessageMetadataFactory metadataFactory;
    
    public MessageFactory() {
        this.contentTypeDetector = new ContentTypeDetector();
        this.metadataFactory = new MessageMetadataFactory();
    }
    
    /**
     * Create a message from a string with auto-detected content type.
     */
    public Message createFromString(String content) {
        ContentType detectedType = contentTypeDetector.detectFromContent(content);
        return createFromString(content, detectedType, StandardCharsets.UTF_8);
    }
    
    /**
     * Create a message from a string with specified content type.
     */
    public Message createFromString(String content, ContentType contentType) {
        return createFromString(content, contentType, StandardCharsets.UTF_8);
    }
    
    /**
     * Create a message from a string with specified content type and charset.
     */
    public Message createFromString(String content, ContentType contentType, Charset charset) {
        if (content == null) {
            content = "";
        }
        
        byte[] bytes = content.getBytes(charset);
        MessageMetadata metadata = metadataFactory.createDefault()
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedAt(Instant.now())
            .build();
        
        // Only add charset if it's not UTF-8 (default)
        ContentType finalContentType = contentType;
        if (charset != null && !charset.equals(StandardCharsets.UTF_8)) {
            finalContentType = contentType.withCharset(charset.name());
        }
        
        return new DefaultMessage(
            finalContentType,
            new ByteArrayInputStream(bytes),
            metadata,
            bytes.length,
            true, // lazy loading
            8192  // buffer size
        );
    }
    
    /**
     * Create a message from a byte array with auto-detected content type.
     */
    public Message createFromBytes(byte[] bytes) {
        ContentType detectedType = contentTypeDetector.detectFromBytes(bytes);
        return createFromBytes(bytes, detectedType);
    }
    
    /**
     * Create a message from a byte array with specified content type.
     */
    public Message createFromBytes(byte[] bytes, ContentType contentType) {
        if (bytes == null) {
            bytes = new byte[0];
        }
        
        MessageMetadata metadata = metadataFactory.createDefault()
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedAt(Instant.now())
            .build();
        
        return new DefaultMessage(
            contentType,
            new ByteArrayInputStream(bytes),
            metadata,
            bytes.length,
            true,
            8192
        );
    }
    
    /**
     * Create a message from an InputStream with specified content type.
     */
    public Message createFromStream(InputStream stream, ContentType contentType) {
        return createFromStream(stream, contentType, -1);
    }
    
    /**
     * Create a message from an InputStream with specified content type and length.
     */
    public Message createFromStream(InputStream stream, ContentType contentType, long contentLength) {
        MessageMetadata metadata = metadataFactory.createDefault()
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedAt(Instant.now())
            .build();
        
        return new DefaultMessage(
            contentType,
            stream,
            metadata,
            contentLength,
            true,
            8192
        );
    }
    
    /**
     * Create a message from a file with auto-detected content type.
     */
    public Message createFromFile(Path filePath) throws MessageException {
        try {
            // Detect content type from file extension and content
            ContentType contentType = contentTypeDetector.detectFromFile(filePath);
            
            long fileSize = Files.size(filePath);
            InputStream fileStream = new FileInputStream(filePath.toFile());
            
            MessageMetadata metadata = metadataFactory.createDefault()
                .withMessageId(UUID.randomUUID().toString())
                .withCreatedAt(Instant.now())
                .withSource("file://" + filePath.toAbsolutePath())
                .withProperty("file.path", filePath.toString())
                .withProperty("file.name", filePath.getFileName().toString())
                .withProperty("file.size", fileSize)
                .build();
            
            return new DefaultMessage(
                contentType,
                fileStream,
                metadata,
                fileSize,
                true,
                8192
            );
        } catch (IOException e) {
            throw new MessageException(MessageException.ErrorCode.IO_ERROR, 
                "Failed to create message from file: " + filePath, e);
        }
    }
    
    /**
     * Create an empty message with specified content type.
     */
    public Message createEmpty(ContentType contentType) {
        MessageMetadata metadata = metadataFactory.createDefault()
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedAt(Instant.now())
            .build();
        
        return new DefaultMessage(
            contentType,
            new ByteArrayInputStream(new byte[0]),
            metadata,
            0,
            true,
            8192
        );
    }
    
    /**
     * Create a message from HTTP request data.
     */
    public Message createFromHttpRequest(InputStream requestBody, String contentTypeHeader, 
                                       java.util.Map<String, String> headers) {
        ContentType contentType = contentTypeHeader != null ? 
            ContentType.parse(contentTypeHeader) : 
            ContentType.APPLICATION_OCTET_STREAM;
        
        MessageMetadata metadata = metadataFactory.createDefault()
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedAt(Instant.now())
            .withHeaders(headers)
            .withSource("http")
            .build();
        
        return new DefaultMessage(
            contentType,
            requestBody,
            metadata,
            -1, // Unknown content length
            true,
            8192
        );
    }
    
    /**
     * Create a message builder for custom message construction.
     */
    public MessageBuilder builder() {
        return new DefaultMessageBuilder()
            .withMessageId(UUID.randomUUID().toString())
            .withCreatedAt(Instant.now());
    }
    
    /**
     * Content type detection utility.
     */
    private static class ContentTypeDetector {
        
        public ContentType detectFromContent(String content) {
            if (content == null || content.trim().isEmpty()) {
                return ContentType.TEXT_PLAIN;
            }
            
            String trimmed = content.trim();
            
            // JSON detection
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                return ContentType.APPLICATION_JSON;
            }
            
            // XML detection
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                return ContentType.APPLICATION_XML;
            }
            
            // YAML detection (basic)
            if (trimmed.contains(":") && (trimmed.contains("\n") || trimmed.contains("- "))) {
                return ContentType.APPLICATION_YAML;
            }
            
            // Default to plain text
            return ContentType.TEXT_PLAIN;
        }
        
        public ContentType detectFromBytes(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return ContentType.APPLICATION_OCTET_STREAM;
            }
            
            // Try to detect as text first
            try {
                String content = new String(bytes, StandardCharsets.UTF_8);
                if (isPrintableText(content)) {
                    return detectFromContent(content);
                }
            } catch (Exception e) {
                // Not valid UTF-8, treat as binary
            }
            
            // Binary content detection
            if (bytes.length >= 4) {
                // PDF
                if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
                    return ContentType.APPLICATION_PDF;
                }
                
                // JPEG
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
                    return ContentType.IMAGE_JPEG;
                }
                
                // PNG
                if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                    return ContentType.IMAGE_PNG;
                }
            }
            
            return ContentType.APPLICATION_OCTET_STREAM;
        }
        
        public ContentType detectFromFile(Path filePath) {
            String fileName = filePath.getFileName().toString().toLowerCase();
            
            // Extension-based detection
            if (fileName.endsWith(".json")) {
                return ContentType.APPLICATION_JSON;
            } else if (fileName.endsWith(".xml")) {
                return ContentType.APPLICATION_XML;
            } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                return ContentType.APPLICATION_YAML;
            } else if (fileName.endsWith(".txt")) {
                return ContentType.TEXT_PLAIN;
            } else if (fileName.endsWith(".csv")) {
                return ContentType.TEXT_CSV;
            } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                return ContentType.TEXT_HTML;
            } else if (fileName.endsWith(".pdf")) {
                return ContentType.APPLICATION_PDF;
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return ContentType.IMAGE_JPEG;
            } else if (fileName.endsWith(".png")) {
                return ContentType.IMAGE_PNG;
            }
            
            // Try content-based detection for small files
            try {
                if (Files.size(filePath) < 1024 * 1024) { // 1MB limit for content detection
                    byte[] bytes = Files.readAllBytes(filePath);
                    return detectFromBytes(bytes);
                }
            } catch (IOException e) {
                // Fall through to default
            }
            
            return ContentType.APPLICATION_OCTET_STREAM;
        }
        
        private boolean isPrintableText(String content) {
            if (content.length() > 1000) {
                content = content.substring(0, 1000); // Sample first 1000 chars
            }
            
            int printableChars = 0;
            int totalChars = content.length();
            
            for (char c : content.toCharArray()) {
                if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || 
                    "!@#$%^&*()_+-=[]{}|;':\",./<>?`~".indexOf(c) >= 0) {
                    printableChars++;
                }
            }
            
            return totalChars > 0 && (printableChars * 100 / totalChars) > 95;
        }
    }
}
