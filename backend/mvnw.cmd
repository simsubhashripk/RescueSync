@REM ----------------------------------------------------------------------------
@REM RescueSync Maven Wrapper batch script
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set "DIR=%~dp0"
set "MAVEN_CMD=%DIR%.maven\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MAVEN_CMD%" (
    call "%MAVEN_CMD%" %*
) else (
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        call mvn %*
    ) else (
        echo [ERROR] Maven not found.
        exit /b 1
    )
)

