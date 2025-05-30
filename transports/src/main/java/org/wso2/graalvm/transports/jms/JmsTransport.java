package org.wso2.graalvm.transports.jms;

import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.core.threading.VirtualThreadExecutor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

/**
 * JMS transport for message queue integration.
 * Provides JMS connectivity with virtual thread support for high-performance async processing.
 */
public class JmsTransport {
    
    private static final Logger logger = LoggerFactory.getLogger(JmsTransport.class);
    
    private final String name;
    private final VirtualThreadExecutor executor;
    private final ConcurrentMap<String, JmsConnectionConfig> connections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MessageConsumer> consumers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MessageProducer> producers = new ConcurrentHashMap<>();
    
    public JmsTransport(String name, VirtualThreadExecutor executor) {
        this.name = name;
        this.executor = executor;
        logger.info("JMS transport '{}' initialized", name);
    }
    
    /**
     * Add a JMS connection configuration
     */
    public void addConnection(String connectionName, JmsConnectionConfig config) {
        connections.put(connectionName, config);
        logger.info("JMS connection '{}' configured for transport '{}'", connectionName, name);
    }
    
    /**
     * Send a message to a JMS destination
     */
    public CompletableFuture<Boolean> sendMessage(String connectionName, String destinationName, 
                                                  IntegrationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JmsConnectionConfig config = connections.get(connectionName);
                if (config == null) {
                    logger.error("JMS connection '{}' not found", connectionName);
                    context.setFault(new IllegalArgumentException("JMS connection not found: " + connectionName));
                    return false;
                }
                
                Connection connection = config.getConnectionFactory().createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                Destination destination = config.isQueue() ? 
                    session.createQueue(destinationName) : 
                    session.createTopic(destinationName);
                
                String producerKey = connectionName + ":" + destinationName;
                MessageProducer producer = producers.computeIfAbsent(producerKey, k -> {
                    try {
                        return session.createProducer(destination);
                    } catch (JMSException e) {
                        logger.error("Failed to create JMS producer for {}", k, e);
                        return null;
                    }
                });
                
                if (producer == null) {
                    logger.error("Failed to create JMS producer for destination: {}", destinationName);
                    context.setFault(new RuntimeException("Failed to create JMS producer"));
                    return false;
                }
                
                Message message = createJmsMessage(session, context);
                producer.send(message);
                
                logger.debug("JMS message sent to destination: {}", destinationName);
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to send JMS message to destination: {}", destinationName, e);
                context.setFault(e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Start consuming messages from a JMS destination
     */
    public CompletableFuture<Void> startConsumer(String connectionName, String destinationName, 
                                                 JmsMessageHandler handler) {
        return CompletableFuture.runAsync(() -> {
            try {
                JmsConnectionConfig config = connections.get(connectionName);
                if (config == null) {
                    logger.error("JMS connection '{}' not found", connectionName);
                    return;
                }
                
                Connection connection = config.getConnectionFactory().createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                Destination destination = config.isQueue() ? 
                    session.createQueue(destinationName) : 
                    session.createTopic(destinationName);
                
                String consumerKey = connectionName + ":" + destinationName;
                MessageConsumer consumer = consumers.computeIfAbsent(consumerKey, k -> {
                    try {
                        return session.createConsumer(destination);
                    } catch (JMSException e) {
                        logger.error("Failed to create JMS consumer for {}", k, e);
                        return null;
                    }
                });
                
                if (consumer == null) {
                    logger.error("Failed to create JMS consumer for destination: {}", destinationName);
                    return;
                }
                
                consumer.setMessageListener(message -> {
                    executor.execute(() -> {
                        try {
                            IntegrationContext context = createContextFromJmsMessage(message);
                            handler.handleMessage(context, message);
                        } catch (Exception e) {
                            logger.error("Error processing JMS message from destination: {}", 
                                       destinationName, e);
                        }
                    });
                });
                
                connection.start();
                logger.info("JMS consumer started for destination: {}", destinationName);
                
            } catch (Exception e) {
                logger.error("Failed to start JMS consumer for destination: {}", destinationName, e);
            }
        }, executor);
    }
    
    /**
     * Receive a single message from a JMS destination (blocking)
     */
    public CompletableFuture<IntegrationContext> receiveMessage(String connectionName, String destinationName, 
                                                               long timeoutMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JmsConnectionConfig config = connections.get(connectionName);
                if (config == null) {
                    logger.error("JMS connection '{}' not found", connectionName);
                    return null;
                }
                
                Connection connection = config.getConnectionFactory().createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                
                Destination destination = config.isQueue() ? 
                    session.createQueue(destinationName) : 
                    session.createTopic(destinationName);
                
                MessageConsumer consumer = session.createConsumer(destination);
                connection.start();
                
                Message message = consumer.receive(timeoutMs);
                
                if (message != null) {
                    IntegrationContext context = createContextFromJmsMessage(message);
                    logger.debug("JMS message received from destination: {}", destinationName);
                    return context;
                } else {
                    logger.debug("No JMS message received within timeout from destination: {}", 
                               destinationName);
                    return null;
                }
                
            } catch (Exception e) {
                logger.error("Failed to receive JMS message from destination: {}", destinationName, e);
                return null;
            }
        }, executor);
    }
    
    private Message createJmsMessage(Session session, IntegrationContext context) throws JMSException {
        Object payload = context.getPayload();
        
        Message message;
        if (payload instanceof String textPayload) {
            message = session.createTextMessage(textPayload);
        } else if (payload instanceof byte[] bytesPayload) {
            BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage.writeBytes(bytesPayload);
            message = bytesMessage;
        } else {
            // Convert to JSON string
            String jsonPayload = payload != null ? payload.toString() : "";
            message = session.createTextMessage(jsonPayload);
        }
        
        // Set JMS headers from context headers
        for (Map.Entry<String, Object> header : context.getHeaders().entrySet()) {
            String headerName = header.getKey();
            Object headerValue = header.getValue();
            
            if (headerValue instanceof String stringValue) {
                message.setStringProperty("custom_" + headerName, stringValue);
            } else if (headerValue instanceof Integer intValue) {
                message.setIntProperty("custom_" + headerName, intValue);
            } else if (headerValue instanceof Long longValue) {
                message.setLongProperty("custom_" + headerName, longValue);
            } else if (headerValue instanceof Boolean boolValue) {
                message.setBooleanProperty("custom_" + headerName, boolValue);
            }
        }
        
        // Set context properties as JMS properties
        for (Map.Entry<String, Object> property : context.getProperties().entrySet()) {
            String propName = property.getKey();
            Object propValue = property.getValue();
            
            if (propValue instanceof String stringValue) {
                message.setStringProperty("ctx_" + propName, stringValue);
            }
        }
        
        return message;
    }
    
    private IntegrationContext createContextFromJmsMessage(Message message) throws JMSException {
        IntegrationContext context = new IntegrationContext();
        
        // Extract payload
        if (message instanceof TextMessage textMessage) {
            context.setPayload(textMessage.getText());
        } else if (message instanceof BytesMessage bytesMessage) {
            byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(bytes);
            context.setPayload(bytes);
        } else if (message instanceof ObjectMessage objectMessage) {
            context.setPayload(objectMessage.getObject());
        }
        
        // Extract JMS headers
        context.setHeader("JMSMessageID", message.getJMSMessageID());
        context.setHeader("JMSTimestamp", message.getJMSTimestamp());
        context.setHeader("JMSCorrelationID", message.getJMSCorrelationID());
        context.setHeader("JMSType", message.getJMSType());
        context.setHeader("JMSPriority", message.getJMSPriority());
        context.setHeader("JMSExpiration", message.getJMSExpiration());
        context.setHeader("JMSRedelivered", message.getJMSRedelivered());
        
        if (message.getJMSDestination() != null) {
            context.setHeader("JMSDestination", message.getJMSDestination().toString());
        }
        if (message.getJMSReplyTo() != null) {
            context.setHeader("JMSReplyTo", message.getJMSReplyTo().toString());
        }
        
        return context;
    }
    
    /**
     * Stop the JMS transport and clean up resources
     */
    public void stop() {
        logger.info("Stopping JMS transport '{}'", name);
        
        // Close consumers
        consumers.values().forEach(consumer -> {
            try {
                consumer.close();
            } catch (JMSException e) {
                logger.warn("Error closing JMS consumer", e);
            }
        });
        consumers.clear();
        
        // Close producers
        producers.values().forEach(producer -> {
            try {
                producer.close();
            } catch (JMSException e) {
                logger.warn("Error closing JMS producer", e);
            }
        });
        producers.clear();
        
        logger.info("JMS transport '{}' stopped", name);
    }
    
    /**
     * JMS connection configuration
     */
    public static class JmsConnectionConfig {
        private final ConnectionFactory connectionFactory;
        private final boolean isQueue;
        
        public JmsConnectionConfig(ConnectionFactory connectionFactory, boolean isQueue) {
            this.connectionFactory = connectionFactory;
            this.isQueue = isQueue;
        }
        
        public ConnectionFactory getConnectionFactory() {
            return connectionFactory;
        }
        
        public boolean isQueue() {
            return isQueue;
        }
    }
    
    /**
     * JMS message handler interface
     */
    @FunctionalInterface
    public interface JmsMessageHandler {
        void handleMessage(IntegrationContext context, Message jmsMessage) throws Exception;
    }
}
