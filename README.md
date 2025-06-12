# WSO2 Micro Integrator - GraalVM Edition

A modern rewrite of WSO2 Micro Integrator built with GraalVM native image support and Java 21 Virtual Threads, completely removing OSGi dependencies.

## 🎯 Project Vision

This is a **next-generation integration runtime** for new customers who need:
- **Ultra-fast startup times** (< 100ms with native image)
- **Lightweight resource footprint** (< 100MB memory)
- **Cloud-native deployment** (Kubernetes, containers)
- **Modern Java features** (Virtual Threads, Records, Pattern Matching)
- **Developer-friendly architecture** (YAML config, simple APIs)

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (GraalVM CE 21.0.2+ recommended)
- **Maven 3.8+**
- **Docker** (optional, for containerized builds)

### Build and Run
```bash
# Clone and build
git clone <repository-url>
cd graalvmsynapse
mvn clean package

# Run JVM version
java -cp "runtime/target/classes:runtime/target/dependency/*:security/target/classes:core/target/classes:management/target/classes" org.wso2.graalvm.runtime.MicroIntegratorApplication

# Build native image (59MB executable, 34ms startup)
mvn package -Pnative -DskipTests -pl distribution

# Run native image
./distribution/target/wso2-micro-integrator
```

## 🏗 Architecture Overview

### Core Design Principles
1. **No OSGi**: Direct dependency injection, simplified class loading
2. **Cloud-First**: Native image support, container-friendly
3. **Developer Experience**: YAML configuration, clear APIs
4. **Performance**: Virtual threads, non-blocking I/O
5. **Modularity**: Pluggable components, minimal core

### Technology Stack
- **Runtime**: Java 21 + GraalVM Native Image
- **HTTP Server**: Helidon 4 WebServer (replaced Netty)
- **Dependency Injection**: Custom lightweight DI container
- **Concurrency**: Virtual Threads + CompletableFuture
- **Serialization**: Jackson (JSON/YAML)
- **Logging**: SLF4J + Logback
- **Build**: Maven + GraalVM native image packaging

## 📁 Module Structure

```
micro-integrator-graalvm/
├── core/                    # 🏗 Foundation classes
│   ├── config/             # YAML configuration management
│   ├── context/            # Message context and threading
│   └── threading/          # Virtual thread executor
├── runtime/                 # 🚀 Application bootstrap
│   ├── server/             # HelidonHttpServer (HTTP/HTTPS)
│   └── MicroIntegratorApplication.java
├── integration-engine/      # 🔄 Message processing pipeline
│   ├── pipeline/           # Mediation sequence execution
│   └── dispatcher/         # Request routing logic
├── mediators/              # 🧩 Transformation & routing logic
│   ├── builtin/            # Core mediators (Log, Property, etc.)
│   └── transform/          # Transform mediator (content-type conversion)
├── transports/             # 🌐 Protocol implementations
│   ├── http/               # HTTP/HTTPS listeners & senders
│   └── listeners/          # Inbound endpoint implementations
├── management/             # 📊 Monitoring & administration
│   ├── api/                # Management REST API
│   ├── health/             # Health checks
│   └── metrics/            # Prometheus-style metrics
├── security/               # 🔐 Authentication & authorization
│   ├── auth/               # Basic auth, API keys, JWT
│   └── filter/             # HelidonSecurityFilter
├── cli/                    # 💻 Command-line interface
│   ├── commands/           # Deploy, manage, debug commands
│   └── utils/              # HTTP client utilities
└── distribution/           # 📦 Native image packaging
    ├── assembly/           # Distribution assembly
    └── native-image/       # GraalVM configuration
```

## 🔧 Feature Implementation Guide (AI Development)

### Priority 1: Core Mediators (First Cut)

#### ✅ Implemented Mediators
- **LogMediator**: Message logging with configurable levels
- **PropertyMediator**: Context property manipulation
- **RespondMediator**: Response generation

#### 🚧 Required Mediators (AI Implementation Targets)

##### Transform Mediator - Content Type Conversion
```java
// Location: mediators/src/main/java/org/wso2/graalvm/mediators/builtin/TransformMediator.java
@MediatorComponent
public class TransformMediator implements Mediator {
    private final ContentType sourceType;
    private final ContentType targetType;
    private final TransformationEngine engine;
    
    /**
     * AI Implementation Requirements:
     * 1. JSON ↔ XML ↔ CSV ↔ YAML bidirectional conversion
     * 2. XSLT 3.0 support for XML transformations
     * 3. JSONPath and JSONata for JSON transformations
     * 4. Custom transformation scripts (Groovy/JavaScript)
     * 5. Schema validation before/after transformation
     * 6. Streaming support for large payloads (>10MB)
     */
    
    @Override
    public CompletableFuture<MediationResult> mediate(MessageContext context) {
        // AI Task: Implement async transformation with error handling
        return CompletableFuture.supplyAsync(() -> {
            try {
                Message input = context.getMessage();
                Message transformed = engine.transform(input, sourceType, targetType);
                context.setMessage(transformed);
                return MediationResult.success();
            } catch (TransformationException e) {
                return MediationResult.failure(e);
            }
        }, context.getExecutor());
    }
}

// Transformation Engine Interface
public interface TransformationEngine {
    Message transform(Message input, ContentType from, ContentType to) 
        throws TransformationException;
    
    boolean supports(ContentType from, ContentType to);
    ValidationResult validate(Message input, Schema schema);
}

// Configuration Example:
// transform:
//   sourceType: "application/json"
//   targetType: "application/xml" 
//   script: "jsonToXml.xslt"
//   validation:
//     input: "user-schema.json"
//     output: "user-schema.xsd"
```

##### Send Mediator - HTTP Client with Connection Pooling
```java
// Location: mediators/src/main/java/org/wso2/graalvm/mediators/builtin/SendMediator.java
@MediatorComponent
public class SendMediator implements Mediator {
    private final HttpClientManager clientManager;
    private final LoadBalancer loadBalancer;
    private final CircuitBreaker circuitBreaker;
    
    /**
     * AI Implementation Requirements:
     * 1. HTTP/1.1 and HTTP/2 support
     * 2. Connection pooling (per endpoint, configurable limits)
     * 3. Load balancing (round-robin, weighted, least-connections)
     * 4. Circuit breaker pattern (fail-fast, auto-recovery)
     * 5. Retry logic with exponential backoff
     * 6. Timeout handling (connection, request, idle)
     * 7. SSL/TLS configuration and certificate management
     * 8. Request/response correlation and tracing
     */
    
    @Override
    public CompletableFuture<MediationResult> mediate(MessageContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                EndpointReference endpoint = loadBalancer.selectEndpoint();
                HttpRequest request = buildHttpRequest(context, endpoint);
                
                return circuitBreaker.execute(() -> {
                    HttpResponse response = clientManager.send(request);
                    updateContextWithResponse(context, response);
                    return MediationResult.success();
                });
            } catch (Exception e) {
                return MediationResult.failure(e);
            }
        }, context.getExecutor());
    }
}

// Connection Pool Configuration
public class ConnectionPoolConfig {
    private int maxConnectionsPerRoute = 20;
    private int maxTotalConnections = 100;
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private Duration requestTimeout = Duration.ofSeconds(30);
    private Duration idleTimeout = Duration.ofMinutes(5);
}

// Load Balancer Implementation
public interface LoadBalancer {
    EndpointReference selectEndpoint();
    void reportFailure(EndpointReference endpoint);
    void reportSuccess(EndpointReference endpoint);
}

// Configuration Example:
// send:
//   endpoint: 
//     uri: "https://api.example.com/users"
//     method: "POST"
//     headers:
//       Content-Type: "application/json"
//       Authorization: "Bearer ${jwt.token}"
//   loadBalancing:
//     algorithm: "round-robin"
//     endpoints:
//       - "https://api1.example.com"
//       - "https://api2.example.com"
//   circuitBreaker:
//     failureThreshold: 5
//     recoveryTimeout: "30s"
//   retry:
//     maxAttempts: 3
//     backoffMultiplier: 2
```

##### Filter Mediator - Conditional Processing
```java
// Location: mediators/src/main/java/org/wso2/graalvm/mediators/builtin/FilterMediator.java
@MediatorComponent
public class FilterMediator implements Mediator {
    private final ExpressionEvaluator evaluator;
    private final MediationPipeline thenSequence;
    private final MediationPipeline elseSequence;
    
    /**
     * AI Implementation Requirements:
     * 1. XPath 3.1 expression evaluation for XML payloads
     * 2. JSONPath expression evaluation for JSON payloads
     * 3. Simple expression language for headers/properties
     * 4. Regular expression matching
     * 5. Logical operators (AND, OR, NOT)
     * 6. Comparison operators (=, !=, <, >, contains, matches)
     * 7. Context variable and property access
     * 8. Performance optimization (expression compilation/caching)
     */
    
    @Override
    public CompletableFuture<MediationResult> mediate(MessageContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean conditionResult = evaluator.evaluate(condition, context);
                MediationPipeline selectedPipeline = conditionResult ? thenSequence : elseSequence;
                
                if (selectedPipeline != null) {
                    return selectedPipeline.execute(context);
                }
                return MediationResult.success();
            } catch (Exception e) {
                return MediationResult.failure(e);
            }
        }, context.getExecutor());
    }
}

// Expression Language Grammar
public interface ExpressionEvaluator {
    boolean evaluate(String expression, MessageContext context);
    Object evaluateToObject(String expression, MessageContext context);
    
    // Built-in functions
    // $header.Content-Type = 'application/json'
    // $payload.user.age > 18
    // $property.region matches 'us-.*'
    // $env.NODE_ENV = 'production'
}

// Configuration Example:
// filter:
//   condition: "$header.Content-Type = 'application/json' AND $payload.user.age >= 18"
//   then:
//     - type: "log"
//       message: "Processing adult user JSON request"
//     - type: "send"
//       endpoint: "https://adult-api.example.com"
//   else:
//     - type: "respond"
//       status: 400
//       payload: '{"error": "Invalid request format or age"}'
```

##### Switch Mediator - Multi-branch Routing
```java
// Location: mediators/src/main/java/org/wso2/graalvm/mediators/builtin/SwitchMediator.java
@MediatorComponent
public class SwitchMediator implements Mediator {
    private final ExpressionEvaluator evaluator;
    private final Map<String, MediationPipeline> cases;
    private final MediationPipeline defaultCase;
    
    /**
     * AI Implementation Requirements:
     * 1. Pattern matching with wildcard support (*, ?)
     * 2. Regular expression case matching
     * 3. Range matching for numeric values
     * 4. Multiple case values (case1|case2|case3)
     * 5. Fall-through behavior configuration
     * 6. Default case handling
     * 7. Case priority and ordering
     * 8. Performance optimization for large case sets
     */
    
    @Override
    public CompletableFuture<MediationResult> mediate(MessageContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sourceValue = String.valueOf(evaluator.evaluateToObject(sourceExpression, context));
                
                // Find matching case
                MediationPipeline matchedPipeline = findMatchingCase(sourceValue);
                if (matchedPipeline != null) {
                    return matchedPipeline.execute(context);
                }
                
                // Execute default case if no match
                if (defaultCase != null) {
                    return defaultCase.execute(context);
                }
                
                return MediationResult.success();
            } catch (Exception e) {
                return MediationResult.failure(e);
            }
        }, context.getExecutor());
    }
    
    private MediationPipeline findMatchingCase(String value) {
        // AI Task: Implement efficient pattern matching
        // Support: exact match, wildcard, regex, ranges
    }
}

// Configuration Example:
// switch:
//   source: "$header.Content-Type"
//   cases:
//     "application/json":
//       - type: "log"
//         message: "Processing JSON request"
//       - type: "transform"
//         targetType: "application/xml"
//     "application/xml":
//       - type: "log" 
//         message: "Processing XML request"
//     "text/*":  # Wildcard matching
//       - type: "log"
//         message: "Processing text request"
//     default:
//       - type: "respond"
//         status: 415
//         payload: '{"error": "Unsupported media type"}'
```

#### DataMapper Integration (Pluggable Architecture)
```java
// Location: mediators/src/main/java/org/wso2/graalvm/mediators/transform/DataMapperMediator.java
/**
 * AI Implementation Strategy:
 * 1. Pluggable architecture - DataMapper as separate JAR
 * 2. Runtime loading only when DataMapper mediator is used
 * 3. Hot-swappable DataMapper versions
 * 4. Support for multiple mapping engines (WSO2 DataMapper, external tools)
 * 5. Visual mapping metadata preservation
 * 6. Performance optimization for large dataset transformations
 */

public interface DataMapperEngine {
    TransformationResult transform(MessageContext context, MappingConfig config);
    ValidationResult validateMapping(MappingConfig config);
    boolean isAvailable(); // Check if engine is loaded and ready
}

@MediatorComponent
public class DataMapperRegistry {
    private final Map<String, DataMapperEngine> engines = new ConcurrentHashMap<>();
    private final PluginLoader pluginLoader;
    
    /**
     * AI Implementation Requirements:
     * 1. Dynamic JAR loading from plugins/ directory
     * 2. Version management and compatibility checking
     * 3. Hot-swapping without service restart
     * 4. Graceful fallback if DataMapper unavailable
     * 5. Configuration validation and error reporting
     */
    
    public void loadDataMapperPlugin(Path jarPath) {
        // AI Task: Implement safe plugin loading with class isolation
    }
    
    public DataMapperEngine getEngine(String engineType) {
        // AI Task: Return appropriate engine or throw descriptive error
    }
}

// Configuration Example:
// dataMapper:
//   engine: "wso2-datamapper"  # or "custom-mapper"
//   mapping: "user-transformation.dmc"
//   input:
//     schema: "input-schema.json"
//   output:
//     schema: "output-schema.xsd"
//   settings:
//     streaming: true
//     batchSize: 1000
```

### Priority 2: Message Context Architecture

#### Generic Message Interface ✅ IMPLEMENTED

The Message interface provides a unified abstraction for all message types with support for streaming, lazy evaluation, and memory-efficient processing:

```java
// Core Message interface with streaming and lazy evaluation
public interface Message {
    // Content type and metadata
    ContentType getContentType();
    MessageMetadata getMetadata();
    Charset getCharset();
    long getContentLength();
    
    // State management
    boolean isConsumed();
    boolean isEmpty();
    boolean isValid();
    
    // Raw content access
    InputStream getPayloadStream() throws MessageException;
    byte[] getPayloadBytes() throws MessageException;
    String getPayloadText() throws MessageException;
    String getPayloadText(Charset charset) throws MessageException;
    
    // Structured content access (lazy evaluation)
    StructuredContent getStructuredContent();
    StreamingContent getStreamingContent();
    
    // Validation and schema checking
    ValidationResult validate(Schema schema);
    
    // Message operations
    MessageBuilder toBuilder();
    Message copy() throws MessageException;
    CompletableFuture<Message> transformTo(ContentType targetContentType);
    void close();
}

// StructuredContent - Lazy access to structured data
public interface StructuredContent {
    JsonNode asJson() throws MessageException;
    Document asXml() throws MessageException;
    Map<String, Object> asMap() throws MessageException;
    List<Object> asList() throws MessageException;
    <T> T as(Class<T> type) throws MessageException;
    
    // Path-based access
    Optional<Object> get(String path) throws MessageException;
    <T> Optional<T> get(String path, Class<T> type) throws MessageException;
    boolean has(String path) throws MessageException;
    
    // Structure inspection
    boolean isArray() throws MessageException;
    boolean isObject() throws MessageException;
    int size() throws MessageException;
    boolean isEmpty() throws MessageException;
}

// StreamingContent - Memory-efficient processing
public interface StreamingContent {
    Stream<String> lines() throws MessageException;
    Stream<JsonNode> jsonArrayElements() throws MessageException;
    Stream<String> xmlElements(String elementName) throws MessageException;
    Stream<Map<String, String>> csvRows(boolean hasHeader) throws MessageException;
    Stream<ByteBuffer> bytes(int chunkSize) throws MessageException;
    <T> Iterator<T> stream(ElementParser<T> parser) throws MessageException;
}

// Message Builder with fluent API
public interface MessageBuilder {
    MessageBuilder withContentType(ContentType contentType);
    MessageBuilder withPayload(InputStream payload);
    MessageBuilder withPayload(String payload);
    MessageBuilder withHeader(String name, String value);
    MessageBuilder withProperty(String name, Object value);
    MessageBuilder withCorrelationId(String correlationId);
    MessageBuilder withPriority(int priority);
    MessageBuilder withLazyLoading(boolean lazy);
    
    Message build() throws MessageException;
    ValidationResult validate();
}

// Message Factory with auto-detection and optimization
@MediatorComponent
public class MessageFactory {
    public Message createFromString(String content, ContentType type);
    public Message createFromString(String content, ContentType type, Charset charset);
    public Message createFromStream(InputStream stream, ContentType type);
    public Message createFromFile(Path file);
    public Message createEmpty(ContentType type);
    public Message createFromHttpRequest(InputStream body, String contentType, Map<String, String> headers);
    
    public MessageBuilder builder();
}
```

**Implementation Status:**
- ✅ **DefaultMessage**: Complete implementation with streaming and caching
- ✅ **DefaultStructuredContent**: JSON/XML/Map lazy evaluation
- ✅ **DefaultStreamingContent**: Memory-efficient line/chunk/element streaming
- ✅ **DefaultMessageBuilder**: Fluent builder with validation
- ✅ **MessageFactory**: Auto-detection and multiple creation methods
- ✅ **MessageTransformerRegistry**: Pluggable content transformation
- ✅ **MessageMetadataFactory**: Comprehensive metadata management

**Key Features Implemented:**
- **Lazy Evaluation**: Content parsed only when accessed
- **Streaming Support**: Process large files without loading into memory
- **Content Type Detection**: Automatic MIME type identification
- **Transformation Pipeline**: Asynchronous content type conversion
- **Validation Framework**: Schema-based and structural validation
- **Resource Management**: Proper cleanup and memory management
```

#### Enhanced Message Context Structure
```java
// Location: core/src/main/java/org/wso2/graalvm/core/context/MessageContext.java
/**
 * AI Implementation Requirements:
 * 1. Thread-safe context for virtual thread environment
 * 2. Correlation ID tracking for distributed tracing
 * 3. Performance metrics collection per request
 * 4. Error handling and rollback capabilities
 * 5. Variable scoping (global, sequence, mediator)
 * 6. Audit trail for debugging and compliance
 */
public class MessageContext {
    // Core message and metadata
    private volatile Message message;
    private final String correlationId;
    private final String messageId;
    private final Instant createdAt;
    
    // Context variables with scoping
    private final VariableScope globalScope;
    private final VariableScope sequenceScope;
    private final VariableScope mediatorScope;
    
    // Execution context
    private final ExecutionMetrics metrics;
    private final AuditTrail auditTrail;
    private final ErrorHandler errorHandler;
    private final VirtualThreadExecutor executor;
    
    // Request/Response correlation
    private MessageContext parentContext; // For sub-requests
    private final List<MessageContext> childContexts;
    
    /**
     * AI Task: Implement validation per mediator requirements
     * Use: Schema validation, business rule validation
     * Return: Detailed validation errors for development feedback
     */
    public ValidationResult validate(MediatorSchema schema) {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // Validate message structure
        if (schema.requiresPayload() && message.getPayload() == null) {
            builder.addError("Payload is required but missing");
        }
        
        // Validate content type
        if (!schema.getSupportedContentTypes().contains(message.getContentType())) {
            builder.addError("Unsupported content type: " + message.getContentType());
        }
        
        // Validate required headers
        for (String requiredHeader : schema.getRequiredHeaders()) {
            if (!message.getHeaders().contains(requiredHeader)) {
                builder.addError("Required header missing: " + requiredHeader);
            }
        }
        
        // Validate payload schema
        if (schema.getPayloadSchema() != null) {
            ValidationResult payloadValidation = message.validate(schema.getPayloadSchema());
            builder.merge(payloadValidation);
        }
        
        return builder.build();
    }
    
    // Performance tracking
    public void recordMediatorExecution(String mediatorName, Duration duration) {
        metrics.recordMediatorExecution(mediatorName, duration);
        auditTrail.addEntry(AuditEntry.mediatorExecution(mediatorName, duration));
    }
    
    // Error handling with rollback
    public void handleError(Exception error, String mediatorName) {
        errorHandler.handleError(error, this, mediatorName);
        auditTrail.addEntry(AuditEntry.error(error, mediatorName));
    }
    
    // Variable management with scoping
    public void setVariable(String name, Object value, VariableScope scope) {
        getScope(scope).setVariable(name, value);
        auditTrail.addEntry(AuditEntry.variableSet(name, scope));
    }
    
    public Object getVariable(String name) {
        // Search in order: mediator -> sequence -> global
        return mediatorScope.getVariable(name)
            .orElseGet(() -> sequenceScope.getVariable(name)
                .orElseGet(() -> globalScope.getVariable(name).orElse(null)));
    }
}

// Variable Scope Implementation
public class VariableScope {
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final String scopeName;
    
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }
    
    public Optional<Object> getVariable(String name) {
        return Optional.ofNullable(variables.get(name));
    }
    
    public void clearScope() {
        variables.clear();
    }
}

// Execution Metrics Collection
public class ExecutionMetrics {
    private final Counter requestCount = Counter.builder("requests.total").register();
    private final Timer requestDuration = Timer.builder("request.duration").register();
    private final Map<String, Timer> mediatorTimers = new ConcurrentHashMap<>();
    
    public void recordMediatorExecution(String mediatorName, Duration duration) {
        mediatorTimers.computeIfAbsent(mediatorName, 
            name -> Timer.builder("mediator.duration")
                .tag("mediator", name)
                .register())
            .record(duration);
    }
}

// Audit Trail for Debugging
public class AuditTrail {
    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();
    
    public void addEntry(AuditEntry entry) {
        entries.add(entry);
    }
    
    public List<AuditEntry> getEntries() {
        return List.copyOf(entries);
    }
    
    public String toJson() {
        // AI Task: Serialize audit trail to JSON for debugging
        return JsonUtils.serialize(entries);
    }
}

public record AuditEntry(
    Instant timestamp,
    String type,
    String description,
    Map<String, Object> details
) {
    public static AuditEntry mediatorExecution(String mediatorName, Duration duration) {
        return new AuditEntry(
            Instant.now(),
            "MEDIATOR_EXECUTION",
            "Executed mediator: " + mediatorName,
            Map.of("mediator", mediatorName, "duration", duration.toMillis())
        );
    }
    
    public static AuditEntry error(Exception error, String mediatorName) {
        return new AuditEntry(
            Instant.now(),
            "ERROR",
            "Error in mediator: " + mediatorName,
            Map.of("mediator", mediatorName, "error", error.getMessage())
        );
    }
    
    public static AuditEntry variableSet(String variableName, VariableScope scope) {
        return new AuditEntry(
            Instant.now(),
            "VARIABLE_SET",
            "Set variable: " + variableName,
            Map.of("variable", variableName, "scope", scope.getScopeName())
        );
    }
}
```

#### Message Context Factory and Management
```java
// Location: core/src/main/java/org/wso2/graalvm/core/context/MessageContextFactory.java
@MediatorComponent
public class MessageContextFactory {
    private final CorrelationIdGenerator correlationIdGenerator;
    private final VirtualThreadExecutor defaultExecutor;
    private final MetricsRegistry metricsRegistry;
    
    /**
     * AI Implementation Requirements:
     * 1. Efficient context creation for high throughput
     * 2. Proper resource cleanup and memory management
     * 3. Context inheritance for sub-requests
     * 4. Integration with distributed tracing
     */
    
    public MessageContext createContext(Message message) {
        return MessageContext.builder()
            .message(message)
            .correlationId(correlationIdGenerator.generate())
            .messageId(UUID.randomUUID().toString())
            .createdAt(Instant.now())
            .executor(defaultExecutor)
            .metrics(metricsRegistry.createExecutionMetrics())
            .auditTrail(new AuditTrail())
            .build();
    }
    
    public MessageContext createChildContext(MessageContext parent, Message message) {
        MessageContext child = createContext(message);
        child.setParentContext(parent);
        parent.addChildContext(child);
        return child;
    }
}

// Configuration Example:
// messageContext:
//   correlation:
//     generator: "uuid"  # or "sequential", "timestamp"
//   metrics:
//     enabled: true
//     detailed: true  # Include mediator-level metrics
//   audit:
//     enabled: true
//     maxEntries: 1000
//   variables:
//     globalScope:
//       maxSize: 1000
//     sequenceScope:
//       maxSize: 500
//     mediatorScope:
//       maxSize: 100
```

### Priority 3: Transport Layer (Pluggable Architecture)

#### Enhanced HTTP Transport (Helidon-based)
```java
// Location: transports/src/main/java/org/wso2/graalvm/transports/http/HelidonHttpTransport.java
/**
 * ✅ Implemented: HelidonHttpServer with security integration
 * 
 * AI Enhancement Requirements:
 * 1. HTTP/2 support with stream multiplexing
 * 2. WebSocket endpoint implementation
 * 3. Server-Sent Events (SSE) support
 * 4. Streaming request/response handling
 * 5. Connection pooling for outbound calls
 * 6. HTTP/3 QUIC support (future)
 * 7. Compression (gzip, br) support
 * 8. Keep-alive and connection reuse optimization
 */

@MediatorComponent
public class EnhancedHelidonHttpTransport implements TransportListener {
    private final WebServer webServer;
    private final WebSocketManager webSocketManager;
    private final StreamingHandler streamingHandler;
    private final CompressionHandler compressionHandler;
    
    /**
     * AI Task: Add HTTP/2 support with push promise
     */
    public void configureHttp2Support(WebServerConfig.Builder builder) {
        builder.addProtocol(ProtocolConfig.builder()
            .name("h2")
            .maxConcurrentStreams(1000)
            .initialWindowSize(65535)
            .maxFrameSize(16384)
            .build());
    }
    
    /**
     * AI Task: Implement WebSocket endpoint support
     */
    public void configureWebSocketEndpoints(Routing.Builder routing) {
        routing.register("/ws", WebSocketRouting.builder()
            .endpoint("/events", webSocketManager::handleEventStream)
            .endpoint("/debug", webSocketManager::handleDebugSession)
            .build());
    }
    
    /**
     * AI Task: Add streaming request/response handling
     */
    public void handleStreamingRequest(ServerRequest request, ServerResponse response) {
        if (isStreamingRequest(request)) {
            streamingHandler.processStream(request.content(), response);
        }
    }
}

// WebSocket Manager for real-time communication
@MediatorComponent
public class WebSocketManager {
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final EventBroadcaster eventBroadcaster;
    
    /**
     * AI Implementation Requirements:
     * 1. Real-time event streaming to clients
     * 2. Debug session management
     * 3. Authentication and authorization for WebSocket connections
     * 4. Message routing based on subscription topics
     * 5. Connection lifecycle management
     * 6. Backpressure handling for slow clients
     */
    
    public void handleEventStream(WebSocketSession session) {
        // AI Task: Implement event subscription management
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, session);
        
        session.onMessage((ws, message, last) -> {
            handleSubscriptionMessage(sessionId, message);
        });
        
        session.onClose((ws, status, reason) -> {
            activeSessions.remove(sessionId);
        });
    }
    
    public void broadcast(String topic, Object event) {
        // AI Task: Broadcast to subscribed sessions only
        String eventJson = JsonUtils.serialize(event);
        activeSessions.values().forEach(session -> {
            if (session.isSubscribedTo(topic)) {
                session.send(eventJson, true);
            }
        });
    }
}

// Streaming Handler for large payloads
@MediatorComponent  
public class StreamingHandler {
    private final ChunkProcessor chunkProcessor;
    private final BackpressureManager backpressureManager;
    
    /**
     * AI Implementation Requirements:
     * 1. Chunked transfer encoding support
     * 2. Backpressure management for slow consumers
     * 3. Memory-efficient processing of large files
     * 4. Progress tracking and resumable uploads
     * 5. Content validation during streaming
     * 6. Error recovery and partial processing
     */
    
    public void processStream(Flow.Publisher<DataChunk> publisher, ServerResponse response) {
        // AI Task: Implement reactive streaming with backpressure
        publisher.subscribe(new Flow.Subscriber<DataChunk>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1); // Start with one chunk
            }
            
            @Override
            public void onNext(DataChunk chunk) {
                try {
                    ProcessingResult result = chunkProcessor.process(chunk);
                    response.send(result.getData());
                    
                    // Request next chunk based on backpressure
                    if (backpressureManager.canProcessMore()) {
                        subscription.request(1);
                    }
                } catch (Exception e) {
                    subscription.cancel();
                    response.status(500).send("Processing error: " + e.getMessage());
                }
            }
        });
    }
}
```

#### JMS Transport Implementation (Future Priority)
```java
// Location: transports/src/main/java/org/wso2/graalvm/transports/jms/JmsTransport.java
/**
 * AI Implementation Requirements:
 * 1. JMS 2.0 support with async message processing
 * 2. Multiple provider support (ActiveMQ, RabbitMQ, AWS SQS, Azure Service Bus)
 * 3. Connection pooling and session management
 * 4. Message acknowledgment strategies
 * 5. Dead letter queue handling
 * 6. Transactional message processing
 * 7. Message selectors and filtering
 * 8. Dynamic destination creation
 */

@MediatorComponent
public class JmsTransport implements TransportListener {
    private final ConnectionManager connectionManager;
    private final MessageListenerRegistry listenerRegistry;
    private final TransactionManager transactionManager;
    
    /**
     * AI Task: Implement JMS listener with virtual thread processing
     */
    public void startMessageListener(JmsListenerConfig config) {
        ConnectionFactory connectionFactory = createConnectionFactory(config);
        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(config.isTransacted(), 
            config.getAcknowledgeMode());
        
        Destination destination = createDestination(session, config);
        MessageConsumer consumer = session.createConsumer(destination, 
            config.getMessageSelector());
        
        consumer.setMessageListener(message -> {
            // Process in virtual thread
            CompletableFuture.runAsync(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    MessageContext context = createMessageContext(message);
                    MediationResult result = processMessage(context);
                    
                    if (result.isSuccess()) {
                        acknowledgeMessage(message, session);
                    } else {
                        handleMessageProcessingError(message, result.getError());
                    }
                } catch (Exception e) {
                    sendToDeadLetterQueue(message, e);
                }
            }, VirtualThread.ofVirtual().factory());
        });
        
        connection.start();
    }
    
    /**
     * AI Task: Implement message sending with connection pooling
     */
    public CompletableFuture<SendResult> sendMessage(JmsEndpoint endpoint, Message message) {
        return CompletableFuture.supplyAsync(() -> {
            try (PooledConnection connection = connectionManager.borrowConnection(endpoint)) {
                Session session = connection.createSession();
                Destination destination = createDestination(session, endpoint);
                MessageProducer producer = session.createProducer(destination);
                
                javax.jms.Message jmsMessage = convertToJmsMessage(message, session);
                producer.send(jmsMessage);
                
                return SendResult.success(jmsMessage.getJMSMessageID());
            } catch (Exception e) {
                return SendResult.failure(e);
            }
        }, VirtualThread.ofVirtual().factory());
    }
}

// JMS Configuration
public class JmsListenerConfig {
    private String connectionFactoryJndi;
    private String destinationName;
    private DestinationType destinationType; // QUEUE, TOPIC
    private boolean transacted;
    private int acknowledgeMode;
    private String messageSelector;
    private int concurrentConsumers;
    private Duration receiveTimeout;
    private String deadLetterQueue;
    
    // Connection pooling
    private int minConnections = 1;
    private int maxConnections = 10;
    private Duration maxIdleTime = Duration.ofMinutes(5);
}

// Configuration Example:
// jms:
//   providers:
//     - name: "activemq"
//       connectionFactory: "tcp://localhost:61616"
//       username: "admin"
//       password: "admin"
//       poolConfig:
//         minConnections: 2
//         maxConnections: 20
//   listeners:
//     - name: "order-queue"
//       destination: "order.queue"
//       type: "QUEUE"
//       transacted: true
//       concurrentConsumers: 5
//       messageSelector: "orderType = 'premium'"
//       sequence: "process-order"
```

#### Pluggable Transport Registry
```java
// Location: transports/src/main/java/org/wso2/graalvm/transports/TransportRegistry.java
@MediatorComponent
public class TransportRegistry {
    private final Map<String, TransportListener> listeners = new ConcurrentHashMap<>();
    private final Map<String, TransportSender> senders = new ConcurrentHashMap<>();
    private final PluginLoader pluginLoader;
    
    /**
     * AI Implementation Requirements:
     * 1. Dynamic transport loading from plugins
     * 2. Hot-swappable transport implementations  
     * 3. Transport capability discovery
     * 4. Protocol negotiation and fallback
     * 5. Health checking for transport endpoints
     * 6. Graceful transport lifecycle management
     */
    
    public void registerTransport(String protocol, TransportListener listener) {
        listeners.put(protocol, listener);
        
        // AI Task: Add capability registration
        TransportCapabilities capabilities = listener.getCapabilities();
        registerCapabilities(protocol, capabilities);
    }
    
    public CompletableFuture<Void> startAllTransports() {
        // AI Task: Start transports in dependency order
        List<CompletableFuture<Void>> startTasks = listeners.values().stream()
            .map(listener -> CompletableFuture.runAsync(() -> {
                try {
                    listener.start();
                } catch (Exception e) {
                    throw new TransportStartupException("Failed to start transport", e);
                }
            }, VirtualThread.ofVirtual().factory()))
            .toList();
            
        return CompletableFuture.allOf(startTasks.toArray(new CompletableFuture[0]));
    }
    
    /**
     * AI Task: Implement transport plugin loading
     */
    public void loadTransportPlugin(Path jarPath) {
        try {
            PluginInfo pluginInfo = pluginLoader.loadPlugin(jarPath);
            TransportListener listener = pluginInfo.getTransportListener();
            
            if (listener != null) {
                registerTransport(pluginInfo.getProtocol(), listener);
            }
        } catch (Exception e) {
            throw new PluginLoadException("Failed to load transport plugin", e);
        }
    }
}

// Transport Capabilities Discovery
public class TransportCapabilities {
    private final Set<String> supportedProtocols;
    private final Set<ContentType> supportedContentTypes;
    private final boolean supportsStreaming;
    private final boolean supportsTransactions;
    private final boolean supportsBidirectional;
    private final int maxConcurrentConnections;
    
    // AI Task: Implement capability-based routing
    public boolean canHandle(TransportRequest request) {
        return supportedProtocols.contains(request.getProtocol()) &&
               supportedContentTypes.contains(request.getContentType()) &&
               (!request.requiresStreaming() || supportsStreaming);
    }
}

// Transport Health Monitoring
@MediatorComponent
public class TransportHealthMonitor {
    private final Map<String, TransportHealth> healthStatus = new ConcurrentHashMap<>();
    private final ScheduledExecutorService healthChecker;
    
    /**
     * AI Implementation Requirements:
     * 1. Periodic health checks for all transports
     * 2. Circuit breaker integration
     * 3. Automatic failover and recovery
     * 4. Health metrics collection and alerting
     */
    
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void checkTransportHealth() {
        listeners.values().forEach(listener -> {
            CompletableFuture.runAsync(() -> {
                try {
                    HealthCheckResult result = listener.checkHealth();
                    updateHealthStatus(listener.getProtocol(), result);
                } catch (Exception e) {
                    markTransportUnhealthy(listener.getProtocol(), e);
                }
            }, VirtualThread.ofVirtual().factory());
        });
    }
}
```

#### Inbound Endpoints Framework (Future Implementation)
```java
// Location: transports/src/main/java/org/wso2/graalvm/transports/inbound/InboundEndpointManager.java
/**
 * AI Implementation Requirements:
 * 1. File system polling (new files, modifications)
 * 2. Database polling (new records, changes)
 * 3. Message queue consumption (JMS, AMQP, Kafka)
 * 4. FTP/SFTP monitoring
 * 5. Email inbox monitoring (IMAP, POP3)
 * 6. WebHook handling
 * 7. Scheduled triggers (cron-like expressions)
 * 8. Event-driven processing with backpressure
 */

public interface InboundEndpoint {
    void initialize(InboundConfig config);
    void startListening();
    void stopListening();
    boolean isRunning();
    InboundCapabilities getCapabilities();
    HealthCheckResult checkHealth();
}

@MediatorComponent
public class InboundEndpointManager {
    private final Map<String, InboundEndpoint> endpoints = new ConcurrentHashMap<>();
    private final EventDispatcher eventDispatcher;
    
    /**
     * AI Task: Implement pluggable inbound endpoint architecture
     */
    public void registerEndpoint(String name, InboundEndpoint endpoint) {
        endpoints.put(name, endpoint);
        
        // Configure event handling
        endpoint.setEventHandler(event -> {
            MessageContext context = createMessageContextFromEvent(event);
            eventDispatcher.dispatch(context, endpoint.getTargetSequence());
        });
    }
    
    /**
     * AI Task: File system watcher implementation
     */
    public InboundEndpoint createFileSystemEndpoint(FileSystemConfig config) {
        return new FileSystemInboundEndpoint(config) {
            @Override
            protected void onFileDetected(Path file) {
                try {
                    Message message = messageFactory.createFromFile(file);
                    MessageContext context = contextFactory.createContext(message);
                    processInboundMessage(context);
                } catch (Exception e) {
                    handleInboundError(file, e);
                }
            }
        };
    }
}

// Configuration Example:
// inboundEndpoints:
//   - name: "order-files"
//     type: "filesystem"
//     config:
//       directory: "/data/orders"
//       pattern: "*.xml"
//       pollInterval: "5s"
//       deleteAfterProcess: true
//     sequence: "process-order-file"
//     
//   - name: "customer-db"
//     type: "database"
//     config:
//       dataSource: "customerDB"
//       query: "SELECT * FROM customers WHERE processed = false"
//       pollInterval: "30s"
//       updateQuery: "UPDATE customers SET processed = true WHERE id = ?"
//     sequence: "sync-customer"
```

### Priority 4: Deployment and Configuration

#### Sequence/Template Merger
```java
// Location: integration-engine/src/main/java/org/wso2/graalvm/engine/sequence/MediationSequence.java
/**
 * AI Implementation Requirements:
 * 1. Merge sequences and templates into unified construct
 * 2. Template parameter substitution with validation
 * 3. Dynamic sequence modification at runtime
 * 4. Sequence versioning and rollback capabilities
 * 5. Performance optimization (sequence compilation)
 * 6. Hot deployment without service restart
 * 7. Dependency resolution between sequences
 * 8. Error handling and graceful degradation
 */
public class MediationSequence {
    private final String name;
    private final String version;
    private final List<Mediator> mediators;
    private final ParameterMap templateParameters;
    private final ErrorHandler errorHandler;
    private final DependencySet dependencies;
    private final SequenceMetadata metadata;
    
    /**
     * AI Task: Implement template parameter substitution
     */
    public MediationSequence instantiate(Map<String, Object> parameters) {
        ParameterValidator.validate(parameters, templateParameters);
        
        List<Mediator> instantiatedMediators = mediators.stream()
            .map(mediator -> mediator.substitute(parameters))
            .toList();
            
        return new MediationSequence(
            name + "-" + UUID.randomUUID().toString(),
            version,
            instantiatedMediators,
            templateParameters,
            errorHandler,
            dependencies,
            metadata
        );
    }
    
    /**
     * AI Task: Implement sequence execution with error handling
     */
    public CompletableFuture<MediationResult> execute(MessageContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                SequenceExecutionContext execContext = new SequenceExecutionContext(this, context);
                
                for (Mediator mediator : mediators) {
                    MediationResult result = mediator.mediate(context).join();
                    
                    if (result.isFailure()) {
                        return errorHandler.handleMediatorError(result, mediator, execContext);
                    }
                    
                    // Check for early termination
                    if (result.shouldTerminate()) {
                        return result;
                    }
                    
                    // Update execution metrics
                    execContext.recordMediatorExecution(mediator, result);
                }
                
                return MediationResult.success();
            } catch (Exception e) {
                return errorHandler.handleSequenceError(e, this, context);
            }
        }, context.getExecutor());
    }
}

// Sequence Builder with Fluent API
public class SequenceBuilder {
    /**
     * AI Implementation Requirements:
     * 1. Fluent API for programmatic sequence construction
     * 2. Validation during build process
     * 3. Integration with YAML configuration
     * 4. IDE support with auto-completion
     */
    
    public static SequenceBuilder newSequence(String name) {
        return new SequenceBuilder(name);
    }
    
    public SequenceBuilder withParameter(String name, Class<?> type, boolean required) {
        templateParameters.addParameter(new ParameterDefinition(name, type, required));
        return this;
    }
    
    public SequenceBuilder addMediator(Mediator mediator) {
        mediators.add(mediator);
        return this;
    }
    
    public SequenceBuilder addLog(String message) {
        return addMediator(new LogMediator(message));
    }
    
    public SequenceBuilder addTransform(ContentType from, ContentType to, String script) {
        return addMediator(new TransformMediator(from, to, script));
    }
    
    public SequenceBuilder addSend(String endpoint) {
        return addMediator(new SendMediator(EndpointReference.of(endpoint)));
    }
    
    public SequenceBuilder onError(ErrorHandler handler) {
        this.errorHandler = handler;
        return this;
    }
    
    public MediationSequence build() throws SequenceValidationException {
        validate();
        return new MediationSequence(name, version, mediators, templateParameters, 
                                   errorHandler, dependencies, metadata);
    }
}

// Dynamic Sequence Registry
@MediatorComponent
public class SequenceRegistry {
    private final Map<String, MediationSequence> sequences = new ConcurrentHashMap<>();
    private final Map<String, List<String>> sequenceVersions = new ConcurrentHashMap<>();
    private final SequenceValidator validator;
    private final DependencyResolver dependencyResolver;
    
    /**
     * AI Implementation Requirements:
     * 1. Hot deployment of sequences
     * 2. Version management and rollback
     * 3. Dependency resolution and validation
     * 4. Graceful sequence replacement
     * 5. Performance monitoring per sequence
     */
    
    public void deploySequence(SequenceDefinition definition) throws DeploymentException {
        // Validate sequence definition
        ValidationResult validation = validator.validate(definition);
        if (!validation.isValid()) {
            throw new DeploymentException("Invalid sequence: " + validation.getErrors());
        }
        
        // Resolve dependencies
        DependencyResolutionResult dependencies = dependencyResolver.resolve(definition);
        if (!dependencies.isResolved()) {
            throw new DeploymentException("Unresolved dependencies: " + dependencies.getMissing());
        }
        
        // Build sequence
        MediationSequence sequence = buildSequence(definition);
        
        // Deploy with versioning
        String sequenceName = definition.getName();
        String version = generateVersion();
        
        sequences.put(sequenceName + ":" + version, sequence);
        sequenceVersions.computeIfAbsent(sequenceName, k -> new ArrayList<>()).add(version);
        
        // Update current version pointer
        sequences.put(sequenceName, sequence);
        
        // Notify deployment listeners
        notifySequenceDeployed(sequenceName, version);
    }
    
    public void rollbackSequence(String sequenceName, String targetVersion) {
        String versionedKey = sequenceName + ":" + targetVersion;
        MediationSequence targetSequence = sequences.get(versionedKey);
        
        if (targetSequence == null) {
            throw new IllegalArgumentException("Version not found: " + targetVersion);
        }
        
        sequences.put(sequenceName, targetSequence);
        notifySequenceRolledBack(sequenceName, targetVersion);
    }
}
```

#### YAML Configuration Schema and Validation
```yaml
# Location: runtime/src/main/resources/schema/sequence-schema.yaml
# AI Task: Complete YAML schema definition with validation rules

# Sequence Definition Schema
sequences:
  - name: "user-registration"                    # Required: unique sequence name
    version: "1.0.0"                            # Optional: semantic versioning
    description: "User registration processing"  # Optional: documentation
    
    # Template parameters for reusability
    parameters:
      - name: "endpoint"
        type: "string"
        required: true
        default: "https://api.example.com"
        validation:
          pattern: "^https?://.*"
          description: "Must be valid HTTP/HTTPS URL"
      - name: "timeout"
        type: "duration"
        required: false
        default: "30s"
        validation:
          min: "1s"
          max: "300s"
      - name: "retries"
        type: "integer"
        required: false
        default: 3
        validation:
          min: 0
          max: 10
    
    # Dependencies on other sequences or external resources
    dependencies:
      sequences:
        - "validate-user"
        - "send-notification"
      endpoints:
        - "${endpoint}/users"
      
    # Mediator pipeline definition
    mediators:
      - type: "log"
        level: "INFO"
        message: "Processing user registration for ${header.userId}"
        
      - type: "validate"
        schema: "user-registration-schema.json"
        onFailure:
          - type: "respond"
            status: 400
            payload: '{"error": "Invalid user data", "details": "${validation.errors}"}'
            
      - type: "transform"
        sourceType: "application/json"
        targetType: "application/xml"
        script: "jsonToXml.xslt"
        settings:
          preserveNamespaces: true
          indentOutput: true
          
      - type: "property"
        action: "set"
        name: "processed.timestamp"
        value: "${now()}"
        scope: "global"
        
      - type: "send"
        endpoint: "${endpoint}/users"
        method: "POST"
        headers:
          Content-Type: "application/xml"
          Authorization: "Bearer ${jwt.token}"
        timeout: "${timeout}"
        retries: "${retries}"
        onSuccess:
          - type: "log"
            message: "User created successfully: ${response.headers.Location}"
        onFailure:
          - type: "log"
            level: "ERROR"
            message: "Failed to create user: ${error.message}"
          - type: "send"
            endpoint: "${deadLetterQueue}"
            
    # Error handling configuration
    errorHandling:
      strategy: "propagate"  # or "handle", "ignore"
      timeout: "60s"
      onError:
        - type: "log"
          level: "ERROR"
          message: "Sequence failed: ${error.message}"
        - type: "property"
          action: "set"
          name: "error.sequence"
          value: "${sequence.name}"

# Inbound Endpoints Configuration
inboundEndpoints:
  - name: "user-api"
    type: "http"
    config:
      protocol: "https"
      port: 8253
      context: "/api/users"
      methods: ["POST", "PUT"]
      security:
        authentication: true
        authorization:
          roles: ["user-admin", "api-user"]
    sequence: "user-registration"
    parameters:
      endpoint: "https://backend.example.com"
      timeout: "45s"
      
  - name: "order-files"
    type: "filesystem"
    config:
      directory: "/data/orders"
      pattern: "order-*.xml"
      pollInterval: "10s"
      deleteAfterProcess: true
      backup:
        enabled: true
        directory: "/data/processed"
    sequence: "process-order-file"
    
  - name: "integration-events"
    type: "jms"
    config:
      connectionFactory: "tcp://localhost:61616"
      destination: "integration.events"
      destinationType: "QUEUE"
      concurrentConsumers: 5
      transacted: true
    sequence: "handle-integration-event"

# Global Configuration
global:
  properties:
    - name: "environment"
      value: "production"
    - name: "region"
      value: "${env:AWS_REGION}"
    - name: "jwt.secret"
      value: "${vault:secret/jwt/signing-key}"
      
  endpoints:
    default:
      timeout: "30s"
      retries: 3
      circuitBreaker:
        enabled: true
        failureThreshold: 5
        recoveryTimeout: "60s"
```

## 📊 Performance Achievements

### Native Image Results
- **Binary Size**: 59.79MB (optimized)
- **Startup Time**: 34ms (vs. 3-5 seconds traditional MI)
- **Memory Usage**: ~50MB base (vs. 200-500MB traditional MI)
- **Code Optimization**: 44.82% code area, 53.78% image heap

### Framework Migration Results
- **✅ Netty → Helidon**: Complete HTTP server migration
- **✅ OSGi Removal**: Simplified dependency management
- **✅ Virtual Threads**: Enabled for high concurrency
- **✅ YAML Configuration**: Replaced XML-based config

## 🚧 Current Status & Roadmap

### ✅ Completed (Phase 1)
- [x] Core architecture and module structure
- [x] Helidon HTTP server with security integration
- [x] Basic mediators (Log, Property, Respond)
- [x] GraalVM native image compilation
- [x] Management API with health checks and metrics
- [x] YAML-based configuration system
- [x] Virtual thread integration
- [x] **Custom Dependency Injection System** (replaces Spring Framework)
- [x] **Generic Message Interface with streaming support**
- [x] **Lazy evaluation and memory-efficient processing**
- [x] **Message transformation pipeline**
- [x] **Comprehensive unit tests for message system**

### 🚧 In Progress (Phase 2)
- [x] ✅ **Message Interface System**: Complete with streaming, lazy evaluation, validation
- [ ] Transform mediator with content-type conversion (base implementation available)
- [ ] Send mediator with connection pooling
- [ ] Filter and Switch mediators
- [ ] Sequence/Template merger
- [ ] Wire logging implementation
- [ ] Enhanced security (Vault, JWT)

### 📋 Planned (Phase 3)
- [ ] JMS transport implementation
- [ ] DataMapper integration (pluggable)
- [ ] Inbound endpoints framework
- [ ] Database connectors
- [ ] Advanced observability (tracing)
- [ ] Migration tools from traditional MI

### 🔮 Future Considerations
- [ ] Proxy services (if customer demand)
- [ ] Data services (if customer demand)  
- [ ] Advanced coordination features
- [ ] Service catalog integration
- [ ] Advanced debugging tools

## 🤝 AI Development Guidelines and Templates

### Code Generation Patterns

#### 1. Mediator Implementation Template
```java
// Template Location: mediators/src/main/java/org/wso2/graalvm/mediators/builtin/[MediatorName].java
package org.wso2.graalvm.mediators.builtin;

import org.wso2.graalvm.core.mediator.Mediator;
import org.wso2.graalvm.core.context.MessageContext;
import org.wso2.graalvm.core.result.MediationResult;
import org.wso2.graalvm.core.annotation.MediatorComponent;
import java.util.concurrent.CompletableFuture;

/**
 * [MediatorName] - [Brief description of what this mediator does]
 * 
 * AI Implementation Checklist:
 * ✅ Implements Mediator interface
 * ✅ Annotated with @MediatorComponent for auto-discovery
 * ✅ Async processing with CompletableFuture
 * ✅ Proper error handling and logging
 * ✅ Configuration validation
 * ✅ Unit tests with edge cases
 * ✅ Performance metrics integration
 * ✅ Documentation with examples
 * 
 * Configuration Example:
 * ```yaml
 * - type: "[mediator-type]"
 *   [configuration-properties]
 * ```
 * 
 * Usage Example:
 * ```java
 * [MediatorName] mediator = new [MediatorName]([configuration]);
 * MediationResult result = mediator.mediate(context).join();
 * ```
 */
@MediatorComponent
public class [MediatorName] implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger([MediatorName].class);
    private final [Dependencies] [dependencyName];
    
    // Configuration properties
    private final [ConfigType] config;
    
    public [MediatorName]([Dependencies] [dependencyName], [ConfigType] config) {
        this.[dependencyName] = [dependencyName];
        this.config = config;
        validateConfiguration(config);
    }
    
    @Override
    public CompletableFuture<MediationResult> mediate(MessageContext context) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            try {
                // Validate input context
                ValidationResult validation = validateContext(context);
                if (!validation.isValid()) {
                    return MediationResult.failure(new ValidationException(validation.getErrors()));
                }
                
                // Record audit entry
                context.getAuditTrail().addEntry(
                    AuditEntry.mediatorStarted(getClass().getSimpleName())
                );
                
                // Perform mediator-specific logic
                [MediationType]Result result = performMediation(context);
                
                // Update context with results
                updateContext(context, result);
                
                // Record success metrics
                long duration = System.nanoTime() - startTime;
                recordMetrics(context, Duration.ofNanos(duration), true);
                
                logger.debug("Successfully executed {} for correlation: {}", 
                    getClass().getSimpleName(), context.getCorrelationId());
                    
                return MediationResult.success();
                
            } catch (Exception e) {
                // Record failure metrics
                long duration = System.nanoTime() - startTime;
                recordMetrics(context, Duration.ofNanos(duration), false);
                
                logger.error("Failed to execute {} for correlation: {}", 
                    getClass().getSimpleName(), context.getCorrelationId(), e);
                    
                context.getAuditTrail().addEntry(
                    AuditEntry.mediatorFailed(getClass().getSimpleName(), e)
                );
                
                return MediationResult.failure(e);
            }
        }, context.getExecutor());
    }
    
    /**
     * AI Implementation: Add mediator-specific validation logic
     */
    private ValidationResult validateContext(MessageContext context) {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // Example validations - customize per mediator
        if (config.requiresPayload() && context.getMessage().getPayload() == null) {
            builder.addError("Payload is required but missing");
        }
        
        // Add more validations as needed
        
        return builder.build();
    }
    
    /**
     * AI Implementation: Core mediator logic goes here
     */
    private [MediationType]Result performMediation(MessageContext context) {
        // Implement mediator-specific logic
        // This is where the main work happens
        
        // Example pattern:
        // 1. Extract required data from context
        // 2. Perform transformation/operation
        // 3. Return result
        
        throw new UnsupportedOperationException("AI Implementation Required");
    }
    
    /**
     * AI Implementation: Update context with mediator results
     */
    private void updateContext(MessageContext context, [MediationType]Result result) {
        // Update message, properties, or attributes based on mediator result
        // Example:
        // context.setMessage(result.getTransformedMessage());
        // context.setProperty("mediator.result", result.getValue());
    }
    
    /**
     * AI Implementation: Record performance metrics
     */
    private void recordMetrics(MessageContext context, Duration duration, boolean success) {
        // Record metrics using the metrics service
        MetricsService.getInstance().recordMediatorExecution(
            getClass().getSimpleName(),
            context.getSequenceName(),
            duration,
            success ? MediationResult.success() : MediationResult.failure(null)
        );
    }
    
    /**
     * AI Implementation: Validate mediator configuration
     */
    private void validateConfiguration([ConfigType] config) {
        // Add configuration validation logic
        // Throw ConfigurationException if invalid
        
        if (config == null) {
            throw new ConfigurationException("Configuration cannot be null");
        }
        
        // Add more validations as needed
    }
    
    @Override
    public String getType() {
        return "[mediator-type]";
    }
    
    @Override
    public MediatorCapabilities getCapabilities() {
        return MediatorCapabilities.builder()
            .supportsAsync(true)
            .requiresPayload(config.requiresPayload())
            .supportedContentTypes(config.getSupportedContentTypes())
            .build();
    }
}
```

#### 2. Transport Implementation Template
```java
// Template Location: transports/src/main/java/org/wso2/graalvm/transports/[protocol]/[Protocol]Transport.java
package org.wso2.graalvm.transports.[protocol];

import org.wso2.graalvm.transports.TransportListener;
import org.wso2.graalvm.core.annotation.MediatorComponent;

/**
 * [Protocol] Transport Implementation
 * 
 * AI Implementation Checklist:
 * ✅ Implements TransportListener interface
 * ✅ Proper resource management (connections, threads)
 * ✅ Error handling and recovery
 * ✅ Health checking capabilities
 * ✅ Metrics and monitoring integration
 * ✅ Configuration validation
 * ✅ Graceful startup and shutdown
 * ✅ Support for virtual threads
 */
@MediatorComponent
public class [Protocol]Transport implements TransportListener {
    
    private static final Logger logger = LoggerFactory.getLogger([Protocol]Transport.class);
    private final [Protocol]Config config;
    private final MessageContextFactory contextFactory;
    private final SequenceRegistry sequenceRegistry;
    
    private volatile boolean running = false;
    private [ConnectionType] connection;
    
    @Override
    public void start(TransportConfig config) throws TransportException {
        try {
            validate[Protocol]Config(config);
            
            // Initialize transport-specific resources
            initializeTransport(config);
            
            // Start listening for messages
            startListening();
            
            running = true;
            logger.info("Started {} transport on {}", getProtocol(), getEndpoint());
            
        } catch (Exception e) {
            throw new TransportStartupException("Failed to start " + getProtocol() + " transport", e);
        }
    }
    
    @Override
    public void stop() throws TransportException {
        if (!running) {
            return;
        }
        
        try {
            stopListening();
            cleanup();
            running = false;
            logger.info("Stopped {} transport", getProtocol());
            
        } catch (Exception e) {
            throw new TransportException("Failed to stop " + getProtocol() + " transport", e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public String getProtocol() {
        return "[protocol]";
    }
    
    @Override
    public TransportCapabilities getCapabilities() {
        return TransportCapabilities.builder()
            .supportedProtocols(Set.of("[protocol]"))
            .supportsStreaming([supportsStreaming])
            .supportsTransactions([supportsTransactions])
            .supportsBidirectional([supportsBidirectional])
            .maxConcurrentConnections(config.getMaxConnections())
            .build();
    }
    
    /**
     * AI Implementation: Protocol-specific initialization
     */
    private void initializeTransport([Protocol]Config config) {
        // Initialize protocol-specific resources
        // Examples: connection pools, thread pools, buffers
    }
    
    /**
     * AI Implementation: Start message listening
     */
    private void startListening() {
        // Start listening for incoming messages
        // Use virtual threads for handling requests
        
        CompletableFuture.runAsync(() -> {
            while (running) {
                try {
                    [MessageType] incomingMessage = receiveMessage();
                    
                    // Process in virtual thread
                    CompletableFuture.runAsync(() -> {
                        processIncomingMessage(incomingMessage);
                    }, VirtualThread.ofVirtual().factory());
                    
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error receiving message", e);
                    }
                }
            }
        }, VirtualThread.ofVirtual().factory());
    }
    
    /**
     * AI Implementation: Process incoming messages
     */
    private void processIncomingMessage([MessageType] incomingMessage) {
        try {
            // Convert transport message to internal message
            Message message = convertToInternalMessage(incomingMessage);
            MessageContext context = contextFactory.createContext(message);
            
            // Find target sequence
            String sequenceName = determineTargetSequence(incomingMessage);
            MediationSequence sequence = sequenceRegistry.getSequence(sequenceName);
            
            if (sequence == null) {
                handleUnknownSequence(sequenceName, context);
                return;
            }
            
            // Execute sequence
            sequence.execute(context)
                .thenAccept(result -> handleSequenceResult(result, incomingMessage))
                .exceptionally(error -> {
                    handleSequenceError(error, incomingMessage);
                    return null;
                });
                
        } catch (Exception e) {
            logger.error("Failed to process incoming message", e);
        }
    }
}
```

#### 3. Configuration Class Template
```java
// Template Location: [module]/src/main/java/org/wso2/graalvm/[module]/config/[Feature]Config.java
package org.wso2.graalvm.[module].config;

import org.wso2.graalvm.core.annotation.ConfigProperties;
import org.wso2.graalvm.core.annotation.ValidatedConfig;
import javax.validation.constraints.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for [Feature]
 * 
 * AI Implementation Checklist:
 * ✅ Annotated with @ConfigProperties
 * ✅ Bean validation annotations
 * ✅ Default values for all properties
 * ✅ Documentation for each property
 * ✅ Type-safe configuration with proper types
 * ✅ Nested configuration objects where appropriate
 */
@ConfigProperties(prefix = "[feature]")
@ValidatedConfig
public class [Feature]Config {
    
    /**
     * Enable/disable [feature] functionality
     */
    private boolean enabled = true;
    
    /**
     * [Feature] timeout duration
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(30);
    
    /**
     * Maximum number of [items]
     */
    @Positive
    private int maxItems = 100;
    
    /**
     * [Feature] endpoint URL
     */
    @NotBlank
    private String endpoint = "http://localhost:8080";
    
    /**
     * List of allowed [items]
     */
    @NotEmpty
    private List<String> allowedItems = List.of("default");
    
    /**
     * [Feature] settings
     */
    private [Feature]Settings settings = new [Feature]Settings();
    
    // Getters and setters with validation
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @NotNull
    public Duration getTimeout() {
        return timeout;
    }
    
    public void setTimeout(@NotNull Duration timeout) {
        this.timeout = timeout;
    }
    
    // ... other getters and setters
    
    /**
     * Nested configuration class
     */
    public static class [Feature]Settings {
        private boolean detailedLogging = false;
        private int retryAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(1);
        
        // Getters and setters...
    }
    
    /**
     * Configuration validation method
     */
    public void validate() throws ConfigurationException {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new ConfigurationException("Timeout must be positive");
        }
        
        if (maxItems <= 0) {
            throw new ConfigurationException("Max items must be positive");
        }
        
        // Add more validation as needed
    }
}
```

#### 4. Test Class Template
```java
// Template Location: [module]/src/test/java/org/wso2/graalvm/[module]/[Class]Test.java
package org.wso2.graalvm.[module];

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wso2.graalvm.testing.annotation.IntegrationTest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for [Class]
 * 
 * AI Implementation Checklist:
 * ✅ Test happy path scenarios
 * ✅ Test error conditions and edge cases
 * ✅ Test configuration validation
 * ✅ Test async behavior with CompletableFuture
 * ✅ Mock external dependencies
 * ✅ Use parameterized tests for multiple scenarios
 * ✅ Test resource cleanup and lifecycle
 * ✅ Performance tests for critical paths
 */
@ExtendWith(MockitoExtension.class)
@IntegrationTest
class [Class]Test {
    
    @Mock
    private [Dependency] mockDependency;
    
    private [Class] [instanceName];
    private [Config] config;
    
    @BeforeEach
    void setUp() {
        config = create[Config]();
        [instanceName] = new [Class](mockDependency, config);
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup resources if needed
    }
    
    @Test
    @DisplayName("Should [expected behavior] when [condition]")
    void should[ExpectedBehavior]When[Condition]() {
        // Given
        [InputType] input = create[InputType]();
        when(mockDependency.[method]()).thenReturn([expected]);
        
        // When
        [ResultType] result = [instanceName].[methodUnderTest](input);
        
        // Then
        assertNotNull(result);
        assertEquals([expected], result.[property]());
        verify(mockDependency).[method]();
    }
    
    @Test
    @DisplayName("Should throw exception when [error condition]")
    void shouldThrowExceptionWhen[ErrorCondition]() {
        // Given
        [InputType] input = create[InvalidInputType]();
        
        // When & Then
        assertThrows([ExceptionType].class, () -> {
            [instanceName].[methodUnderTest](input);
        });
    }
    
    @ParameterizedTest
    @ValueSource([type] = {[values]})
    @DisplayName("Should handle multiple [scenario] values")
    void shouldHandleMultiple[Scenario]Values([type] value) {
        // Test with multiple values
        [ResultType] result = [instanceName].[method](value);
        assertTrue(result.[condition]());
    }
    
    @Test
    @DisplayName("Should handle async processing correctly")
    void shouldHandleAsyncProcessingCorrectly() {
        // Given
        MessageContext context = createMessageContext();
        
        // When
        CompletableFuture<MediationResult> future = [instanceName].mediate(context);
        
        // Then
        assertDoesNotThrow(() -> {
            MediationResult result = future.get(5, TimeUnit.SECONDS);
            assertTrue(result.isSuccess());
        });
    }
    
    // Helper methods for test data creation
    
    private [Config] create[Config]() {
        [Config] config = new [Config]();
        // Set test configuration
        return config;
    }
    
    private MessageContext createMessageContext() {
        return MessageContext.builder()
            .message(createTestMessage())
            .correlationId("test-correlation-id")
            .build();
    }
    
    private Message createTestMessage() {
        return MessageFactory.createFromString(
            "test payload", 
            ContentType.APPLICATION_JSON
        );
    }
}
```

### Performance Optimization Guidelines

#### 1. Virtual Thread Optimization
```java
// Use virtual threads for I/O bound operations
CompletableFuture.supplyAsync(() -> {
    // I/O intensive work
    return performNetworkCall();
}, VirtualThread.ofVirtual().factory());

// For CPU intensive work, use regular thread pool
CompletableFuture.supplyAsync(() -> {
    // CPU intensive work
    return performComplexCalculation();
}, ForkJoinPool.commonPool());
```

#### 2. Memory Management
```java
// Prefer streaming for large payloads
public Stream<String> processLargeFile(Path file) {
    return Files.lines(file)
        .filter(line -> !line.isEmpty())
        .map(this::processLine);
}

// Use try-with-resources for proper cleanup
try (InputStream input = message.getPayload()) {
    return processStream(input);
}
```

#### 3. Caching Strategies
```java
// Use caffeine cache for performance-critical operations
@Cacheable(value = "sequences", key = "#name")
public MediationSequence getSequence(String name) {
    return loadSequenceFromStorage(name);
}

// Implement cache warming for frequently used data
@PostConstruct
public void warmupCache() {
    frequentlyUsedSequences.forEach(this::getSequence);
}
```

### Error Handling Patterns

#### 1. Structured Error Handling
```java
public class MediationResult {
    public static MediationResult failure(String code, String message, Throwable cause) {
        return new MediationResult(false, code, message, cause);
    }
    
    public static MediationResult failure(ErrorCode errorCode, Object... args) {
        String message = MessageFormat.format(errorCode.getTemplate(), args);
        return failure(errorCode.getCode(), message, null);
    }
}

// Usage
if (invalidCondition) {
    return MediationResult.failure(ErrorCodes.INVALID_PAYLOAD, 
        "Expected JSON but got " + actualType);
}
```

#### 2. Circuit Breaker Pattern
```java
@MediatorComponent
public class CircuitBreakerManager {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    
    public <T> T execute(String name, Supplier<T> operation) {
        CircuitBreaker breaker = breakers.computeIfAbsent(name, 
            k -> CircuitBreaker.ofDefaults(k));
            
        return breaker.executeSupplier(operation);
    }
}
```

### Configuration Best Practices

#### 1. Environment-Specific Configuration
```yaml
# application.yml
server:
  port: ${SERVER_PORT:8290}
  
logging:
  level:
    org.wso2.graalvm: ${LOG_LEVEL:INFO}
    
security:
  jwt:
    issuer: ${JWT_ISSUER:https://auth.example.com}
    secret: ${JWT_SECRET:#{null}}  # Force from environment
```

#### 2. Configuration Validation
```java
@PostConstruct
public void validateConfiguration() {
    if (jwtConfig.getSecret() == null) {
        throw new ConfigurationException("JWT secret must be provided via JWT_SECRET environment variable");
    }
}
```

## 🔧 Custom Dependency Injection System

This project uses a **custom lightweight dependency injection container** instead of Spring Framework, designed specifically for GraalVM native image compatibility and minimal resource usage.

### Key Features

- **No Framework Dependencies**: Zero dependency on Spring or other DI frameworks
- **GraalVM Native Compatible**: Fully compatible with ahead-of-time compilation
- **Lightweight**: Minimal memory footprint and fast startup
- **Component Scanning**: Automatic discovery of annotated components
- **Configuration Binding**: Type-safe configuration property binding
- **Lifecycle Management**: Proper component initialization and cleanup

### Custom Annotations

#### Component Registration
```java
@MediatorComponent              // Replaces @Component
@RestEndpoint                   // Replaces @RestController  
@ConfigProperties("prefix")     // Replaces @ConfigurationProperties
@ValidatedConfig               // Replaces @Validated
```

#### HTTP Routing
```java
@Route("/api/v1/sequences")    // Replaces @RequestMapping
@Get("/list")                  // Replaces @GetMapping
@Post("/deploy")               // Replaces @PostMapping
```

#### Testing
```java
@IntegrationTest               // Replaces @SpringJUnitConfig
```

### Usage Examples

#### Mediator Component
```java
@MediatorComponent
public class LogMediator implements Mediator {
    private final MetricsService metricsService;
    
    // Constructor injection
    public LogMediator(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @Override
    public CompletableFuture<MediationResult> mediate(MessageContext context) {
        // Implementation
    }
}
```

#### Configuration Properties
```java
@ConfigProperties("server")
@ValidatedConfig
public class ServerConfig {
    private int port = 8290;
    private Duration timeout = Duration.ofSeconds(30);
    private List<String> allowedOrigins = List.of("*");
    
    // Getters and setters...
}
```

#### REST Endpoint
```java
@RestEndpoint
@Route("/api/v1/management")
public class ManagementController {
    private final SequenceRegistry sequenceRegistry;
    
    public ManagementController(SequenceRegistry sequenceRegistry) {
        this.sequenceRegistry = sequenceRegistry;
    }
    
    @Get("/sequences")
    public List<SequenceInfo> listSequences() {
        return sequenceRegistry.getAllSequences();
    }
}
```

### DI Container Initialization

```java
// Create configuration binder
Map<String, String> properties = loadConfigurationProperties();
ConfigurationBinder configBinder = new ConfigurationBinder(properties);

// Create and configure DI container
DIContainer container = new DIContainer(configBinder);

// Scan for components
container.scanPackages(
    "org.wso2.graalvm.mediators",
    "org.wso2.graalvm.transports", 
    "org.wso2.graalvm.management",
    "org.wso2.graalvm.security"
);

// Initialize all singleton components
container.initializeComponents();

// Get components
LogMediator logMediator = container.getComponent(LogMediator.class);
ServerConfig config = container.getComponent("server", ServerConfig.class);
```

### Configuration Property Sources

The configuration binder supports multiple property sources with the following precedence:

1. **System Properties** (`-Dserver.port=8080`)
2. **Environment Variables** (`SERVER_PORT=8080`)
3. **Configuration Files** (`application.yml`, `application.properties`)
4. **Default Values** (defined in configuration classes)

### Type Conversion Support

- **Primitives**: `int`, `long`, `boolean`, `double`
- **Strings**: Plain text values
- **Durations**: `30s`, `5m`, `1h`, `2d`
- **Collections**: `List<String>`, `Set<String>` (comma-separated values)
- **Enums**: Case-insensitive enum value matching
