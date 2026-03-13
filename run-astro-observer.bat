@echo off
setlocal

rem 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
echo 错误：未找到Java运行时环境。请先安装Java 25或更高版本。
pause
exit /b 1
)

rem 运行天文观测系统
echo 启动天文观测系统...
java -jar target\astro-observer-1.0.0.jar

endlocal