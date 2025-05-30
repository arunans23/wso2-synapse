package org.wso2.graalvm.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.core.threading.VirtualThreadExecutor;
import org.wso2.graalvm.engine.pipeline.MediationPipeline;
import org.wso2.graalvm.mediators.builtin.LogMediator;
import org.wso2.graalvm.mediators.builtin.PropertyMediator;
import org.wso2.graalvm.connectors.http.HttpCallMediator;

/**
 * Example demonstrating a simple integration flow with virtual threads.
 */
public class SimpleIntegrationExample {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleIntegrationExample.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("Starting Simple Integration Example");
        
        // Create virtual thread executor
        VirtualThreadExecutor executor = new VirtualThreadExecutor("example");
        
        try {
            // Create a simple integration pipeline
            MediationPipeline pipeline = new MediationPipeline("simple-example", executor)
                .addMediator(new LogMediator("start-log")
                    .setLogLevel(LogMediator.LogLevel.FULL)
                    .setLogMessage("Starting request processing"))
                    
                .addMediator(new PropertyMediator("set-response")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("response.message")
                    .setPropertyValue("Hello from WSO2 Micro Integrator GraalVM!"))
                    
                .addMediator(new PropertyMediator("set-timestamp")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("response.timestamp")
                    .setPropertyValue(String.valueOf(System.currentTimeMillis())))
                    
                .addMediator(new LogMediator("end-log")
                    .setLogLevel(LogMediator.LogLevel.SIMPLE)
                    .setLogMessage("Request processing completed"));
            
            // Create integration context with sample data
            IntegrationContext context = new IntegrationContext();
            context.setPayload("{\"user\":\"john\",\"action\":\"getData\"}");
            context.setHeader("Content-Type", "application/json");
            context.setHeader("User-Agent", "WSO2-MI-GraalVM/1.0");
            
            // Execute the pipeline
            logger.info("Executing integration pipeline...");
            boolean success = pipeline.execute(context);
            
            if (success) {
                logger.info("Pipeline executed successfully!");
                logger.info("Final payload: {}", context.getPayload());
                logger.info("Response message: {}", context.getProperty("response.message"));
                logger.info("Response timestamp: {}", context.getProperty("response.timestamp"));
            } else {
                logger.error("Pipeline execution failed");
                if (context.hasFault()) {
                    logger.error("Fault: ", context.getFault());
                }
            }
            
            // Demonstrate async execution
            logger.info("Demonstrating async execution...");
            pipeline.executeAsync(context)
                   .thenAccept(result -> logger.info("Async execution completed: {}", result))
                   .join();
            
        } finally {
            executor.shutdown();
            logger.info("Example completed");
        }
    }
}
