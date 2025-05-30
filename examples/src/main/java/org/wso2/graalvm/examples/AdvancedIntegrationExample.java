package org.wso2.graalvm.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.connectors.database.DatabaseConnector;
import org.wso2.graalvm.connectors.http.HttpCallMediator;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.core.threading.VirtualThreadExecutor;
import org.wso2.graalvm.engine.pipeline.MediationPipeline;
import org.wso2.graalvm.mediators.builtin.*;
import org.wso2.graalvm.transports.http.HttpClient;

/**
 * Advanced integration example demonstrating complex message flows
 * with database operations, HTTP calls, transformations, and error handling.
 */
public class AdvancedIntegrationExample {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedIntegrationExample.class);
    
    public static void main(String[] args) {
        logger.info("Starting Advanced Integration Example");
        
        try {
            // Initialize virtual thread executor
            VirtualThreadExecutor executor = new VirtualThreadExecutor("example-executor");
            
            // Initialize HTTP client
            HttpClient httpClient = new HttpClient(executor);
            
            // Run different integration scenarios
            runUserRegistrationFlow(executor, httpClient);
            runDataTransformationFlow(executor);
            runErrorHandlingFlow(executor);
            runParallelProcessingFlow(executor);
            
            logger.info("Advanced Integration Example completed successfully");
            
        } catch (Exception e) {
            logger.error("Error in Advanced Integration Example", e);
        }
    }
    
    /**
     * User registration flow: Validate, transform, store in DB, send notification
     */
    private static void runUserRegistrationFlow(VirtualThreadExecutor executor, HttpClient httpClient) {
        logger.info("=== User Registration Flow ===");
        
        try {
            // Create database connector for user storage
            DatabaseConnector dbConnector = new DatabaseConnector("user-db-connector")
                .setDataSource("userdb")
                .setSqlQuery("INSERT INTO users (name, email, age, created_at) VALUES (?, ?, ?, NOW())")
                .setOperation(DatabaseConnector.DatabaseOperation.INSERT)
                .addParameter("name", "${user.name}")
                .addParameter("email", "${user.email}")
                .addParameter("age", "${user.age}")
                .setExecutor(executor);
            
            // Create HTTP notification mediator
            HttpCallMediator notificationMediator = new HttpCallMediator("notification-service")
                .setUrl("https://api.notification-service.com/send")
                .setMethod("POST")
                .setTimeout(5000)
                .setExecutor(executor);
            
            // Build the registration pipeline
            MediationPipeline registrationPipeline = new MediationPipeline("user-registration", executor)
                
                // 1. Log incoming request
                .addMediator(new LogMediator("request-logger")
                    .setLogLevel(LogMediator.LogLevel.FULL)
                    .setLogMessage("User registration request received"))
                
                // 2. Validate user data
                .addMediator(new PropertyMediator("validator")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("validation.status")
                    .setPropertyValue("validated"))
                
                // 3. Transform user data for database storage
                .addMediator(new TransformMediator("user-transformer")
                    .setTransformationType("FIELD_MAPPING")
                    .addFieldMapping("fullName", "user.name")
                    .addFieldMapping("emailAddress", "user.email")
                    .addFieldMapping("userAge", "user.age"))
                
                // 4. Store user in database
                .addMediator(dbConnector)
                
                // 5. Prepare notification payload
                .addMediator(new TransformMediator("notification-transformer")
                    .setTransformationType("TEMPLATE")
                    .setTemplate("{\"type\":\"USER_REGISTERED\",\"user\":\"${user.email}\",\"message\":\"Welcome ${user.name}!\"}"))
                
                // 6. Send notification
                .addMediator(notificationMediator)
                
                // 7. Prepare success response
                .addMediator(new RespondMediator("success-response")
                    .setStatusCode(201)
                    .setContentType("application/json")
                    .setResponsePayload("{\"status\":\"success\",\"message\":\"User registered successfully\"}"));
            
            // Execute with sample user data
            IntegrationContext context = new IntegrationContext();
            context.setPayload("{\"fullName\":\"John Doe\",\"emailAddress\":\"john.doe@example.com\",\"userAge\":30}");
            context.setProperty("user.name", "John Doe");
            context.setProperty("user.email", "john.doe@example.com");
            context.setProperty("user.age", 30);
            
            boolean success = registrationPipeline.execute(context);
            
            logger.info("User registration flow completed: {}", success ? "SUCCESS" : "FAILED");
            if (context.hasFault()) {
                logger.error("Registration error: ", context.getFault());
            }
            
        } catch (Exception e) {
            logger.error("Error in user registration flow", e);
        }
    }
    
    /**
     * Data transformation flow: Complex JSON transformations
     */
    private static void runDataTransformationFlow(VirtualThreadExecutor executor) {
        logger.info("=== Data Transformation Flow ===");
        
        try {
            MediationPipeline transformationPipeline = new MediationPipeline("data-transformation", executor)
                
                // 1. Log incoming data
                .addMediator(new LogMediator("input-logger")
                    .setLogLevel(LogMediator.LogLevel.PAYLOAD)
                    .setLogMessage("Processing data transformation"))
                
                // 2. Transform order data structure
                .addMediator(new TransformMediator("order-transformer")
                    .setTransformationType("FIELD_MAPPING")
                    .addFieldMapping("order.id", "orderId")
                    .addFieldMapping("order.customer.name", "customerName")
                    .addFieldMapping("order.customer.email", "customerEmail")
                    .addFieldMapping("order.items", "orderItems")
                    .addFieldMapping("order.total", "totalAmount"))
                
                // 3. Add calculated fields
                .addMediator(new PropertyMediator("add-metadata")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("processedAt")
                    .setPropertyValue(String.valueOf(System.currentTimeMillis())))
                
                // 4. Create final transformed structure
                .addMediator(new TransformMediator("final-transformer")
                    .setTransformationType("TEMPLATE")
                    .setTemplate("""
                        {
                          "transformedOrder": {
                            "id": "${orderId}",
                            "customer": {
                              "name": "${customerName}",
                              "email": "${customerEmail}"
                            },
                            "items": ${orderItems},
                            "total": ${totalAmount},
                            "metadata": {
                              "processedAt": "${processedAt}",
                              "version": "2.0"
                            }
                          }
                        }
                        """))
                
                // 5. Log transformed result
                .addMediator(new LogMediator("output-logger")
                    .setLogLevel(LogMediator.LogLevel.PAYLOAD)
                    .setLogMessage("Data transformation completed"));
            
            // Execute with sample order data
            IntegrationContext context = new IntegrationContext();
            context.setPayload("""
                {
                  "order": {
                    "id": "ORD-12345",
                    "customer": {
                      "name": "Jane Smith",
                      "email": "jane.smith@example.com"
                    },
                    "items": [
                      {"product": "Laptop", "price": 999.99, "quantity": 1},
                      {"product": "Mouse", "price": 29.99, "quantity": 1}
                    ],
                    "total": 1029.98
                  }
                }
                """);
            
            boolean success = transformationPipeline.execute(context);
            
            logger.info("Data transformation flow completed: {}", success ? "SUCCESS" : "FAILED");
            logger.info("Transformed payload: {}", context.getPayload());
            
        } catch (Exception e) {
            logger.error("Error in data transformation flow", e);
        }
    }
    
    /**
     * Error handling flow: Demonstrate fault management and recovery
     */
    private static void runErrorHandlingFlow(VirtualThreadExecutor executor) {
        logger.info("=== Error Handling Flow ===");
        
        try {
            MediationPipeline errorHandlingPipeline = new MediationPipeline("error-handling", executor)
                
                // 1. Start processing
                .addMediator(new LogMediator("start-logger")
                    .setLogMessage("Starting error handling demonstration"))
                
                // 2. Simulate an operation that might fail
                .addMediator(new PropertyMediator("risky-operation")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("operation.status")
                    .setPropertyValue("processing"))
                
                // 3. Transform with potential error
                .addMediator(new TransformMediator("error-prone-transform")
                    .setTransformationType("JSON_TO_JSON")
                    .addFieldMapping("nonexistent.field", "target.field"))
                
                // 4. Handle errors gracefully
                .addMediator(new PropertyMediator("error-handler")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("error.handled")
                    .setPropertyValue("true"))
                
                // 5. Prepare error response
                .addMediator(new RespondMediator("error-response")
                    .setStatusCode(500)
                    .setContentType("application/json")
                    .setResponsePayload("{\"error\":\"Processing failed\",\"recovery\":\"Error handled gracefully\"}"));
            
            // Execute with problematic data
            IntegrationContext context = new IntegrationContext();
            context.setPayload("{\"validField\":\"validValue\"}");
            
            boolean success = errorHandlingPipeline.execute(context);
            
            logger.info("Error handling flow completed: {}", success ? "SUCCESS" : "FAILED");
            if (context.hasFault()) {
                logger.info("Fault handled: {}", context.getFault().getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Error in error handling flow", e);
        }
    }
    
    /**
     * Parallel processing flow: Demonstrate concurrent mediator execution
     */
    private static void runParallelProcessingFlow(VirtualThreadExecutor executor) {
        logger.info("=== Parallel Processing Flow ===");
        
        try {
            MediationPipeline parallelPipeline = new MediationPipeline("parallel-processing", executor)
                .setExecutionMode(MediationPipeline.ExecutionMode.PARALLEL)
                
                // These mediators will run in parallel
                .addMediator(new PropertyMediator("parallel-task-1")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("task1.result")
                    .setPropertyValue("Task 1 completed"))
                
                .addMediator(new PropertyMediator("parallel-task-2")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("task2.result")
                    .setPropertyValue("Task 2 completed"))
                
                .addMediator(new PropertyMediator("parallel-task-3")
                    .setAction(PropertyMediator.Action.SET)
                    .setPropertyName("task3.result")
                    .setPropertyValue("Task 3 completed"))
                
                .addMediator(new LogMediator("parallel-logger")
                    .setLogMessage("Parallel task executing"));
            
            // Sequential pipeline to aggregate results
            MediationPipeline aggregationPipeline = new MediationPipeline("result-aggregation", executor)
                .addMediator(new TransformMediator("result-aggregator")
                    .setTransformationType("TEMPLATE")
                    .setTemplate("""
                        {
                          "parallelResults": {
                            "task1": "${task1.result}",
                            "task2": "${task2.result}",
                            "task3": "${task3.result}",
                            "completedAt": "${timestamp}"
                          }
                        }
                        """))
                
                .addMediator(new LogMediator("aggregation-logger")
                    .setLogLevel(LogMediator.LogLevel.PAYLOAD)
                    .setLogMessage("Parallel processing results aggregated"));
            
            // Execute parallel processing
            IntegrationContext context = new IntegrationContext();
            context.setPayload("{\"operation\":\"parallel-demo\"}");
            context.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
            
            boolean parallelSuccess = parallelPipeline.execute(context);
            boolean aggregationSuccess = aggregationPipeline.execute(context);
            
            logger.info("Parallel processing flow completed: Parallel={}, Aggregation={}", 
                       parallelSuccess ? "SUCCESS" : "FAILED",
                       aggregationSuccess ? "SUCCESS" : "FAILED");
            
            logger.info("Final result: {}", context.getPayload());
            
        } catch (Exception e) {
            logger.error("Error in parallel processing flow", e);
        }
    }
}
