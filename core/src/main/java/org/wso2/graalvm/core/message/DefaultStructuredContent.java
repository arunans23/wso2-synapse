package org.wso2.graalvm.core.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of StructuredContent providing lazy access to structured data.
 */
public class DefaultStructuredContent implements StructuredContent {
    
    private final Message message;
    private volatile JsonNode jsonNode;
    private volatile Document xmlDocument;
    private volatile Map<String, Object> mapData;
    private volatile List<Object> listData;
    
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final DocumentBuilderFactory XML_FACTORY = DocumentBuilderFactory.newInstance();
    
    public DefaultStructuredContent(Message message) {
        this.message = message;
    }
    
    @Override
    public JsonNode asJson() throws MessageException {
        if (jsonNode == null) {
            synchronized (this) {
                if (jsonNode == null) {
                    if (!message.getContentType().isJson()) {
                        throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                            "Message content type is not JSON: " + message.getContentType());
                    }
                    
                    try {
                        String content = message.getPayloadText();
                        jsonNode = JSON_MAPPER.readTree(content);
                    } catch (Exception e) {
                        throw new MessageException(MessageException.ErrorCode.PARSING_ERROR,
                            "Failed to parse JSON content", e);
                    }
                }
            }
        }
        return jsonNode;
    }
    
    @Override
    public Document asXml() throws MessageException {
        if (xmlDocument == null) {
            synchronized (this) {
                if (xmlDocument == null) {
                    if (!message.getContentType().isXml()) {
                        throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                            "Message content type is not XML: " + message.getContentType());
                    }
                    
                    try {
                        String content = message.getPayloadText();
                        DocumentBuilder builder = XML_FACTORY.newDocumentBuilder();
                        xmlDocument = builder.parse(new InputSource(new StringReader(content)));
                    } catch (Exception e) {
                        throw new MessageException(MessageException.ErrorCode.PARSING_ERROR,
                            "Failed to parse XML content", e);
                    }
                }
            }
        }
        return xmlDocument;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap() throws MessageException {
        if (mapData == null) {
            synchronized (this) {
                if (mapData == null) {
                    try {
                        if (message.getContentType().isJson()) {
                            JsonNode json = asJson();
                            mapData = JSON_MAPPER.convertValue(json, Map.class);
                        } else {
                            throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                                "Cannot convert " + message.getContentType() + " to Map");
                        }
                    } catch (Exception e) {
                        throw new MessageException(MessageException.ErrorCode.PARSING_ERROR,
                            "Failed to convert content to Map", e);
                    }
                }
            }
        }
        return mapData;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Object> asList() throws MessageException {
        if (listData == null) {
            synchronized (this) {
                if (listData == null) {
                    try {
                        if (message.getContentType().isJson()) {
                            JsonNode json = asJson();
                            if (!json.isArray()) {
                                throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                                    "JSON content is not an array");
                            }
                            listData = JSON_MAPPER.convertValue(json, List.class);
                        } else {
                            throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                                "Cannot convert " + message.getContentType() + " to List");
                        }
                    } catch (Exception e) {
                        throw new MessageException(MessageException.ErrorCode.PARSING_ERROR,
                            "Failed to convert content to List", e);
                    }
                }
            }
        }
        return listData;
    }
    
    @Override
    public <T> T as(Class<T> type) throws MessageException {
        try {
            if (message.getContentType().isJson()) {
                JsonNode json = asJson();
                return JSON_MAPPER.convertValue(json, type);
            } else {
                throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                    "Cannot convert " + message.getContentType() + " to " + type.getSimpleName());
            }
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.PARSING_ERROR,
                "Failed to convert content to " + type.getSimpleName(), e);
        }
    }
    
    @Override
    public Optional<Object> get(String path) throws MessageException {
        try {
            if (message.getContentType().isJson()) {
                JsonNode json = asJson();
                JsonNode result = json.at("/" + path.replace(".", "/"));
                return result.isMissingNode() ? Optional.empty() : 
                    Optional.of(JSON_MAPPER.convertValue(result, Object.class));
            } else {
                Map<String, Object> map = asMap();
                return Optional.ofNullable(getNestedValue(map, path));
            }
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.PARSING_ERROR,
                "Failed to get value at path: " + path, e);
        }
    }
    
    @Override
    public <T> Optional<T> get(String path, Class<T> type) throws MessageException {
        Optional<Object> value = get(path);
        if (value.isPresent()) {
            try {
                if (type.isInstance(value.get())) {
                    return Optional.of(type.cast(value.get()));
                } else {
                    return Optional.of(JSON_MAPPER.convertValue(value.get(), type));
                }
            } catch (Exception e) {
                throw new MessageException(MessageException.ErrorCode.TYPE_CONVERSION_ERROR,
                    "Failed to convert value to " + type.getSimpleName(), e);
            }
        }
        return Optional.empty();
    }
    
    @Override
    public boolean has(String path) throws MessageException {
        return get(path).isPresent();
    }
    
    @Override
    public boolean isArray() throws MessageException {
        if (message.getContentType().isJson()) {
            return asJson().isArray();
        }
        return false;
    }
    
    @Override
    public boolean isObject() throws MessageException {
        if (message.getContentType().isJson()) {
            return asJson().isObject();
        }
        return message.getContentType().isXml() || message.getContentType().getType().equals("application/x-www-form-urlencoded");
    }
    
    @Override
    public int size() throws MessageException {
        if (message.getContentType().isJson()) {
            JsonNode json = asJson();
            return json.isArray() || json.isObject() ? json.size() : 1;
        } else if (message.getContentType().isXml()) {
            // For XML, return number of child elements of root
            Document doc = asXml();
            return doc.getDocumentElement().getChildNodes().getLength();
        }
        return 1;
    }
    
    @Override
    public boolean isEmpty() throws MessageException {
        return size() == 0;
    }
    
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }

    @Override
    public Optional<Object> getField(String path) throws MessageException {
        return get(path);
    }
    
    @Override
    public List<Object> getFields(String path) throws MessageException {
        // For simplicity, return single result as list
        Optional<Object> value = getField(path);
        return value.map(List::of).orElse(List.of());
    }
    
    @Override
    public boolean hasField(String path) {
        try {
            return has(path);
        } catch (MessageException e) {
            return false;
        }
    }
    
    @Override
    public Optional<String> getRootElement() {
        try {
            if (message.getContentType().isJson()) {
                JsonNode json = asJson();
                if (json.isObject() && json.fieldNames().hasNext()) {
                    return Optional.of(json.fieldNames().next());
                }
                return Optional.of("root");
            } else if (message.getContentType().isXml()) {
                Document doc = asXml();
                return Optional.of(doc.getDocumentElement().getNodeName());
            }
        } catch (MessageException e) {
            // Return empty if cannot determine
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<SchemaInfo> getSchemaInfo() {
        // For now, return empty. Schema validation can be added later
        return Optional.empty();
    }
}
