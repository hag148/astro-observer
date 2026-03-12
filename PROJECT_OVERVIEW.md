# 天文观测系统 Pro (Astronomy Observer Pro) 项目概述

## 1. 项目简介

天文观测系统 Pro 是一个基于 Java 开发的专业天文观测应用程序，专为 UGREEN FHD 2K 摄像头优化，提供全面的天文观测功能。该系统集成了实时摄像头预览、图像叠加、天体检测、图像增强等专业功能，为天文爱好者和专业观测者提供了一个强大的观测工具。

## 2. 主要功能

### 2.1 核心功能
- 实时摄像头预览
- 多摄像头支持
- 可调节分辨率（支持2K/2560x1440）
- 高速图像捕获
- 图像保存和管理

### 2.2 天文观测专用功能
- **天文预设**：月球、行星、深空天体、恒星、太阳观测优化
- **图像叠加**：支持平均叠加、中位数叠加、Kappa-Sigma叠加
- **图像增强**：直方图均衡化、锐化、对比度调整
- **天体检测**：自动识别星星、行星等天体对象
- **观测会话**：完整的观测记录管理
- **天体标识**：点击检测出的天体在实时画面显示标记
- **交互式观测**：支持地图式操作（拖拽、缩放、定位）

### 2.3 摄像头控制
- 曝光时间调节
- 增益控制
- 亮度/对比度调整
- 分辨率选择
- 手动/自动对焦控制
- 针对UGREEN摄像头的优化配置

### 2.4 界面交互
- 点击放大：点击画面任意位置进行放大查看
- 拖拽移动：拖拽画面移动查看区域
- 滚轮缩放：使用鼠标滚轮平滑缩放
- 双击定位：双击快速定位到指定位置
- 高性能渲染：流畅的缩放和移动体验

## 3. 技术栈

- **Java 17** - 核心语言
- **OpenCV** - 计算机视觉和图像处理
- **WebCam Capture** - USB摄像头访问
- **FlatLaf** - 现代化暗色主题UI
- **Swing** - 图形用户界面
- **SLF4J** - 日志框架
- **Maven** - 项目构建和依赖管理

## 4. 系统要求

- Java 17 或更高版本
- Maven 3.6+
- USB摄像头（推荐 UGREEN FHD 2K）
- 至少4GB内存
- Windows/Linux/macOS

## 5. 项目结构

```
astro-observer/
├── src/main/java/com/astronomy/observer/
│   ├── camera/            # 摄像头管理
│   │   └── OpenCVCameraManager.java
│   ├── config/            # 配置文件
│   │   └── UGreenCameraConfig.java
│   ├── gui/               # 图形用户界面
│   │   ├── AdvancedAstronomyFrame.java
│   │   ├── CameraViewPanel.java
│   │   ├── EnhancementControlPanel.java
│   │   ├── InfoPanel.java
│   │   ├── MainToolBar.java
│   │   ├── OverlayPanel.java
│   │   ├── ParametersControlPanel.java
│   │   └── StatusBar.java
│   ├── image/             # 图像处理
│   │   ├── AstronomyImageEnhancer.java
│   │   ├── OpenCVImageProcessor.java
│   │   └── OpenCVImageStacker.java
│   └── model/             # 数据模型
│       └── ObservationSession.java
├── captures/              # 保存的图像
├── sessions/              # 观测会话记录
├── pom.xml                # Maven配置
├── run.bat                # Windows启动脚本
└── run.sh                 # Linux/Mac启动脚本
```

## 6. 核心模块

### 6.1 摄像头管理模块
- **OpenCVCameraManager**：负责摄像头的初始化、参数设置和视频流获取
- 支持多摄像头检测和切换
- 提供分辨率选择和参数调节功能

### 6.2 图像处理模块
- **OpenCVImageProcessor**：处理图像转换和天体检测
- **OpenCVImageStacker**：执行图像叠加操作
- **AstronomyImageEnhancer**：提供针对不同天体类型的图像增强算法

### 6.3 用户界面模块
- **AdvancedAstronomyFrame**：主应用程序窗口
- **CameraViewPanel**：摄像头视图和交互控制
- **ParametersControlPanel**：参数调节面板
- **EnhancementControlPanel**：图像增强控制
- **InfoPanel**：观测信息和天体检测结果显示

### 6.4 观测会话模块
- **ObservationSession**：管理观测数据和记录
- 支持会话创建、数据记录和保存功能

## 7. 安装与运行

### 7.1 安装步骤
1. 克隆项目：`cd d:\CodeBuddy\astro-observer`
2. 编译项目：`mvn clean package`

### 7.2 运行方式
- **Windows**：`run.bat`
- **Linux/Mac**：`chmod +x run.sh && ./run.sh`
- **直接运行JAR**：`java -jar target/astro-observer-1.0.0.jar`

## 8. 技术亮点

1. **专业性**：专为天文观测设计，提供针对不同天体类型的优化预设和处理算法
2. **易用性**：直观的用户界面和交互式操作，适合各级用户
3. **高性能**：流畅的实时预览和图像处理，支持高分辨率摄像头
4. **扩展性**：模块化架构，易于添加新功能和支持新设备
5. **专业性**：集成了多种专业天文图像处理技术，如Kappa-Sigma叠加和天体检测
6. **优化**：专为UGREEN FHD 2K摄像头优化，充分发挥硬件性能
7. **跨平台**：支持Windows、Linux和macOS等多种操作系统

## 9. 典型用例

### 9.1 月球观测
- 使用月球预设：FHD，曝光50ms，增益30
- 短曝光避免过曝
- 可使用图像增强突出月球表面细节

### 9.2 行星观测
- 使用行星预设：FHD，曝光100ms，增益50
- 捕获多帧图像进行叠加
- 应用行星表面增强算法

### 9.3 深空天体观测
- 使用深空天体预设：2K QHD，曝光200ms，增益100
- 捕获大量帧进行叠加（30-100帧）
- 应用深空增强算法突出微弱信号

## 10. 版本历史

### v1.1.0 (2026-03-04)
- 新增实时画面天体标识功能
- 新增手动对焦控制
- 新增点击放大、拖拽移动、滚轮缩放等交互功能
- 修复画面闪烁和UI字体消失问题
- 修复中文字体显示乱码
- 优化渲染性能，缩放更加流畅

### v1.0.0 (2025-03-03)
- 初始版本发布
- 支持USB摄像头实时预览
- 实现图像叠加功能
- 添加天体检测
- UGREEN FHD 2K摄像头优化
- 观测会话管理

## 11. 许可证

MIT License

## 12. 总结

天文观测系统 Pro 是一个功能全面、专业的天文观测应用程序，为天文爱好者和专业观测者提供了一个强大的工具，使天文观测变得更加便捷和高效。通过图像叠加和增强技术，用户可以获得更加清晰、详细的天体图像，而交互式操作则使观测体验更加直观和沉浸。

项目的模块化设计和清晰的代码结构也使其成为学习Java桌面应用开发和图像处理的优秀范例。