@echo off
REM WSO2 Micro Integrator GraalVM Edition Startup Script for Windows

setlocal enabledelayedexpansion

REM Set script directory
set SCRIPT_DIR=%~dp0
set MI_HOME=%SCRIPT_DIR%..

REM Set Java options for virtual threads and GraalVM
set JAVA_OPTS=%JAVA_OPTS% --enable-preview
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=200
set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx2g

REM Set system properties
set JAVA_OPTS=%JAVA_OPTS% -Dmi.home=%MI_HOME%
set JAVA_OPTS=%JAVA_OPTS% -Dmi.config.file=%MI_HOME%\conf\application.yml
set JAVA_OPTS=%JAVA_OPTS% -Dlogback.configurationFile=%MI_HOME%\conf\logback.xml

REM Check if running as native image
if exist "%MI_HOME%\bin\wso2-micro-integrator.exe" (
    echo Starting WSO2 Micro Integrator ^(Native Image^)...
    "%MI_HOME%\bin\wso2-micro-integrator.exe" %*
) else (
    echo Starting WSO2 Micro Integrator ^(JVM^)...
    
    REM Check Java version
    java -version >nul 2>&1
    if errorlevel 1 (
        echo Java is not installed or not in PATH
        exit /b 1
    )
    
    REM Build classpath
    set CLASSPATH=%MI_HOME%\lib\*
    
    REM Start the application
    java %JAVA_OPTS% -cp "%CLASSPATH%" org.wso2.graalvm.runtime.MicroIntegratorApplication %*
)
