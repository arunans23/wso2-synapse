package org.wso2.graalvm.core.message;

import java.util.Map;
import java.util.Optional;

/**
 * Contains schema information for structured content validation.
 */
public interface SchemaInfo {
    
    /**
     * Get the schema type (e.g., "json-schema", "xsd", "avro").
     * @return schema type
     */
    String getSchemaType();
    
    /**
     * Get the schema version if available.
     * @return schema version
     */
    Optional<String> getSchemaVersion();
    
    /**
     * Get the schema URI or location.
     * @return schema URI
     */
    Optional<String> getSchemaUri();
    
    /**
     * Get the raw schema content.
     * @return schema content as string
     */
    Optional<String> getSchemaContent();
    
    /**
     * Get schema metadata properties.
     * @return schema metadata
     */
    Map<String, Object> getMetadata();
    
    /**
     * Validate content against this schema.
     * @param content the content to validate
     * @return validation result
     */
    ValidationResult validate(Object content);
}
