package org.wso2.graalvm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a configuration class as validated.
 * 
 * This replaces Spring's @Validated annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedConfig {
    
    /**
     * The validation groups to apply, if any.
     * @return the validation groups
     */
    Class<?>[] groups() default {};
}
