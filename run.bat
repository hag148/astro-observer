@echo off
title 天文观测系统 Pro
chcp 65001 >nul

echo ============================================
echo   天文观测系统 Pro v1.0
echo   Astronomy Observation System
echo ============================================
echo.

REM 检查Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请安装Java 25或更高版本
    pause
    exit /b 1
)

echo [信息] Java已安装
java -version
echo.

REM 检查Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [警告] 未找到Maven，尝试直接运行已编译的jar...
    if exist "target\astro-observer-1.0.0.jar" (
        echo [信息] 找到已编译的jar文件
        goto :run
    ) else (
        echo [错误] 未找到已编译的jar文件
        echo [提示] 请先运行: mvn clean package
        pause
        exit /b 1
    )
)

REM 检查是否已编译
if not exist "target\astro-observer-1.0.0.jar" (
    echo [信息] 正在编译程序...
    call mvn clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [错误] 编译失败
        pause
        exit /b 1
    )
    echo [信息] 编译成功
    echo.
)

:run
REM 创建必要的目录
if not exist "captures" mkdir captures
if not exist "sessions" mkdir sessions

REM 运行程序
echo [信息] 启动天文观测系统...
echo.
java -jar "target\astro-observer-1.0.0.jar"

if %errorlevel% neq 0 (
    echo.
    echo [错误] 程序运行失败
    pause
    exit /b 1
)

echo.
echo [信息] 程序已正常退出
pause
