package org.wso2.graalvm.core.message;

/**
 * Interface for message transformations between different content types.
 */
public interface MessageTransformer {
    
    /**
     * Check if this transformer can handle the transformation from source to target content type.
     * @param sourceType the source content type
     * @param targetType the target content type
     * @return true if transformation is supported
     */
    boolean canTransform(ContentType sourceType, ContentType targetType);
    
    /**
     * Transform a message from one content type to another.
     * @param message the source message
     * @param targetType the target content type
     * @return the transformed message
     * @throws MessageException if transformation fails
     */
    Message transform(Message message, ContentType targetType) throws MessageException;
    
    /**
     * Get the priority of this transformer (higher numbers = higher priority).
     * Used when multiple transformers can handle the same transformation.
     * @return the priority
     */
    default int getPriority() {
        return 0;
    }
}
