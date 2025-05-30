#!/bin/bash

# WSO2 Micro Integrator GraalVM Edition Startup Script

# Set script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MI_HOME="$(dirname "$SCRIPT_DIR")"

# Set Java options for virtual threads and GraalVM
JAVA_OPTS="${JAVA_OPTS} --enable-preview"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx2g"

# Set system properties
JAVA_OPTS="${JAVA_OPTS} -Dmi.home=${MI_HOME}"
JAVA_OPTS="${JAVA_OPTS} -Dmi.config.file=${MI_HOME}/conf/application.yml"
JAVA_OPTS="${JAVA_OPTS} -Dlogback.configurationFile=${MI_HOME}/conf/logback.xml"

# Check if running as native image
if [ -x "${MI_HOME}/bin/wso2-micro-integrator" ]; then
    echo "Starting WSO2 Micro Integrator (Native Image)..."
    exec "${MI_HOME}/bin/wso2-micro-integrator" "$@"
else
    echo "Starting WSO2 Micro Integrator (JVM)..."
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        echo "Java is not installed or not in PATH"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt "21" ]; then
        echo "Java 21 or higher is required. Found Java $JAVA_VERSION"
        exit 1
    fi
    
    # Build classpath
    CLASSPATH="${MI_HOME}/lib/*"
    
    # Start the application
    exec java $JAVA_OPTS -cp "$CLASSPATH" org.wso2.graalvm.runtime.MicroIntegratorApplication "$@"
fi
