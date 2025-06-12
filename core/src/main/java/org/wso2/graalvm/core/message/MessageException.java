package org.wso2.graalvm.core.message;

/**
 * Exception thrown when message operations fail.
 */
public class MessageException extends Exception {
    
    private final ErrorCode errorCode;
    private final String messageId;
    
    public MessageException(String message) {
        super(message);
        this.errorCode = ErrorCode.GENERAL_ERROR;
        this.messageId = null;
    }
    
    public MessageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.GENERAL_ERROR;
        this.messageId = null;
    }
    
    public MessageException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.messageId = null;
    }
    
    public MessageException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageId = null;
    }
    
    public MessageException(ErrorCode errorCode, String message, String messageId) {
        super(message);
        this.errorCode = errorCode;
        this.messageId = messageId;
    }
    
    public MessageException(ErrorCode errorCode, String message, String messageId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageId = messageId;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public enum ErrorCode {
        GENERAL_ERROR("GENERAL_ERROR", "General message processing error"),
        STREAM_CONSUMED("STREAM_CONSUMED", "Message stream has already been consumed"),
        STREAMING_ERROR("STREAMING_ERROR", "Error during streaming operation"),
        INVALID_CONTENT_TYPE("INVALID_CONTENT_TYPE", "Invalid or unsupported content type"),
        CONTENT_TYPE_MISMATCH("CONTENT_TYPE_MISMATCH", "Content type does not match expected type"),
        TYPE_CONVERSION_ERROR("TYPE_CONVERSION_ERROR", "Failed to convert content to requested type"),
        PARSING_ERROR("PARSING_ERROR", "Failed to parse message content"),
        TRANSFORMATION_ERROR("TRANSFORMATION_ERROR", "Failed to transform message content"),
        VALIDATION_ERROR("VALIDATION_ERROR", "Message validation failed"),
        IO_ERROR("IO_ERROR", "Input/output error while processing message"),
        SCHEMA_ERROR("SCHEMA_ERROR", "Schema validation error"),
        ENCODING_ERROR("ENCODING_ERROR", "Character encoding error"),
        SIZE_LIMIT_EXCEEDED("SIZE_LIMIT_EXCEEDED", "Message size exceeds configured limit");
        
        private final String code;
        private final String description;
        
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
