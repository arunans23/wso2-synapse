package org.wso2.graalvm.core.message;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default implementation of StreamingContent providing memory-efficient streaming access.
 */
public class DefaultStreamingContent implements StreamingContent {
    
    private final Message message;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final XMLInputFactory XML_FACTORY = XMLInputFactory.newInstance();
    
    public DefaultStreamingContent(Message message) {
        this.message = message;
    }
    
    @Override
    public Stream<String> lines() throws MessageException {
        try {
            InputStream stream = message.getPayloadStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, message.getCharset()));
            return reader.lines().onClose(() -> {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Log but don't throw
                }
            });
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create line stream", e);
        }
    }
    
    @Override
    public Stream<JsonNode> jsonArrayElements() throws MessageException {
        if (!message.getContentType().isJson()) {
            throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                "Message content type is not JSON: " + message.getContentType());
        }
        
        try {
            InputStream stream = message.getPayloadStream();
            JsonParser parser = JSON_MAPPER.getFactory().createParser(stream);
            
            // Verify it's an array
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                    "JSON content is not an array");
            }
            
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new JsonArrayIterator(parser), 0),
                false
            ).onClose(() -> {
                try {
                    parser.close();
                } catch (IOException e) {
                    // Log but don't throw
                }
            });
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create JSON array stream", e);
        }
    }
    
    @Override
    public Stream<String> xmlElements(String elementName) throws MessageException {
        if (!message.getContentType().isXml()) {
            throw new MessageException(MessageException.ErrorCode.CONTENT_TYPE_MISMATCH,
                "Message content type is not XML: " + message.getContentType());
        }
        
        try {
            InputStream stream = message.getPayloadStream();
            XMLStreamReader reader = XML_FACTORY.createXMLStreamReader(stream);
            
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new XmlElementIterator(reader, elementName), 0),
                false
            ).onClose(() -> {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Log but don't throw
                }
            });
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create XML element stream", e);
        }
    }
    
    @Override
    public Stream<Map<String, String>> csvRows(boolean hasHeader) throws MessageException {
        try {
            Stream<String> lines = lines();
            Iterator<String> lineIterator = lines.iterator();
            
            String[] headers = null;
            if (hasHeader && lineIterator.hasNext()) {
                headers = parseCsvLine(lineIterator.next());
            }
            
            final String[] finalHeaders = headers;
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new CsvRowIterator(lineIterator, finalHeaders), 0),
                false
            );
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create CSV row stream", e);
        }
    }
    
    @Override
    public Stream<ByteBuffer> bytes(int chunkSize) throws MessageException {
        try {
            InputStream stream = message.getPayloadStream();
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new ByteChunkIterator(stream, chunkSize), 0),
                false
            ).onClose(() -> {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Log but don't throw
                }
            });
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create byte stream", e);
        }
    }
    
    public <T> Iterator<T> stream(ElementParser<T> parser) throws MessageException {
        try {
            Stream<String> lines = lines();
            return new CustomElementIterator<>(lines.iterator(), parser);
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create custom stream", e);
        }
    }
    
    @Override
    public <T> Iterator<T> iterator(Class<T> elementType, ElementParser<T> parser) throws MessageException {
        try {
            Stream<String> lineStream = lines();
            return lineStream.map(line -> {
                try {
                    return parser.parse(line);
                } catch (MessageException e) {
                    throw new RuntimeException(e);
                }
            }).iterator();
        } catch (Exception e) {
            throw new MessageException(MessageException.ErrorCode.STREAMING_ERROR,
                "Failed to create iterator for element type: " + elementType.getSimpleName(), e);
        }
    }

    @Override
    public boolean isStreamingSupported() {
        // Most content types support streaming
        return true;
    }

    @Override
    public long getEstimatedElementCount() {
        // For simplicity, return -1 to indicate unknown
        // In a real implementation, this could analyze content headers or sample the stream
        return -1;
    }

    // Helper classes for streaming implementations
    
    private static class JsonArrayIterator implements Iterator<JsonNode> {
        private final JsonParser parser;
        private boolean hasNext = true;
        
        public JsonArrayIterator(JsonParser parser) {
            this.parser = parser;
        }
        
        @Override
        public boolean hasNext() {
            if (!hasNext) {
                return false;
            }
            
            try {
                JsonToken token = parser.nextToken();
                if (token == JsonToken.END_ARRAY || token == null) {
                    hasNext = false;
                    return false;
                }
                return true;
            } catch (IOException e) {
                hasNext = false;
                return false;
            }
        }
        
        @Override
        public JsonNode next() {
            try {
                return JSON_MAPPER.readTree(parser);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static class XmlElementIterator implements Iterator<String> {
        private final XMLStreamReader reader;
        private final String targetElement;
        private String nextElement;
        private boolean hasNext = true;
        
        public XmlElementIterator(XMLStreamReader reader, String targetElement) {
            this.reader = reader;
            this.targetElement = targetElement;
            advance();
        }
        
        @Override
        public boolean hasNext() {
            return hasNext && nextElement != null;
        }
        
        @Override
        public String next() {
            String current = nextElement;
            advance();
            return current;
        }
        
        private void advance() {
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamReader.START_ELEMENT && 
                        targetElement.equals(reader.getLocalName())) {
                        
                        StringBuilder element = new StringBuilder();
                        element.append("<").append(reader.getLocalName());
                        
                        // Add attributes
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            element.append(" ")
                                .append(reader.getAttributeLocalName(i))
                                .append("=\"")
                                .append(reader.getAttributeValue(i))
                                .append("\"");
                        }
                        element.append(">");
                        
                        // Read element content
                        int depth = 1;
                        while (depth > 0 && reader.hasNext()) {
                            event = reader.next();
                            if (event == XMLStreamReader.START_ELEMENT) {
                                depth++;
                                element.append("<").append(reader.getLocalName()).append(">");
                            } else if (event == XMLStreamReader.END_ELEMENT) {
                                depth--;
                                if (depth > 0) {
                                    element.append("</").append(reader.getLocalName()).append(">");
                                }
                            } else if (event == XMLStreamReader.CHARACTERS) {
                                element.append(reader.getText());
                            }
                        }
                        element.append("</").append(targetElement).append(">");
                        
                        nextElement = element.toString();
                        return;
                    }
                }
                hasNext = false;
                nextElement = null;
            } catch (Exception e) {
                hasNext = false;
                nextElement = null;
            }
        }
    }
    
    private static class CsvRowIterator implements Iterator<Map<String, String>> {
        private final Iterator<String> lineIterator;
        private final String[] headers;
        
        public CsvRowIterator(Iterator<String> lineIterator, String[] headers) {
            this.lineIterator = lineIterator;
            this.headers = headers;
        }
        
        @Override
        public boolean hasNext() {
            return lineIterator.hasNext();
        }
        
        @Override
        public Map<String, String> next() {
            String line = lineIterator.next();
            String[] values = parseCsvLine(line);
            
            Map<String, String> row = new ConcurrentHashMap<>();
            if (headers != null) {
                for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                    row.put(headers[i], values[i]);
                }
            } else {
                for (int i = 0; i < values.length; i++) {
                    row.put("column" + i, values[i]);
                }
            }
            return row;
        }
    }
    
    private static class ByteChunkIterator implements Iterator<ByteBuffer> {
        private final InputStream stream;
        private final int chunkSize;
        private boolean hasNext = true;
        
        public ByteChunkIterator(InputStream stream, int chunkSize) {
            this.stream = stream;
            this.chunkSize = chunkSize;
        }
        
        @Override
        public boolean hasNext() {
            return hasNext;
        }
        
        @Override
        public ByteBuffer next() {
            try {
                byte[] buffer = new byte[chunkSize];
                int bytesRead = stream.read(buffer);
                
                if (bytesRead == -1) {
                    hasNext = false;
                    return ByteBuffer.allocate(0);
                } else if (bytesRead < chunkSize) {
                    hasNext = false;
                    return ByteBuffer.wrap(buffer, 0, bytesRead);
                } else {
                    return ByteBuffer.wrap(buffer);
                }
            } catch (IOException e) {
                hasNext = false;
                throw new RuntimeException(e);
            }
        }
    }
    
    private static class CustomElementIterator<T> implements Iterator<T> {
        private final Iterator<String> lineIterator;
        private final ElementParser<T> parser;
        
        public CustomElementIterator(Iterator<String> lineIterator, ElementParser<T> parser) {
            this.lineIterator = lineIterator;
            this.parser = parser;
        }
        
        @Override
        public boolean hasNext() {
            return lineIterator.hasNext();
        }
        
        @Override
        public T next() {
            try {
                String line = lineIterator.next();
                return parser.parse(line);
            } catch (MessageException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    // Utility method for CSV parsing
    private static String[] parseCsvLine(String line) {
        // Simple CSV parser - in production, use a proper CSV library
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}
