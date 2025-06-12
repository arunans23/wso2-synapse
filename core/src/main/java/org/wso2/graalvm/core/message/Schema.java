package org.wso2.graalvm.core.message;

import java.util.Optional;

/**
 * Represents a schema for message validation.
 */
public interface Schema {
    
    /**
     * Get the schema type (JSON Schema, XSD, etc.).
     */
    SchemaType getType();
    
    /**
     * Get the schema content.
     */
    String getContent();
    
    /**
     * Get the schema version if specified.
     */
    Optional<String> getVersion();
    
    /**
     * Get the target namespace for XML schemas.
     */
    Optional<String> getTargetNamespace();
    
    /**
     * Validate content against this schema.
     */
    ValidationResult validate(String content);
    
    /**
     * Check if this schema is compatible with the given content type.
     */
    boolean isCompatibleWith(ContentType contentType);
    
    enum SchemaType {
        JSON_SCHEMA("application/schema+json"),
        XML_SCHEMA("application/xml"), 
        YAML_SCHEMA("application/yaml"),
        AVRO_SCHEMA("application/vnd.apache.avro+json"),
        CUSTOM("application/octet-stream");
        
        private final String mimeType;
        
        SchemaType(String mimeType) {
            this.mimeType = mimeType;
        }
        
        public String getMimeType() {
            return mimeType;
        }
    }
}
