package org.wso2.graalvm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a REST endpoint.
 * 
 * This replaces Spring's @RestController annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestEndpoint {
    
    /**
     * The value may indicate a suggestion for a logical component name.
     * @return the suggested component name, if any
     */
    String value() default "";
}
