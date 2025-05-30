package org.wso2.graalvm.mediators.builtin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wso2.graalvm.core.context.IntegrationContext;

import static org.junit.jupiter.api.Assertions.*;

class LogMediatorTest {
    
    private LogMediator logMediator;
    private IntegrationContext context;
    
    @BeforeEach
    void setUp() {
        logMediator = new LogMediator("test-log");
        context = new IntegrationContext();
        context.setPayload("{\"message\":\"test\"}");
        context.setHeader("Content-Type", "application/json");
        context.setProperty("test.property", "test.value");
    }
    
    @Test
    void testSimpleLogging() {
        logMediator.setLogLevel(LogMediator.LogLevel.SIMPLE);
        boolean result = logMediator.mediate(context);
        assertTrue(result);
    }
    
    @Test
    void testFullLogging() {
        logMediator.setLogLevel(LogMediator.LogLevel.FULL);
        boolean result = logMediator.mediate(context);
        assertTrue(result);
    }
    
    @Test
    void testHeadersLogging() {
        logMediator.setLogLevel(LogMediator.LogLevel.HEADERS);
        boolean result = logMediator.mediate(context);
        assertTrue(result);
    }
    
    @Test
    void testCustomMessage() {
        logMediator.setLogMessage("Custom log message");
        logMediator.setLogLevel(LogMediator.LogLevel.CUSTOM);
        boolean result = logMediator.mediate(context);
        assertTrue(result);
    }
    
    @Test
    void testDifferentCategories() {
        logMediator.setCategory("DEBUG");
        assertTrue(logMediator.mediate(context));
        
        logMediator.setCategory("INFO");
        assertTrue(logMediator.mediate(context));
        
        logMediator.setCategory("WARN");
        assertTrue(logMediator.mediate(context));
        
        logMediator.setCategory("ERROR");
        assertTrue(logMediator.mediate(context));
    }
    
    @Test
    void testMediatorName() {
        assertEquals("test-log", logMediator.getName());
        assertEquals("LogMediator", logMediator.getType());
    }
}
