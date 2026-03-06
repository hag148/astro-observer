# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

天文观测系统 Pro 是一个基于 Java 17 的专业天文观测应用程序，使用 USB 摄像头进行天文图像采集、处理和分析。

**技术栈：**
- Java 17
- OpenCV 4.13.0 (通过 ByteDeco)
- webcam-capture API
- FlatLaf (现代化暗色主题 UI)
- Swing
- Maven
- JUnit 5

## 构建和运行

### 编译项目
```bash
mvn clean package
```

### 跳过测试编译
```bash
mvn package -DskipTests
```

### 运行程序
**Windows:**
```bash
run.bat
```

**Linux/Mac:**
```bash
chmod +x run.sh
./run.sh
```

**或直接运行 JAR:**
```bash
java -jar target/astro-observer-1.0.0.jar
```

### 运行测试
```bash
mvn test
```

## 代码架构

### 分层架构
```
Presentation Layer (GUI)
    ↓ depends on
Business Layer (Camera, Image, Config, Model)
    ↓ depends on
Integration Layer (WebCam, OpenCV)
```

### 核心模块

**GUI 层** (`src/main/java/com/astronomy/observer/gui/`)
- `AdvancedAstronomyFrame` - 主界面，整合所有功能

**业务层** (`src/main/java/com/astronomy/observer/`)
- `camera/OpenCVCameraManager` - 摄像头管理，支持多摄像头、分辨率切换
- `image/OpenCVImageProcessor` - 图像处理核心，使用 OpenCV API
- `image/OpenCVImageStacker` - 图像叠加算法（平均、中位数、Kappa-Sigma）
- `image/AstronomyImageEnhancer` - 天文专用图像增强（直方图均衡化、锐化等）
- `config/UGreenCameraConfig` - UGREEN FHD 2K 摄像头优化配置
- `model/ObservationSession` - 观测会话数据模型

### 关键设计模式

1. **Listener 模式**：摄像头帧监听器 (`FrameListener` 接口)
2. **配置对象模式**：`UGreenCameraConfig.AstronomyPreset` 天文预设配置
3. **策略模式**：图像增强算法 (`EnhancementAlgorithm` 枚举)

## OpenCV 4.13.0 注意事项

项目使用 OpenCV 4.13.0-1.5.13 (ByteDeco 平台版本)，所有 API 调用应使用 `org.bytedeco.opencv` 包。

**常见 API 调用示例：**
```java
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_photo;
import org.bytedeco.opencv.opencv_core.Mat;

// 图像转换
opencv_imgproc.cvtColor(src, dst, opencv_imgproc.COLOR_BGR2RGB);

// 直方图均衡化
opencv_photo.createCLAHE(40.0, new Size(8, 8)).apply(src, dst);
```

**重要：** 不要使用 `org.opencv` 包，那是旧版本 OpenCV，项目已完全迁移到 ByteDeco 版本。

## 图像处理流程

1. **摄像头采集** → `OpenCVCameraManager.captureFrame()`
2. **图像处理** → `OpenCVImageProcessor` (转换、滤波、增强)
3. **图像叠加** → `OpenCVImageStacker` (多帧处理)
4. **UI 显示** → `AdvancedAstronomyFrame` 渲染

## 观测会话管理

- 新建会话：`newSessionButton` → 创建新的 `ObservationSession`
- 保存会话：`saveSessionButton` → 序列化到 JSON 文件
- 会话目录：`sessions/`

## UGREEN FHD 2K 摄像头优化

项目针对 UGREEN FHD 2K 摄像头进行了特殊配置，包括：
- 分辨率支持：2560x1440 (2K)、1920x1080 (FHD)、1280x720 (HD)、640x480 (VGA)
- 天文观测预设：月球、行星、深空天体、恒星、太阳
- 参数优化：曝光时间、增益、亮度/对比度范围

参考 `config/UGreenCameraConfig.java` 了解详细配置。

## 文档位置

详细文档位于 `docs/` 目录：
- `README.md` - 项目总览
- `01-项目概述.md` - 项目背景和目标
- `02-系统设计文档.md` - 架构和设计
- `03-API文档.md` - API 接口说明
- `05-用户手册.md` - 使用指南
- `06-开发文档.md` - 开发环境搭建

## 注意事项

1. **编码问题**：项目使用 UTF-8 编码，中文字体显示需配置
2. **线程安全**：摄像头采集使用独立线程，UI 更新需考虑线程安全
3. **内存管理**：OpenCV Mat 对象使用 `release()` 释放
4. **字体路径**：中文字体路径需要根据系统配置
