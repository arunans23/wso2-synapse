package org.wso2.graalvm.engine.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Pipeline for executing a sequence of mediators.
 * This replaces the Synapse mediation engine with a simpler, more direct approach.
 */
public class MediationPipeline {
    
    private static final Logger logger = LoggerFactory.getLogger(MediationPipeline.class);
    
    private final String name;
    private final List<Mediator> mediators;
    private final Executor executor;
    private final boolean parallelExecution;
    
    public MediationPipeline(String name, Executor executor) {
        this(name, executor, false);
    }
    
    public MediationPipeline(String name, Executor executor, boolean parallelExecution) {
        this.name = name;
        this.mediators = new ArrayList<>();
        this.executor = executor;
        this.parallelExecution = parallelExecution;
    }
    
    /**
     * Adds a mediator to the pipeline.
     */
    public MediationPipeline addMediator(Mediator mediator) {
        mediators.add(mediator);
        logger.debug("Added mediator {} to pipeline {}", mediator.getName(), name);
        return this;
    }
    
    /**
     * Executes the pipeline synchronously.
     */
    public boolean execute(IntegrationContext context) {
        logger.debug("Executing pipeline {} with context {}", name, context.getContextId());
        
        try {
            if (parallelExecution) {
                return executeParallel(context);
            } else {
                return executeSequential(context);
            }
        } catch (Exception e) {
            logger.error("Pipeline {} execution failed for context {}", name, context.getContextId(), e);
            context.setFault(e);
            return false;
        }
    }
    
    /**
     * Executes the pipeline asynchronously.
     */
    public CompletableFuture<Boolean> executeAsync(IntegrationContext context) {
        return CompletableFuture.supplyAsync(() -> execute(context), executor);
    }
    
    private boolean executeSequential(IntegrationContext context) {
        for (Mediator mediator : mediators) {
            if (context.hasFault()) {
                logger.warn("Stopping pipeline {} execution due to fault in context {}", 
                           name, context.getContextId());
                return false;
            }
            
            try {
                logger.debug("Executing mediator {} in pipeline {}", mediator.getName(), name);
                boolean result = mediator.mediate(context);
                
                if (!result) {
                    logger.warn("Mediator {} returned false, stopping pipeline {}", 
                               mediator.getName(), name);
                    return false;
                }
            } catch (Exception e) {
                logger.error("Mediator {} failed in pipeline {}", mediator.getName(), name, e);
                context.setFault(e);
                return false;
            }
        }
        
        logger.debug("Pipeline {} completed successfully for context {}", name, context.getContextId());
        return true;
    }
    
    private boolean executeParallel(IntegrationContext context) {
        // For parallel execution, we create copies of the context for each mediator
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (Mediator mediator : mediators) {
            IntegrationContext contextCopy = context.copy();
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return mediator.mediate(contextCopy);
                } catch (Exception e) {
                    logger.error("Parallel mediator {} failed in pipeline {}", 
                                mediator.getName(), name, e);
                    contextCopy.setFault(e);
                    return false;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all mediators to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        try {
            allOf.join();
            
            // Check if all mediators succeeded
            boolean allSucceeded = futures.stream()
                .allMatch(CompletableFuture::join);
            
            if (allSucceeded) {
                logger.debug("Parallel pipeline {} completed successfully", name);
            } else {
                logger.warn("Some mediators failed in parallel pipeline {}", name);
            }
            
            return allSucceeded;
            
        } catch (Exception e) {
            logger.error("Parallel pipeline {} execution failed", name, e);
            context.setFault(e);
            return false;
        }
    }
    
    /**
     * Gets the number of mediators in this pipeline.
     */
    public int size() {
        return mediators.size();
    }
    
    /**
     * Gets the pipeline name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets an immutable list of mediators.
     */
    public List<Mediator> getMediators() {
        return List.copyOf(mediators);
    }
    
    /**
     * Checks if the pipeline is configured for parallel execution.
     */
    public boolean isParallelExecution() {
        return parallelExecution;
    }
    
    @Override
    public String toString() {
        return String.format("MediationPipeline{name='%s', mediators=%d, parallel=%s}", 
                            name, mediators.size(), parallelExecution);
    }
}
