package org.wso2.graalvm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark HTTP routing for REST endpoints.
 * 
 * This replaces Spring's @RequestMapping annotation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
    
    /**
     * The path or paths that this endpoint handles.
     * @return the request paths
     */
    String[] value() default {};
    
    /**
     * The HTTP methods this endpoint accepts.
     * @return the HTTP methods
     */
    String[] method() default {};
    
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
