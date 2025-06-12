package org.wso2.graalvm.core.di;

import org.wso2.graalvm.core.annotation.MediatorComponent;
import org.wso2.graalvm.core.annotation.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Lightweight dependency injection container to replace Spring's IoC container.
 * 
 * This implementation provides:
 * - Component scanning and auto-discovery
 * - Singleton and prototype scopes
 * - Constructor-based dependency injection
 * - Configuration property binding
 * - Lifecycle management
 */
public class DIContainer {
    
    private static final Logger logger = LoggerFactory.getLogger(DIContainer.class);
    
    private final Map<Class<?>, Object> singletonInstances = new ConcurrentHashMap<>();
    private final Map<Class<?>, ComponentMetadata> componentRegistry = new ConcurrentHashMap<>();
    private final Map<String, Object> namedComponents = new ConcurrentHashMap<>();
    private final ConfigurationBinder configurationBinder;
    
    public DIContainer(ConfigurationBinder configurationBinder) {
        this.configurationBinder = configurationBinder;
    }
    
    /**
     * Scan for components in the specified packages.
     */
    public void scanPackages(String... packages) {
        logger.info("Scanning packages for components: {}", String.join(", ", packages));
        
        for (String packageName : packages) {
            try {
                Set<Class<?>> classes = findClassesInPackage(packageName);
                for (Class<?> clazz : classes) {
                    registerComponentIfAnnotated(clazz);
                }
            } catch (Exception e) {
                logger.warn("Failed to scan package: {}", packageName, e);
            }
        }
        
        logger.info("Component registration complete. Found {} components", componentRegistry.size());
    }
    
    /**
     * Get a component instance by type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> type) {
        ComponentMetadata metadata = componentRegistry.get(type);
        if (metadata == null) {
            throw new DIException("No component registered for type: " + type.getName());
        }
        
        if ("singleton".equals(metadata.scope)) {
            return (T) singletonInstances.computeIfAbsent(type, k -> createInstance(metadata));
        } else {
            return (T) createInstance(metadata);
        }
    }
    
    /**
     * Get a component instance by name.
     */
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name, Class<T> expectedType) {
        Object component = namedComponents.get(name);
        if (component == null) {
            throw new DIException("No component registered with name: " + name);
        }
        
        if (!expectedType.isInstance(component)) {
            throw new DIException("Component " + name + " is not of expected type: " + expectedType.getName());
        }
        
        return (T) component;
    }
    
    /**
     * Register a component manually.
     */
    public void registerComponent(Class<?> type, Object instance) {
        singletonInstances.put(type, instance);
        componentRegistry.put(type, new ComponentMetadata(type, "singleton", ""));
        logger.debug("Manually registered component: {}", type.getName());
    }
    
    /**
     * Initialize all singleton components.
     */
    public void initializeComponents() {
        logger.info("Initializing singleton components...");
        
        // First pass: create configuration components
        componentRegistry.values().stream()
            .filter(metadata -> metadata.componentClass.isAnnotationPresent(ConfigProperties.class))
            .forEach(metadata -> {
                if ("singleton".equals(metadata.scope)) {
                    getComponent(metadata.componentClass);
                }
            });
        
        // Second pass: create other components
        componentRegistry.values().stream()
            .filter(metadata -> !metadata.componentClass.isAnnotationPresent(ConfigProperties.class))
            .forEach(metadata -> {
                if ("singleton".equals(metadata.scope)) {
                    getComponent(metadata.componentClass);
                }
            });
        
        logger.info("Component initialization complete. {} singletons created", singletonInstances.size());
    }
    
    private void registerComponentIfAnnotated(Class<?> clazz) {
        if (clazz.isAnnotationPresent(MediatorComponent.class)) {
            MediatorComponent annotation = clazz.getAnnotation(MediatorComponent.class);
            String scope = annotation.scope();
            String name = annotation.value().isEmpty() ? clazz.getSimpleName() : annotation.value();
            
            ComponentMetadata metadata = new ComponentMetadata(clazz, scope, name);
            componentRegistry.put(clazz, metadata);
            
            logger.debug("Registered mediator component: {} (scope: {})", clazz.getName(), scope);
        } else if (clazz.isAnnotationPresent(ConfigProperties.class)) {
            ConfigProperties annotation = clazz.getAnnotation(ConfigProperties.class);
            String name = annotation.value().isEmpty() ? clazz.getSimpleName() : annotation.value();
            
            ComponentMetadata metadata = new ComponentMetadata(clazz, "singleton", name);
            componentRegistry.put(clazz, metadata);
            
            logger.debug("Registered configuration component: {} (prefix: {})", clazz.getName(), name);
        }
    }
    
    private Object createInstance(ComponentMetadata metadata) {
        try {
            Class<?> clazz = metadata.componentClass;
            
            // Handle configuration binding
            if (clazz.isAnnotationPresent(ConfigProperties.class)) {
                ConfigProperties annotation = clazz.getAnnotation(ConfigProperties.class);
                Object instance = createInstanceWithDependencies(clazz);
                configurationBinder.bindConfiguration(instance, annotation.value());
                
                if (!metadata.name.isEmpty()) {
                    namedComponents.put(metadata.name, instance);
                }
                
                return instance;
            } else {
                Object instance = createInstanceWithDependencies(clazz);
                
                if (!metadata.name.isEmpty()) {
                    namedComponents.put(metadata.name, instance);
                }
                
                return instance;
            }
        } catch (Exception e) {
            throw new DIException("Failed to create instance of: " + metadata.componentClass.getName(), e);
        }
    }
    
    private Object createInstanceWithDependencies(Class<?> clazz) throws Exception {
        Constructor<?>[] constructors = clazz.getConstructors();
        
        if (constructors.length == 0) {
            throw new DIException("No public constructors found for: " + clazz.getName());
        }
        
        // Find the best constructor (preferably with dependencies)
        Constructor<?> bestConstructor = findBestConstructor(constructors);
        
        // Resolve dependencies
        Object[] dependencies = resolveDependencies(bestConstructor);
        
        // Create instance
        return bestConstructor.newInstance(dependencies);
    }
    
    private Constructor<?> findBestConstructor(Constructor<?>[] constructors) {
        // Prefer constructors with more parameters (more dependencies)
        Constructor<?> best = constructors[0];
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > best.getParameterCount()) {
                best = constructor;
            }
        }
        return best;
    }
    
    private Object[] resolveDependencies(Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] dependencies = new Object[parameterTypes.length];
        
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> dependencyType = parameterTypes[i];
            
            if (componentRegistry.containsKey(dependencyType)) {
                dependencies[i] = getComponent(dependencyType);
            } else {
                // Try to find by interface or superclass
                dependencies[i] = findComponentByType(dependencyType);
                if (dependencies[i] == null) {
                    throw new DIException("Cannot resolve dependency: " + dependencyType.getName() + 
                        " for constructor: " + constructor.getDeclaringClass().getName());
                }
            }
        }
        
        return dependencies;
    }
    
    private Object findComponentByType(Class<?> requiredType) {
        // Find component that implements the required interface or extends the required class
        for (Map.Entry<Class<?>, ComponentMetadata> entry : componentRegistry.entrySet()) {
            Class<?> componentType = entry.getKey();
            if (requiredType.isAssignableFrom(componentType)) {
                return getComponent(componentType);
            }
        }
        return null;
    }
    
    private Set<Class<?>> findClassesInPackage(String packageName) throws IOException {
        Set<Class<?>> classes = ConcurrentHashMap.newKeySet();
        String packagePath = packageName.replace('.', '/');
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                Path packageDir = Paths.get(resource.getPath());
                if (Files.exists(packageDir)) {
                    try (Stream<Path> paths = Files.walk(packageDir)) {
                        paths.filter(path -> path.toString().endsWith(".class"))
                            .forEach(path -> {
                                try {
                                    String className = packageName + "." + 
                                        packageDir.relativize(path).toString()
                                            .replace('/', '.')
                                            .replace(".class", "");
                                    Class<?> clazz = Class.forName(className);
                                    classes.add(clazz);
                                } catch (ClassNotFoundException e) {
                                    logger.debug("Failed to load class: {}", path, e);
                                }
                            });
                    }
                }
            }
        }
        
        return classes;
    }
    
    /**
     * Shutdown the container and cleanup resources.
     */
    public void shutdown() {
        logger.info("Shutting down DI container...");
        singletonInstances.clear();
        componentRegistry.clear();
        namedComponents.clear();
        logger.info("DI container shutdown complete");
    }
    
    private static class ComponentMetadata {
        final Class<?> componentClass;
        final String scope;
        final String name;
        
        ComponentMetadata(Class<?> componentClass, String scope, String name) {
            this.componentClass = componentClass;
            this.scope = scope;
            this.name = name;
        }
    }
    
    public static class DIException extends RuntimeException {
        public DIException(String message) {
            super(message);
        }
        
        public DIException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
