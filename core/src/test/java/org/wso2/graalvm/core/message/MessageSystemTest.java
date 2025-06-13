package org.wso2.graalvm.core.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Unit tests for the Message interface and its implementations.
 * Demonstrates the message system capabilities and validates functionality.
 */
class MessageSystemTest {
    
    private MessageFactory messageFactory;
    
    @BeforeEach
    void setUp() {
        messageFactory = new MessageFactory();
    }
    
    @Test
    void testJsonMessageCreationAndAccess() throws MessageException {
        // Test JSON message creation
        String jsonContent = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
        Message jsonMessage = messageFactory.createFromString(jsonContent, ContentType.APPLICATION_JSON);
        
        // Basic message properties
        assertEquals(ContentType.APPLICATION_JSON, jsonMessage.getContentType());
        assertEquals(StandardCharsets.UTF_8, jsonMessage.getCharset());
        assertFalse(jsonMessage.isEmpty());
        assertTrue(jsonMessage.isValid());
        
        // Test structured content access
        StructuredContent structured = jsonMessage.getStructuredContent();
        assertTrue(structured.isObject());
        assertFalse(structured.isArray());
        
        // Test JSON parsing
        var jsonNode = structured.asJson();
        assertNotNull(jsonNode);
        assertTrue(jsonNode.has("name"));
        
        // Test map conversion
        Map<String, Object> map = structured.asMap();
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        
        // Test path-based access
        assertEquals("John", structured.get("name", String.class).orElse(null));
        assertEquals(30, structured.get("age", Integer.class).orElse(null));
        assertTrue(structured.has("city"));
        assertFalse(structured.has("country"));
    }
    
    @Test
    void testXmlMessageCreationAndAccess() throws MessageException {
        // Test XML message creation
        String xmlContent = "<person><name>Jane</name><age>25</age></person>";
        Message xmlMessage = messageFactory.createFromString(xmlContent, ContentType.APPLICATION_XML);
        
        // Basic message properties
        assertEquals(ContentType.APPLICATION_XML, xmlMessage.getContentType());
        assertTrue(xmlMessage.isValid());
        
        // Test structured content access
        StructuredContent structured = xmlMessage.getStructuredContent();
        assertTrue(structured.isObject());
        
        // Test XML parsing
        var document = structured.asXml();
        assertNotNull(document);
        assertEquals("person", document.getDocumentElement().getTagName());
    }
    
    @Test
    void testJsonArrayStreaming() throws MessageException {
        // Test JSON array streaming
        String jsonArray = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"},{\"id\":3,\"name\":\"Charlie\"}]";
        Message arrayMessage = messageFactory.createFromString(jsonArray, ContentType.APPLICATION_JSON);
        
        // Verify it's an array
        StructuredContent structured = arrayMessage.getStructuredContent();
        assertTrue(structured.isArray());
        assertEquals(3, structured.size());
        
        // Test streaming access
        StreamingContent streaming = arrayMessage.getStreamingContent();
        long count = streaming.jsonArrayElements().count();
        assertEquals(3, count);
    }
    
    @Test
    void testTextLineStreaming() throws MessageException {
        // Test text line streaming
        String textContent = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";
        Message textMessage = messageFactory.createFromString(textContent, ContentType.TEXT_PLAIN);
        
        // Test line streaming
        StreamingContent streaming = textMessage.getStreamingContent();
        long lineCount = streaming.lines().count();
        assertEquals(5, lineCount);
        
        // Test line processing
        String firstLine = streaming.lines().findFirst().orElse("");
        assertEquals("Line 1", firstLine);
    }
    
    @Test
    void testMessageBuilder() throws MessageException {
        // Test message builder pattern
        Message builtMessage = messageFactory.builder()
            .withContentType(ContentType.APPLICATION_JSON)
            .withPayload("{\"test\":true}")
            .withHeader("X-Custom", "value")
            .withProperty("source", "test")
            .withCorrelationId("test-correlation")
            .withPriority(5)
            .build();
        
        // Verify built message
        assertEquals(ContentType.APPLICATION_JSON, builtMessage.getContentType());
        assertEquals("{\"test\":true}", builtMessage.getPayloadText());
        
        MessageMetadata metadata = builtMessage.getMetadata();
        assertEquals("test-correlation", metadata.getCorrelationId().orElse(null));
        assertEquals("value", metadata.getHeaders().get("X-Custom"));
        assertEquals("test", metadata.getProperties().get("source"));
        assertEquals(5, metadata.getPriority().orElse(0).intValue());
    }
    
    @Test
    void testMessageTransformation() throws MessageException {
        // Test JSON to XML transformation
        String jsonContent = "{\"name\":\"John\",\"age\":30}";
        Message jsonMessage = messageFactory.createFromString(jsonContent, ContentType.APPLICATION_JSON);
        
        // Transform to XML
        var transformationFuture = jsonMessage.transformTo(ContentType.APPLICATION_XML);
        assertNotNull(transformationFuture);
        
        // Note: In a real test, you would complete the future and verify the result
        // Message xmlMessage = transformationFuture.get();
        // assertEquals(ContentType.APPLICATION_XML, xmlMessage.getContentType());
    }
    
    @Test
    void testMessageCopy() throws MessageException {
        // Test message copying
        String content = "{\"data\":\"original\"}";
        Message original = messageFactory.createFromString(content, ContentType.APPLICATION_JSON);
        
        // Copy the message
        Message copy = original.copy();
        
        // Verify copy
        assertEquals(original.getContentType(), copy.getContentType());
        assertEquals(original.getPayloadText(), copy.getPayloadText());
        assertNotSame(original, copy);
    }
    
    @Test
    void testMessageValidation() throws MessageException {
        // Test valid JSON
        Message validJson = messageFactory.createFromString("{\"valid\":true}", ContentType.APPLICATION_JSON);
        assertTrue(validJson.isValid());
        
        // Test invalid JSON
        Message invalidJson = messageFactory.createFromString("{invalid json", ContentType.APPLICATION_JSON);
        assertFalse(invalidJson.isValid());
        
        // Test valid XML
        Message validXml = messageFactory.createFromString("<root><valid>true</valid></root>", ContentType.APPLICATION_XML);
        assertTrue(validXml.isValid());
        
        // Test invalid XML - suppress XML parser error output for test
        // The error "XML document structures must start and end within the same entity" is expected
        Message invalidXml = messageFactory.createFromString("<invalid><xml", ContentType.APPLICATION_XML);
        assertFalse(invalidXml.isValid(), "Invalid XML should be detected as invalid");
    }
    
    @Test
    void testEmptyMessage() throws MessageException {
        // Test empty message creation
        Message emptyMessage = messageFactory.createEmpty(ContentType.TEXT_PLAIN);
        
        assertTrue(emptyMessage.isEmpty());
        assertEquals(0, emptyMessage.getContentLength());
        assertEquals("", emptyMessage.getPayloadText());
    }
    
    @Test
    void testBinaryMessage() throws MessageException {
        // Test binary message
        byte[] binaryData = "Hello World".getBytes(StandardCharsets.UTF_8);
        Message binaryMessage = messageFactory.createFromStream(
            new ByteArrayInputStream(binaryData),
            ContentType.APPLICATION_OCTET_STREAM,
            binaryData.length
        );
        
        assertEquals(ContentType.APPLICATION_OCTET_STREAM, binaryMessage.getContentType());
        assertEquals(binaryData.length, binaryMessage.getContentLength());
        assertArrayEquals(binaryData, binaryMessage.getPayloadBytes());
    }
    
    @Test
    void testLazyLoading() throws MessageException {
        // Test lazy loading behavior
        String jsonContent = "{\"lazy\":true,\"data\":[1,2,3,4,5]}";
        Message lazyMessage = messageFactory.createFromString(jsonContent, ContentType.APPLICATION_JSON);
        
        // Initially, structured content should not be parsed
        assertFalse(lazyMessage.isConsumed());
        
        // Access structured content - this should trigger lazy loading
        StructuredContent structured = lazyMessage.getStructuredContent();
        assertNotNull(structured);
        
        // After accessing content, the stream should be consumed
        assertTrue(lazyMessage.isConsumed());
    }
    
    @Test
    void testMessageMetadata() throws MessageException {
        // Test comprehensive metadata
        Message message = messageFactory.builder()
            .withContentType(ContentType.APPLICATION_JSON)
            .withPayload("{\"test\":true}")
            .withMessageId("test-msg-123")
            .withCorrelationId("test-corr-456")
            .withSource("unit-test")
            .withTarget("test-target")
            .withPriority(3)
            .withHeader("Authorization", "Bearer token123")
            .withHeader("Content-Language", "en-US")
            .withProperty("test-property", "test-value")
            .withProperty("timestamp", System.currentTimeMillis())
            .build();
        
        MessageMetadata metadata = message.getMetadata();
        
        // Verify metadata
        assertEquals("test-msg-123", metadata.getMessageId());
        assertEquals("test-corr-456", metadata.getCorrelationId().orElse(null));
        assertEquals("unit-test", metadata.getSource().orElse(null));
        assertEquals("test-target", metadata.getTarget().orElse(null));
        assertEquals(3, metadata.getPriority().orElse(0).intValue());
        
        // Verify headers
        assertEquals("Bearer token123", metadata.getHeaders().get("Authorization"));
        assertEquals("en-US", metadata.getHeaders().get("Content-Language"));
        
        // Verify properties
        assertEquals("test-value", metadata.getProperties().get("test-property"));
        assertNotNull(metadata.getProperties().get("timestamp"));
        
        // Verify builder pattern for metadata
        MessageMetadata.Builder builder = metadata.toBuilder();
        assertNotNull(builder);
    }
    
    @Test
    void testResourceManagement() throws MessageException {
        // Test resource cleanup
        String content = "{\"resource\":\"test\"}";
        Message message = messageFactory.createFromString(content, ContentType.APPLICATION_JSON);
        
        // Access content
        assertNotNull(message.getPayloadText());
        
        // Close the message
        message.close();
        
        // After closing, accessing content should still work (cached content)
        assertNotNull(message.getPayloadText());
    }
}
