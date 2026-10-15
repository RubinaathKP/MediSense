@echo off
REM MediSense Java & Maven Environment Configuration
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1
set M2_HOME=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\plugins\maven\lib\maven3
set MAVEN_HOME=%M2_HOME%
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%
echo Environment configured:
echo JAVA_HOME = %JAVA_HOME%
echo MAVEN_HOME = %MAVEN_HOME%
