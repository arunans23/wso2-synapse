package org.wso2.graalvm.core.message;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.stream.Stream;
import java.nio.ByteBuffer;

/**
 * Provides streaming access to message content for memory-efficient processing
 * of large payloads without loading the entire content into memory.
 */
public interface StreamingContent {
    
    /**
     * Stream the content line by line.
     * Useful for large text files, CSV data, log files.
     * @return stream of text lines
     * @throws MessageException if streaming fails
     */
    Stream<String> lines() throws MessageException;
    
    /**
     * Stream JSON array elements one by one.
     * Useful for processing large JSON arrays without loading everything into memory.
     * @return stream of JSON nodes
     * @throws MessageException if content is not a JSON array or streaming fails
     */
    Stream<JsonNode> jsonArrayElements() throws MessageException;
    
    /**
     * Stream XML elements that match a specific tag name.
     * Useful for processing large XML documents with repeated elements.
     * @param elementName the XML element name to stream
     * @return stream of XML elements as strings
     * @throws MessageException if streaming fails
     */
    Stream<String> xmlElements(String elementName) throws MessageException;
    
    /**
     * Stream CSV rows as maps with column headers.
     * @param hasHeader whether the first row contains headers
     * @return stream of row maps
     * @throws MessageException if streaming fails
     */
    Stream<java.util.Map<String, String>> csvRows(boolean hasHeader) throws MessageException;
    
    /**
     * Stream raw bytes in chunks.
     * Useful for binary content processing.
     * @param chunkSize the size of each chunk in bytes
     * @return stream of byte buffers
     * @throws MessageException if streaming fails
     */
    Stream<ByteBuffer> bytes(int chunkSize) throws MessageException;
    
    /**
     * Create an iterator for custom streaming patterns.
     * @param <T> the type of elements to iterate
     * @param elementType the class of elements
     * @param parser custom parser function
     * @return iterator of parsed elements
     * @throws MessageException if streaming setup fails
     */
    <T> Iterator<T> iterator(Class<T> elementType, ElementParser<T> parser) throws MessageException;
    
    /**
     * Get the estimated number of elements (if available).
     * @return estimated element count, or -1 if unknown
     */
    long getEstimatedElementCount();
    
    /**
     * Check if the content supports streaming for the current content type.
     * @return true if streaming is supported
     */
    boolean isStreamingSupported();
    
    /**
     * Interface for custom element parsing during streaming.
     */
    @FunctionalInterface
    interface ElementParser<T> {
        T parse(String rawElement) throws MessageException;
    }
}
