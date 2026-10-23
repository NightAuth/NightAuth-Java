@echo off
setlocal enabledelayedexpansion
title NightAuth Native Java CLI
chcp 65001 >nul
cd /d "%~dp0"

set "JAVA_BIN=java"
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)

"%JAVA_BIN%" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%~dp0..\..\bin\NightAuth-Console.jar" %*
set "EXIT_CODE=%ERRORLEVEL%"

if %EXIT_CODE% NEQ 0 (
    echo.
    echo Application terminated with exit code %EXIT_CODE%.
    pause
)
endlocal
