# MediSense Java & Maven Environment Configuration
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
$env:M2_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\plugins\maven\lib\maven3"
$env:MAVEN_HOME = $env:M2_HOME
$env:PATH = "$env:JAVA_HOME\bin;$env:M2_HOME\bin;$env:PATH"

function mvn {
    & "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" @args
}

Write-Host "Environment configured:"
Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "MAVEN_HOME = $env:MAVEN_HOME"
