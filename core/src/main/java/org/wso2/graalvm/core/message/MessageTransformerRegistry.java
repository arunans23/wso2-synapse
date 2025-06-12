package org.wso2.graalvm.core.message;

import org.wso2.graalvm.core.annotation.MediatorComponent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for message transformers providing transformation capabilities between content types.
 */
@MediatorComponent
public class MessageTransformerRegistry {
    
    private static final Map<String, List<MessageTransformer>> transformers = new ConcurrentHashMap<>();
    private static volatile MessageTransformerRegistry instance;
    
    // Built-in transformers
    private static final MessageTransformer JSON_TO_XML_TRANSFORMER = new JsonToXmlTransformer();
    private static final MessageTransformer XML_TO_JSON_TRANSFORMER = new XmlToJsonTransformer();
    private static final MessageTransformer TEXT_TO_JSON_TRANSFORMER = new TextToJsonTransformer();
    private static final MessageTransformer BINARY_TO_TEXT_TRANSFORMER = new BinaryToTextTransformer();
    
    static {
        // Register built-in transformers
        registerTransformer(JSON_TO_XML_TRANSFORMER);
        registerTransformer(XML_TO_JSON_TRANSFORMER);
        registerTransformer(TEXT_TO_JSON_TRANSFORMER);
        registerTransformer(BINARY_TO_TEXT_TRANSFORMER);
    }
    
    public static MessageTransformerRegistry getInstance() {
        if (instance == null) {
            synchronized (MessageTransformerRegistry.class) {
                if (instance == null) {
                    instance = new MessageTransformerRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a transformer.
     * @param transformer the transformer to register
     */
    public static void registerTransformer(MessageTransformer transformer) {
        // Create a key for all supported transformations
        List<MessageTransformer> transformerList = transformers.computeIfAbsent(
            "all", k -> new CopyOnWriteArrayList<>());
        transformerList.add(transformer);
        transformerList.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    /**
     * Get a transformer for the specified content types.
     * @param sourceType the source content type
     * @param targetType the target content type
     * @return the transformer
     * @throws MessageException if no transformer is found
     */
    public static MessageTransformer getTransformer(ContentType sourceType, ContentType targetType) 
            throws MessageException {
        List<MessageTransformer> candidates = transformers.get("all");
        if (candidates != null) {
            for (MessageTransformer transformer : candidates) {
                if (transformer.canTransform(sourceType, targetType)) {
                    return transformer;
                }
            }
        }
        
        throw new MessageException(MessageException.ErrorCode.TRANSFORMATION_ERROR,
            "No transformer found for " + sourceType + " to " + targetType);
    }
    
    // Built-in transformer implementations
    
    private static class JsonToXmlTransformer implements MessageTransformer {
        @Override
        public boolean canTransform(ContentType sourceType, ContentType targetType) {
            return sourceType.isJson() && targetType.isXml();
        }
        
        @Override
        public Message transform(Message message, ContentType targetType) throws MessageException {
            try {
                // Simple JSON to XML transformation
                String jsonContent = message.getPayloadText();
                String xmlContent = convertJsonToXml(jsonContent);
                
                return new MessageFactory().createFromString(xmlContent, targetType);
            } catch (Exception e) {
                throw new MessageException(MessageException.ErrorCode.TRANSFORMATION_ERROR,
                    "Failed to transform JSON to XML", e);
            }
        }
        
        @Override
        public int getPriority() {
            return 10;
        }
        
        private String convertJsonToXml(String json) {
            // Simplified JSON to XML conversion
            // In production, use a proper library like Jackson or JSON-XML
            if (json.trim().startsWith("{")) {
                return "<root>" + json.replace("{", "<object>").replace("}", "</object>")
                    .replace("[", "<array>").replace("]", "</array>") + "</root>";
            }
            return "<root><value>" + json + "</value></root>";
        }
    }
    
    private static class XmlToJsonTransformer implements MessageTransformer {
        @Override
        public boolean canTransform(ContentType sourceType, ContentType targetType) {
            return sourceType.isXml() && targetType.isJson();
        }
        
        @Override
        public Message transform(Message message, ContentType targetType) throws MessageException {
            try {
                // Simple XML to JSON transformation
                String xmlContent = message.getPayloadText();
                String jsonContent = convertXmlToJson(xmlContent);
                
                return new MessageFactory().createFromString(jsonContent, targetType);
            } catch (Exception e) {
                throw new MessageException(MessageException.ErrorCode.TRANSFORMATION_ERROR,
                    "Failed to transform XML to JSON", e);
            }
        }
        
        @Override
        public int getPriority() {
            return 10;
        }
        
        private String convertXmlToJson(String xml) {
            // Simplified XML to JSON conversion
            // In production, use a proper library
            return "{\"xml_content\":\"" + xml.replace("\"", "\\\"") + "\"}";
        }
    }
    
    private static class TextToJsonTransformer implements MessageTransformer {
        @Override
        public boolean canTransform(ContentType sourceType, ContentType targetType) {
            return sourceType.isText() && targetType.isJson();
        }
        
        @Override
        public Message transform(Message message, ContentType targetType) throws MessageException {
            try {
                String textContent = message.getPayloadText();
                String jsonContent = "{\"text\":\"" + textContent.replace("\"", "\\\"") + "\"}";
                
                return new MessageFactory().createFromString(jsonContent, targetType);
            } catch (Exception e) {
                throw new MessageException(MessageException.ErrorCode.TRANSFORMATION_ERROR,
                    "Failed to transform text to JSON", e);
            }
        }
        
        @Override
        public int getPriority() {
            return 5;
        }
    }
    
    private static class BinaryToTextTransformer implements MessageTransformer {
        @Override
        public boolean canTransform(ContentType sourceType, ContentType targetType) {
            return sourceType.isBinary() && targetType.isText();
        }
        
        @Override
        public Message transform(Message message, ContentType targetType) throws MessageException {
            try {
                // Convert binary to base64 text
                byte[] binaryContent = message.getPayloadBytes();
                String base64Content = java.util.Base64.getEncoder().encodeToString(binaryContent);
                
                return new MessageFactory().createFromString(base64Content, targetType);
            } catch (Exception e) {
                throw new MessageException(MessageException.ErrorCode.TRANSFORMATION_ERROR,
                    "Failed to transform binary to text", e);
            }
        }
        
        @Override
        public int getPriority() {
            return 5;
        }
    }
}
