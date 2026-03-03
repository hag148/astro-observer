#!/bin/bash

echo "============================================"
echo "  天文观测系统 Pro v1.0"
echo "  Astronomy Observation System"
echo "============================================"
echo ""

# 检查Java
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java，请安装Java 17或更高版本"
    exit 1
fi

echo "[信息] Java已安装"
java -version
echo ""

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo "[警告] 未找到Maven，尝试直接运行已编译的jar..."
    if [ -f "target/astro-observer-1.0.0.jar" ]; then
        echo "[信息] 找到已编译的jar文件"
    else
        echo "[错误] 未找到已编译的jar文件"
        echo "[提示] 请先运行: mvn clean package"
        exit 1
    fi
else
    # 检查是否已编译
    if [ ! -f "target/astro-observer-1.0.0.jar" ]; then
        echo "[信息] 正在编译程序..."
        mvn clean package -DskipTests
        if [ $? -ne 0 ]; then
            echo "[错误] 编译失败"
            exit 1
        fi
        echo "[信息] 编译成功"
        echo ""
    fi
fi

# 创建必要的目录
mkdir -p captures
mkdir -p sessions

# 运行程序
echo "[信息] 启动天文观测系统..."
echo ""
java -jar target/astro-observer-1.0.0.jar

if [ $? -ne 0 ]; then
    echo ""
    echo "[错误] 程序运行失败"
    exit 1
fi

echo ""
echo "[信息] 程序已正常退出"
