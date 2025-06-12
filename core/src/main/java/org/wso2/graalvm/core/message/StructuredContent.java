package org.wso2.graalvm.core.message;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides structured access to message content with lazy evaluation.
 * Content is parsed and built into object models only when accessed.
 */
public interface StructuredContent {
    
    /**
     * Get the content as a JSON object (lazy evaluation).
     * @return the JSON representation
     * @throws MessageException if content cannot be parsed as JSON
     */
    JsonNode asJson() throws MessageException;
    
    /**
     * Get the content as an XML Document (lazy evaluation).
     * @return the XML Document
     * @throws MessageException if content cannot be parsed as XML
     */
    Document asXml() throws MessageException;
    
    /**
     * Get the content as a Map for simple key-value access.
     * Works for JSON objects, XML with simple structure, and form data.
     * @return the content as a map
     * @throws MessageException if content cannot be converted to map
     */
    Map<String, Object> asMap() throws MessageException;
    
    /**
     * Get the content as a List for array-like data.
     * Works for JSON arrays, XML with repeated elements, CSV rows.
     * @return the content as a list
     * @throws MessageException if content cannot be converted to list
     */
    List<Object> asList() throws MessageException;
    
    /**
     * Get a specific field value using a path expression.
     * Supports JSONPath for JSON content and XPath for XML content.
     * @param path the path expression (JSONPath or XPath)
     * @return the field value if found
     * @throws MessageException if path is invalid or evaluation fails
     */
    Optional<Object> getField(String path) throws MessageException;
    
    /**
     * Get multiple field values using a path expression.
     * @param path the path expression
     * @return list of matching values
     * @throws MessageException if path is invalid or evaluation fails
     */
    List<Object> getFields(String path) throws MessageException;
    
    /**
     * Check if a field exists at the specified path.
     * @param path the path expression
     * @return true if the field exists
     */
    boolean hasField(String path);
    
    /**
     * Get the root element name for structured content.
     * For JSON: returns "root" or the top-level key
     * For XML: returns the root element name
     * @return the root element name
     */
    Optional<String> getRootElement();
    
    /**
     * Get schema information if available.
     * @return schema information
     */
    Optional<SchemaInfo> getSchemaInfo();
    
    /**
     * Convert content to a specific type.
     * @param type the target type
     * @return the converted content
     * @throws MessageException if conversion fails
     */
    <T> T as(Class<T> type) throws MessageException;
    
    /**
     * Get a value at the specified path.
     * @param path the path to the value
     * @return the value if found
     * @throws MessageException if path evaluation fails
     */
    Optional<Object> get(String path) throws MessageException;
    
    /**
     * Get a typed value at the specified path.
     * @param path the path to the value
     * @param type the expected type
     * @return the typed value if found and of correct type
     * @throws MessageException if path evaluation fails
     */
    <T> Optional<T> get(String path, Class<T> type) throws MessageException;
    
    /**
     * Check if a value exists at the specified path.
     * @param path the path to check
     * @return true if the value exists
     * @throws MessageException if path evaluation fails
     */
    boolean has(String path) throws MessageException;
    
    /**
     * Check if the content represents an array.
     * @return true if content is an array
     * @throws MessageException if content type cannot be determined
     */
    boolean isArray() throws MessageException;
    
    /**
     * Check if the content represents an object.
     * @return true if content is an object
     * @throws MessageException if content type cannot be determined
     */
    boolean isObject() throws MessageException;
    
    /**
     * Get the size of the content.
     * @return number of elements or fields
     * @throws MessageException if size cannot be determined
     */
    int size() throws MessageException;
    
    /**
     * Check if the content is empty.
     * @return true if content is empty
     * @throws MessageException if emptiness cannot be determined
     */
    boolean isEmpty() throws MessageException;
}
