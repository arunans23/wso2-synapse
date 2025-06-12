package org.wso2.graalvm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a mediator component for auto-discovery
 * by the custom dependency injection container.
 * 
 * This replaces Spring's @Component annotation for mediator classes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MediatorComponent {
    
    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a managed component in case of an autodetected component.
     * @return the suggested component name, if any (or empty String otherwise)
     */
    String value() default "";
    
    /**
     * Scope of the component (singleton, prototype, etc.)
     * @return the scope name
     */
    String scope() default "singleton";
}
