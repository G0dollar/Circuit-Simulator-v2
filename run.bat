@echo off
echo =========================================
echo Compiling Electricity Circuit Simulator...
echo =========================================

set "MAVEN_CMD=mvn"
if exist "%USERPROFILE%\.maven\maven-3.9.15\bin\mvn.cmd" set "MAVEN_CMD=%USERPROFILE%\.maven\maven-3.9.15\bin\mvn.cmd"

call "%MAVEN_CMD%" -q -DskipTests package
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %errorlevel%
)
echo =========================================
echo Launching Simulator...
echo =========================================
java -cp target\classes -Dswing.aatext=true -Dawt.useSystemAAFontSettings=on electricity.ui.CircuitSim
