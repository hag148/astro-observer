---
name: astronomy-observer
description: 此skill用于天文观测系统Pro项目 - 一个基于Java 17和USB摄像头的专业天文观测桌面应用程序。当需要实现、调试或扩展摄像头管理、图像处理（增强、叠加、检测）、GUI交互或天文观测工作流时使用此skill。同时也用于回答关于项目架构、依赖关系或天文观测技术的问题。
---

# 天文观测系统 Pro (Astronomy Observer Pro)

## 概述

天文观测系统Pro是一个专业的Java 17 Swing桌面应用程序，用于使用USB摄像头进行天文观测。它提供实时摄像头预览、多摄像头支持、图像增强、叠加和天体检测功能，专为天文成像优化。

## 项目结构

```
astro-observer/
├── src/main/java/com/astronomy/observer/
│   ├── camera/           # 摄像头管理
│   ├── config/           # UGREEN摄像头配置
│   ├── gui/              # Swing GUI
│   ├── image/            # 图像处理
│   └── model/            # 观测会话模型
├── captures/             # 图像目录
├── sessions/             # 会话记录
└── docs/                 # 文档
```

## 核心功能

### 1. 摄像头管理
- 使用 `OpenCVCameraManager.java` 进行摄像头控制
- 使用 `UGreenCameraConfig.java` 配置UGREEN FHD 2K摄像头
- 设置分辨率（2K QHD: 2560x1440, FHD: 1920x1080, HD: 1280x720）
- 控制曝光时间（10-1000ms）、增益（0-100）、亮度（0-100）
- 实现手动/自动对焦控制

### 2. 图像处理
- 使用 `AstronomyImageEnhancer.java` 的9种增强算法
- 9种算法：BASIC、CLAHE、DENOISE、SHARPEN、CONTRAST、HISTOGRAM、BRIGHTNESS、GAMMA、COLOR
- 使用 `OpenCVImageProcessor.java` 进行核心图像处理

### 3. 图像叠加
- 使用 `OpenCVImageStacker.java` 进行多帧叠加
- 三种叠加方式：平均叠加、中位数叠加、Kappa-Sigma叠加
- 捕获30-100帧以提高信噪比

### 4. 天体检测
- 自动检测星星、行星等天体对象
- 在图像上显示检测标记
- 支持点击查看天体详情

### 5. GUI交互
- 使用 `AdvancedAstronomyFrame.java` 作为主界面
- 点击放大、拖拽移动、滚轮缩放
- 双击定位、天体标记显示

### 6. 观测会话
- 创建和管理观测会话
- 记录观测备注和数据
- 保存会话到sessions目录

### 7. 天文预设
- 月球、行星、深空、恒星、太阳五种预设
- 每种预设都有优化的曝光、增益等参数

## 技术栈

- **语言**：Java 17
- **UI框架**：Swing + FlatLaf暗色主题
- **图像处理**：OpenCV 4.13.0
- **摄像头API**：WebCam Capture
- **日志**：SLF4J + Logback

## 项目架构

### 分层架构
```
表示层 (GUI)
    ↓
业务逻辑层
    - CameraManager
    - ImageProcessor
    - ImageStacker
    ↓
数据层 (Model)
    - ObservationSession
```

### 设计模式
- **MVC模式**：Model、View、Controller
- **策略模式**：多种图像增强算法
- **工厂模式**：纯Java版vs OpenCV版图像处理器

## 构建和运行

```bash
# 编译
mvn clean package

# 运行
run.bat  # Windows
./run.sh # Linux/Mac
```

## 天文观测最佳实践

1. **设备配置**：连接摄像头到望远镜目镜，使用三脚架固定
2. **观测环境**：远离城市光污染，新月光照最佳
3. **拍摄技巧**：
   - 行星：高帧率视频，后期叠加
   - 月球：短曝光，避免过曝
   - 深空：长曝光，多帧叠加
4. **叠加指南**：捕获30-100帧，选择合适的叠加方法
5. **图像增强**：先叠加再增强，调整亮度对比度

## 常见问题

- **找不到摄像头**：检查连接和驱动
- **检测不到天体**：确保图像够亮，先叠加以提高信噪比
- **图像质量差**：检查摄像头清洁度，尝试更高分辨率
