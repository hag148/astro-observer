# 天文观测系统 Pro - API参考文档

## 核心类说明

### 1. OpenCVCameraManager

**职责**: 摄像头管理和控制

**主要方法**:
- `startCamera()`: 启动摄像头
- `stopCamera()`: 停止摄像头
- `setResolution(int width, int height)`: 设置分辨率
- `setExposureTime(int ms)`: 设置曝光时间
- `setGain(int value)`: 设置增益
- `setBrightness(int value)`: 设置亮度
- `setFocusMode(boolean auto)`: 设置对焦模式

### 2. AstronomyImageEnhancer

**职责**: 图像增强处理

**9种增强算法**:
1. `BASIC` - 基础调整
2. `CLAHE` - 限制对比度自适应直方图均衡化
3. `DENOISE` - 去噪处理
4. `SHARPEN` - 锐化增强
5. `CONTRAST` - 对比度调整
6. `HISTOGRAM` - 直方图均衡化
7. `BRIGHTNESS` - 亮度调整
8. `GAMMA` - Gamma校正
9. `COLOR` - 颜色增强

**主要方法**:
- `enhance(BufferedImage image, EnhancementType type)`: 应用增强
- `getEnhancementTypeNames()`: 获取所有算法名称

### 3. OpenCVImageStacker

**职责**: 图像叠加和信噪比计算

**三种叠加方式**:
1. `AVERAGE` - 平均叠加
2. `MEDIAN` - 中位数叠加
3. `KAPPA_SIGMA` - Kappa-Sigma叠加

**主要方法**:
- `stack(List<BufferedImage> images, StackingType type)`: 执行叠加
- `calculateSNR(BufferedImage image)`: 计算信噪比
- `getStackingTypeNames()`: 获取所有叠加方式名称

### 4. AdvancedAstronomyFrame

**职责**: 主GUI界面和事件处理

**主要组件**:
- 摄像头选择下拉框
- 预设下拉框（月球、行星、深空等）
- 参数调节滑块（曝光、增益、亮度）
- 启动/停止按钮
- 图像处理按钮
- 观测会话管理

**交互功能**:
- 点击放大
- 拖拽移动
- 滚轮缩放
- 双击定位
- 天体标记显示

### 5. ObservationSession

**职责**: 观测会话数据模型

**主要属性**:
- 会话ID
- 创建时间
- 观测目标
- 摄像头配置
- 图像列表
- 备注信息

**主要方法**:
- `saveToFile(String path)`: 保存会话数据
- `loadFromFile(String path)`: 加载会话数据
- `addImage(BufferedImage image)`: 添加图像

## UGREEN摄像头配置

### 支持的分辨率
- 2K QHD: 2560x1440
- FHD: 1920x1080
- HD: 1280x720
- VGA: 640x480

### 天文观测推荐配置
- **月球**: FHD, 曝光50ms, 增益30
- **行星**: FHD, 曝光100ms, 增益50
- **深空**: 2K QHD, 曝光200ms, 增益100
- **恒星**: FHD, 曝光150ms, 增益75
- **太阳**: FHD, 曝光10ms, 增益1000 (必须使用滤镜)
