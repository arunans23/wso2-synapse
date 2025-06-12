package org.wso2.graalvm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for HTTP POST endpoints.
 * 
 * This replaces Spring's @PostMapping annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {
    
    /**
     * The path that this endpoint handles.
     * @return the request path
     */
    String value() default "";
    
    /**
     * The content types this endpoint produces.
     * @return the content types
     */
    String[] produces() default {};
    
    /**
     * The content types this endpoint consumes.
     * @return the content types
     */
    String[] consumes() default {};
}
