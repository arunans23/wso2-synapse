package org.wso2.graalvm.mediators.examples;

import org.wso2.graalvm.core.annotation.MediatorComponent;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.core.message.*;

import java.util.concurrent.CompletableFuture;

/**
 * Example mediator demonstrating the new Message interface capabilities.
 * Shows streaming, lazy evaluation, content transformation, and validation.
 */
@MediatorComponent
public class MessageProcessingExampleMediator {
    
    private final MessageFactory messageFactory;
    
    public MessageProcessingExampleMediator() {
        this.messageFactory = new MessageFactory();
    }
    
    /**
     * Process incoming message with streaming and lazy evaluation.
     */
    public boolean mediate(IntegrationContext context) {
        try {
            Message incomingMessage = context.getMessage();
            
            // Example 1: Basic message inspection
            logMessageInfo(incomingMessage);
            
            // Example 2: Streaming large JSON arrays
            if (incomingMessage.getContentType().isJson()) {
                processJsonStream(incomingMessage);
            }
            
            // Example 3: XML processing with streaming
            else if (incomingMessage.getContentType().isXml()) {
                processXmlStream(incomingMessage);
            }
            
            // Example 4: Content transformation
            Message transformedMessage = transformMessage(incomingMessage);
            
            // Example 5: Message validation
            ValidationResult validation = validateMessage(transformedMessage);
            if (!validation.isValid()) {
                context.setFault(new Exception("Message validation failed: " + validation.getErrors()));
                return false;
            }
            
            // Example 6: Create response message
            Message responseMessage = createResponseMessage(transformedMessage);
            context.setMessage(responseMessage);
            
            return true;
            
        } catch (Exception e) {
            context.setFault(e);
            return false;
        }
    }
    
    private void logMessageInfo(Message message) throws MessageException {
        System.out.println("=== Message Information ===");
        System.out.println("Content Type: " + message.getContentType());
        System.out.println("Charset: " + message.getCharset());
        System.out.println("Content Length: " + message.getContentLength());
        System.out.println("Is Consumed: " + message.isConsumed());
        System.out.println("Is Empty: " + message.isEmpty());
        System.out.println("Is Valid: " + message.isValid());
        
        MessageMetadata metadata = message.getMetadata();
        System.out.println("Message ID: " + metadata.getMessageId());
        System.out.println("Correlation ID: " + metadata.getCorrelationId());
        System.out.println("Source: " + metadata.getSource());
        System.out.println("Headers: " + metadata.getHeaders());
    }
    
    private void processJsonStream(Message message) throws MessageException {
        System.out.println("=== Processing JSON Stream ===");
        
        StructuredContent structured = message.getStructuredContent();
        
        // Check if it's a JSON array for streaming
        if (structured.isArray()) {
            StreamingContent streaming = message.getStreamingContent();
            
            // Stream JSON array elements one by one (memory-efficient)
            streaming.jsonArrayElements().forEach(jsonNode -> {
                System.out.println("Processing JSON element: " + jsonNode.toString());
                // Process each element without loading entire array into memory
            });
        } else {
            // Process as single JSON object
            System.out.println("Processing single JSON object");
            var jsonData = structured.asMap();
            jsonData.forEach((key, value) -> 
                System.out.println("  " + key + ": " + value));
        }
    }
    
    private void processXmlStream(Message message) throws MessageException {
        System.out.println("=== Processing XML Stream ===");
        
        StreamingContent streaming = message.getStreamingContent();
        
        // Stream specific XML elements (e.g., "record" elements)
        streaming.xmlElements("record").forEach(element -> {
            System.out.println("Processing XML record: " + element);
            // Process each record element without loading entire document
        });
    }
    
    private Message transformMessage(Message originalMessage) throws MessageException {
        System.out.println("=== Message Transformation ===");
        
        ContentType originalType = originalMessage.getContentType();
        
        // Transform JSON to XML for demonstration
        if (originalType.isJson()) {
            ContentType targetType = ContentType.APPLICATION_XML;
            
            // Asynchronous transformation
            CompletableFuture<Message> transformationFuture = 
                originalMessage.transformTo(targetType);
            
            try {
                Message transformed = transformationFuture.get();
                System.out.println("Successfully transformed from " + originalType + " to " + targetType);
                return transformed;
            } catch (Exception e) {
                System.err.println("Transformation failed: " + e.getMessage());
                return originalMessage;
            }
        }
        
        return originalMessage;
    }
    
    private ValidationResult validateMessage(Message message) throws MessageException {
        System.out.println("=== Message Validation ===");
        
        // Basic validation based on content type
        if (message.getContentType().isJson()) {
            // Try to parse JSON to validate structure
            try {
                message.getStructuredContent().asJson();
                System.out.println("JSON validation: PASSED");
                return ValidationResult.valid();
            } catch (MessageException e) {
                System.out.println("JSON validation: FAILED - " + e.getMessage());
                return ValidationResult.invalid("Invalid JSON structure: " + e.getMessage());
            }
        } else if (message.getContentType().isXml()) {
            // Try to parse XML to validate structure
            try {
                message.getStructuredContent().asXml();
                System.out.println("XML validation: PASSED");
                return ValidationResult.valid();
            } catch (MessageException e) {
                System.out.println("XML validation: FAILED - " + e.getMessage());
                return ValidationResult.invalid("Invalid XML structure: " + e.getMessage());
            }
        }
        
        return ValidationResult.valid();
    }
    
    private Message createResponseMessage(Message originalMessage) throws MessageException {
        System.out.println("=== Creating Response Message ===");
        
        // Create a response message using the builder pattern
        MessageBuilder builder = messageFactory.builder()
            .withContentType(ContentType.APPLICATION_JSON)
            .withCorrelationId(originalMessage.getMetadata().getMessageId())
            .withSource("MessageProcessingExampleMediator")
            .withProperty("original_type", originalMessage.getContentType().toString())
            .withProperty("processed_at", System.currentTimeMillis());
        
        // Create response payload based on original message
        String responsePayload;
        if (originalMessage.getContentType().isJson()) {
            // Create JSON response
            responsePayload = String.format(
                "{\"status\":\"processed\",\"original_type\":\"%s\",\"message_id\":\"%s\",\"timestamp\":%d}",
                originalMessage.getContentType(),
                originalMessage.getMetadata().getMessageId(),
                System.currentTimeMillis()
            );
        } else {
            // Create generic response
            responsePayload = String.format(
                "{\"status\":\"processed\",\"original_type\":\"%s\",\"message\":\"Successfully processed %s message\"}",
                originalMessage.getContentType(),
                originalMessage.getContentType().getType()
            );
        }
        
        return builder.withPayload(responsePayload).build();
    }
    
    /**
     * Example of creating messages for different scenarios.
     */
    public void demonstrateMessageCreation() {
        System.out.println("=== Message Creation Examples ===");
        
        try {
            // Example 1: Create JSON message from string
            Message jsonMessage = messageFactory.createFromString(
                "{\"name\":\"John\",\"age\":30}",
                ContentType.APPLICATION_JSON
            );
            System.out.println("Created JSON message: " + jsonMessage.getPayloadText());
            
            // Example 2: Create XML message with custom headers
            Message xmlMessage = messageFactory.builder()
                .withContentType(ContentType.APPLICATION_XML)
                .withPayload("<person><name>Jane</name><age>25</age></person>")
                .withHeader("X-Custom-Header", "example-value")
                .withProperty("source", "demo")
                .build();
            System.out.println("Created XML message with headers");
            
            // Example 3: Create empty message
            Message emptyMessage = messageFactory.createEmpty(ContentType.TEXT_PLAIN);
            System.out.println("Created empty message: " + emptyMessage.isEmpty());
            
            // Example 4: Create binary message
            byte[] binaryData = "Hello World".getBytes();
            Message binaryMessage = messageFactory.builder()
                .withContentType(ContentType.APPLICATION_OCTET_STREAM)
                .withPayload(binaryData)
                .build();
            System.out.println("Created binary message with " + binaryData.length + " bytes");
            
        } catch (Exception e) {
            System.err.println("Message creation failed: " + e.getMessage());
        }
    }
    
    /**
     * Example of handling large messages with streaming.
     */
    public void demonstrateLargeMessageProcessing(Message largeMessage) {
        System.out.println("=== Large Message Processing ===");
        
        try {
            if (largeMessage.getContentType().isText()) {
                // Process large text file line by line
                StreamingContent streaming = largeMessage.getStreamingContent();
                
                streaming.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .limit(10) // Process only first 10 lines for demo
                    .forEach(line -> System.out.println("Processing line: " + line));
                    
            } else if (largeMessage.getContentType().isBinary()) {
                // Process large binary data in chunks
                StreamingContent streaming = largeMessage.getStreamingContent();
                
                streaming.bytes(1024) // 1KB chunks
                    .limit(5) // Process only first 5 chunks for demo
                    .forEach(chunk -> 
                        System.out.println("Processing chunk of " + chunk.remaining() + " bytes"));
            }
            
        } catch (Exception e) {
            System.err.println("Large message processing failed: " + e.getMessage());
        }
    }
}
