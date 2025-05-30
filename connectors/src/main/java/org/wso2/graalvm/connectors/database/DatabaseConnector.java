package org.wso2.graalvm.connectors.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.graalvm.core.context.IntegrationContext;
import org.wso2.graalvm.engine.core.Mediator;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Database connector mediator for SQL database operations.
 * Provides high-performance database connectivity with connection pooling using HikariCP.
 */
public class DatabaseConnector implements Mediator {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
    
    private final String name;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    
    private String dataSourceName;
    private String sqlQuery;
    private DatabaseOperation operation = DatabaseOperation.SELECT;
    private Map<String, Object> parameters = new HashMap<>();
    private boolean useVirtualThreads = true;
    private Executor executor;
    
    public DatabaseConnector(String name) {
        this.name = name;
        logger.debug("Database connector '{}' created", name);
    }
    
    public DatabaseConnector setDataSource(String dataSourceName) {
        this.dataSourceName = dataSourceName;
        return this;
    }
    
    public DatabaseConnector setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
        return this;
    }
    
    public DatabaseConnector setOperation(DatabaseOperation operation) {
        this.operation = operation;
        return this;
    }
    
    public DatabaseConnector addParameter(String name, Object value) {
        this.parameters.put(name, value);
        return this;
    }
    
    public DatabaseConnector setParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
        return this;
    }
    
    public DatabaseConnector setUseVirtualThreads(boolean useVirtualThreads) {
        this.useVirtualThreads = useVirtualThreads;
        return this;
    }
    
    public DatabaseConnector setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }
    
    /**
     * Register a data source with the connector
     */
    public void registerDataSource(String name, DatabaseConfig config) {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setDriverClassName(config.getDriverClassName());
            
            // Performance tuning
            hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
            hikariConfig.setMinimumIdle(config.getMinIdle());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
            hikariConfig.setIdleTimeout(config.getIdleTimeout());
            hikariConfig.setMaxLifetime(config.getMaxLifetime());
            
            // Additional properties
            hikariConfig.setPoolName("mi-db-pool-" + name);
            hikariConfig.setAutoCommit(true);
            hikariConfig.setConnectionTestQuery(config.getValidationQuery());
            
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            dataSources.put(name, dataSource);
            
            logger.info("Database connection pool '{}' created successfully", name);
            
        } catch (Exception e) {
            logger.error("Failed to create database connection pool '{}'", name, e);
            throw new RuntimeException("Failed to create database connection pool", e);
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean mediate(IntegrationContext context) {
        logger.debug("Database connector '{}' processing request", name);
        
        try {
            if (dataSourceName == null) {
                logger.error("No data source configured for database connector '{}'", name);
                context.setFault(new IllegalStateException("No data source configured"));
                return false;
            }
            
            if (sqlQuery == null) {
                logger.error("No SQL query configured for database connector '{}'", name);
                context.setFault(new IllegalStateException("No SQL query configured"));
                return false;
            }
            
            DataSource dataSource = dataSources.get(dataSourceName);
            if (dataSource == null) {
                logger.error("Data source '{}' not found", dataSourceName);
                context.setFault(new IllegalStateException("Data source not found: " + dataSourceName));
                return false;
            }
            
            if (useVirtualThreads && executor != null) {
                return executeAsync(dataSource, context);
            } else {
                return executeSync(dataSource, context);
            }
            
        } catch (Exception e) {
            logger.error("Error in database connector '{}'", name, e);
            context.setFault(e);
            return false;
        }
    }
    
    private boolean executeAsync(DataSource dataSource, IntegrationContext context) {
        CompletableFuture.runAsync(() -> {
            try {
                executeSync(dataSource, context);
            } catch (Exception e) {
                logger.error("Async database operation failed", e);
                context.setFault(e);
            }
        }, executor);
        
        return true;
    }
    
    private boolean executeSync(DataSource dataSource, IntegrationContext context) {
        try (Connection connection = dataSource.getConnection()) {
            
            switch (operation) {
                case SELECT -> {
                    return executeSelect(connection, context);
                }
                case INSERT, UPDATE, DELETE -> {
                    return executeUpdate(connection, context);
                }
                case CALL -> {
                    return executeStoredProcedure(connection, context);
                }
                default -> {
                    logger.error("Unsupported database operation: {}", operation);
                    context.setFault(new IllegalArgumentException("Unsupported operation: " + operation));
                    return false;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Database operation failed", e);
            context.setFault(e);
            return false;
        }
    }
    
    private boolean executeSelect(Connection connection, IntegrationContext context) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            
            setParameters(statement, context);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                JsonNode result = convertResultSetToJson(resultSet);
                context.setPayload(result);
                
                logger.debug("SELECT operation completed successfully");
                return true;
            }
        }
    }
    
    private boolean executeUpdate(Connection connection, IntegrationContext context) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            
            setParameters(statement, context);
            
            int affectedRows = statement.executeUpdate();
            
            // Create result object
            ObjectNode result = objectMapper.createObjectNode();
            result.put("operation", operation.name());
            result.put("affectedRows", affectedRows);
            result.put("success", true);
            
            context.setPayload(result);
            
            logger.debug("{} operation completed successfully, affected rows: {}", 
                        operation, affectedRows);
            return true;
        }
    }
    
    private boolean executeStoredProcedure(Connection connection, IntegrationContext context) throws SQLException {
        try (CallableStatement statement = connection.prepareCall(sqlQuery)) {
            
            setParameters(statement, context);
            
            boolean hasResultSet = statement.execute();
            
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    JsonNode result = convertResultSetToJson(resultSet);
                    context.setPayload(result);
                }
            } else {
                int updateCount = statement.getUpdateCount();
                ObjectNode result = objectMapper.createObjectNode();
                result.put("operation", "CALL");
                result.put("updateCount", updateCount);
                result.put("success", true);
                context.setPayload(result);
            }
            
            logger.debug("Stored procedure call completed successfully");
            return true;
        }
    }
    
    private void setParameters(PreparedStatement statement, IntegrationContext context) throws SQLException {
        int paramIndex = 1;
        
        // Set parameters from context properties
        for (Map.Entry<String, Object> param : parameters.entrySet()) {
            Object value = param.getValue();
            
            // Check if it's a context property reference
            if (value instanceof String stringValue && stringValue.startsWith("${") && stringValue.endsWith("}")) {
                String propertyName = stringValue.substring(2, stringValue.length() - 1);
                value = context.getProperty(propertyName);
            }
            
            setParameter(statement, paramIndex++, value);
        }
    }
    
    private void setParameter(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NULL);
        } else if (value instanceof String) {
            statement.setString(index, (String) value);
        } else if (value instanceof Integer) {
            statement.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            statement.setLong(index, (Long) value);
        } else if (value instanceof Double) {
            statement.setDouble(index, (Double) value);
        } else if (value instanceof Boolean) {
            statement.setBoolean(index, (Boolean) value);
        } else if (value instanceof Timestamp) {
            statement.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Date) {
            statement.setDate(index, (Date) value);
        } else {
            statement.setString(index, value.toString());
        }
    }
    
    private JsonNode convertResultSetToJson(ResultSet resultSet) throws SQLException {
        ArrayNode results = objectMapper.createArrayNode();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (resultSet.next()) {
            ObjectNode row = objectMapper.createObjectNode();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                
                if (value != null) {
                    if (value instanceof String) {
                        row.put(columnName, (String) value);
                    } else if (value instanceof Integer) {
                        row.put(columnName, (Integer) value);
                    } else if (value instanceof Long) {
                        row.put(columnName, (Long) value);
                    } else if (value instanceof Double) {
                        row.put(columnName, (Double) value);
                    } else if (value instanceof Boolean) {
                        row.put(columnName, (Boolean) value);
                    } else if (value instanceof Timestamp) {
                        row.put(columnName, value.toString());
                    } else {
                        row.put(columnName, value.toString());
                    }
                } else {
                    row.putNull(columnName);
                }
            }
            
            results.add(row);
        }
        
        return results;
    }
    
    /**
     * Shutdown all data sources
     */
    public void shutdown() {
        logger.info("Shutting down database connector '{}'", name);
        
        dataSources.values().forEach(dataSource -> {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        });
        
        dataSources.clear();
        logger.info("Database connector '{}' shutdown completed", name);
    }
    
    public enum DatabaseOperation {
        SELECT, INSERT, UPDATE, DELETE, CALL
    }
    
    /**
     * Database configuration class
     */
    public static class DatabaseConfig {
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName;
        private int maxPoolSize = 20;
        private int minIdle = 5;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private String validationQuery = "SELECT 1";
        
        // Getters and setters
        public String getJdbcUrl() { return jdbcUrl; }
        public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        
        public int getMinIdle() { return minIdle; }
        public void setMinIdle(int minIdle) { this.minIdle = minIdle; }
        
        public long getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        
        public long getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; }
        
        public long getMaxLifetime() { return maxLifetime; }
        public void setMaxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; }
        
        public String getValidationQuery() { return validationQuery; }
        public void setValidationQuery(String validationQuery) { this.validationQuery = validationQuery; }
    }
    
    @Override
    public String toString() {
        return String.format("DatabaseConnector{name='%s', dataSource='%s', operation=%s}", 
                           name, dataSourceName, operation);
    }
}
