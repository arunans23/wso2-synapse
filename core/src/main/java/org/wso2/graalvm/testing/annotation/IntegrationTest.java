package org.wso2.graalvm.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark integration test classes.
 * 
 * This replaces Spring's @SpringJUnitConfig annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationTest {
    
    /**
     * Configuration classes to load for the test.
     * @return the configuration classes
     */
    Class<?>[] classes() default {};
    
    /**
     * Configuration properties to set for the test.
     * @return the configuration properties
     */
    String[] properties() default {};
    
    /**
     * Whether to use a test-specific DI container.
     * @return true if test container should be used
     */
    boolean useTestContainer() default true;
}
