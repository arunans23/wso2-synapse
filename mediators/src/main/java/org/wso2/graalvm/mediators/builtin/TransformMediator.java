package org.wso2.graalvm.mediators.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform mediator for transforming message content using JSON transformations.
 * This provides a simplified transformation capability compared to traditional XSLT.
 */
public class TransformMediator implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformMediator.class);
    
    private final String name;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, String> fieldMappings = new HashMap<>();
    private String transformationType = "JSON_TO_JSON";
    private String template;
    
    public TransformMediator(String name) {
        this.name = name;
    }
    
    public TransformMediator addFieldMapping(String sourcePath, String targetPath) {
        fieldMappings.put(sourcePath, targetPath);
        return this;
    }
    
    public TransformMediator setTransformationType(String type) {
        this.transformationType = type;
        return this;
    }
    
    public TransformMediator setTemplate(String template) {
        this.template = template;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        logger.debug("Transform mediator {} processing message", name);
        
        try {
            Object payload = context.getPayload();
            
            if (payload == null) {
                logger.warn("No payload to transform in mediator {}", name);
                return true;
            }
            
            Object transformedPayload = switch (transformationType) {
                case "JSON_TO_JSON" -> transformJsonToJson(payload);
                case "TEMPLATE" -> transformUsingTemplate(payload, context);
                case "FIELD_MAPPING" -> transformUsingFieldMapping(payload);
                default -> {
                    logger.warn("Unknown transformation type: {}", transformationType);
                    yield payload;
                }
            };
            
            context.setPayload(transformedPayload);
            logger.debug("Message transformed successfully using type: {}", transformationType);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error in transform mediator {}", name, e);
            context.setFault(e);
            return false;
        }
    }
    
    private Object transformJsonToJson(Object payload) throws Exception {
        if (payload instanceof String payloadStr) {
            JsonNode jsonNode = objectMapper.readTree(payloadStr);
            return transformJsonNode(jsonNode);
        } else if (payload instanceof JsonNode jsonNode) {
            return transformJsonNode(jsonNode);
        } else {
            // Try to convert to JSON
            String jsonStr = objectMapper.writeValueAsString(payload);
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            return transformJsonNode(jsonNode);
        }
    }
    
    private JsonNode transformJsonNode(JsonNode sourceNode) {
        if (fieldMappings.isEmpty()) {
            return sourceNode;
        }
        
        ObjectNode targetNode = objectMapper.createObjectNode();
        
        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String sourcePath = mapping.getKey();
            String targetPath = mapping.getValue();
            
            JsonNode sourceValue = sourceNode.at("/" + sourcePath.replace(".", "/"));
            if (!sourceValue.isMissingNode()) {
                setNestedValue(targetNode, targetPath, sourceValue);
            }
        }
        
        return targetNode;
    }
    
    private void setNestedValue(ObjectNode targetNode, String path, JsonNode value) {
        String[] pathParts = path.split("\\.");
        ObjectNode currentNode = targetNode;
        
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            if (!currentNode.has(part)) {
                currentNode.set(part, objectMapper.createObjectNode());
            }
            currentNode = (ObjectNode) currentNode.get(part);
        }
        
        currentNode.set(pathParts[pathParts.length - 1], value);
    }
    
    private Object transformUsingTemplate(Object payload, IntegrationContext context) {
        if (template == null) {
            return payload;
        }
        
        String result = template;
        
        // Simple template variable replacement
        // Replace ${payload} with the actual payload
        if (result.contains("${payload}")) {
            try {
                String payloadStr = payload instanceof String ? 
                    (String) payload : objectMapper.writeValueAsString(payload);
                result = result.replace("${payload}", payloadStr);
            } catch (Exception e) {
                logger.warn("Failed to serialize payload for template", e);
            }
        }
        
        // Replace property variables like ${propertyName}
        for (Map.Entry<String, Object> property : context.getProperties().entrySet()) {
            String placeholder = "${" + property.getKey() + "}";
            if (result.contains(placeholder)) {
                String value = property.getValue() != null ? property.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
    
    private Object transformUsingFieldMapping(Object payload) throws Exception {
        return transformJsonToJson(payload);
    }
    
    @Override
    public String toString() {
        return String.format("TransformMediator{name='%s', type='%s', mappings=%d}", 
                           name, transformationType, fieldMappings.size());
    }
}
