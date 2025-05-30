package org.wso2.graalvm.mediators.builtin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

/**
 * Respond mediator for responding back to the client.
 * This replaces the traditional Respond mediator with simplified response handling.
 */
public class RespondMediator implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(RespondMediator.class);
    
    private final String name;
    private String responsePayload;
    private String contentType = "application/json";
    private int statusCode = 200;
    
    public RespondMediator(String name) {
        this.name = name;
    }
    
    public RespondMediator setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
        return this;
    }
    
    public RespondMediator setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
    
    public RespondMediator setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        logger.debug("Respond mediator {} processing response", name);
        
        try {
            // Set response properties
            context.setProperty("HTTP_STATUS_CODE", statusCode);
            context.setProperty("CONTENT_TYPE", contentType);
            context.setProperty("RESPONSE_COMPLETE", true);
            
            // Set response payload if provided, otherwise use current payload
            if (responsePayload != null) {
                context.setPayload(responsePayload);
                logger.debug("Response payload set to: {}", responsePayload);
            } else {
                logger.debug("Using existing payload for response");
            }
            
            // Set response headers
            context.setHeader("Content-Type", contentType);
            context.setHeader("X-Powered-By", "WSO2-MI-GraalVM");
            
            logger.debug("Response prepared with status code: {}, content type: {}", 
                        statusCode, contentType);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error in respond mediator {}", name, e);
            context.setFault(e);
            return false;
        }
    }
    
    @Override
    public String toString() {
        return String.format("RespondMediator{name='%s', statusCode=%d, contentType='%s'}", 
                           name, statusCode, contentType);
    }
}
