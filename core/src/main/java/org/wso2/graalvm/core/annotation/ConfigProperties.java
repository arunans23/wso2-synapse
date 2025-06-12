package org.wso2.graalvm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class for configuration property binding.
 * 
 * This replaces Spring Boot's @ConfigurationProperties annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperties {
    
    /**
     * The prefix to use when binding configuration properties.
     * @return the configuration prefix
     */
    String value() default "";
    
    /**
     * Whether to ignore unknown properties during binding.
     * @return true if unknown properties should be ignored
     */
    boolean ignoreUnknownFields() default true;
    
    /**
     * Whether to ignore invalid properties during binding.
     * @return true if invalid properties should be ignored
     */
    boolean ignoreInvalidFields() default false;
}
