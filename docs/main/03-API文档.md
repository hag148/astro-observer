# 天文观测系统 Pro - API 文档

## 1. 公共 API 接口

### 1.1 CameraManager

#### 类概述
`CameraManager` 负责管理 USB 摄像头设备，提供摄像头检测、连接控制和图像捕获功能。

#### 方法列表

##### `public CameraManager()`
构造函数，初始化摄像头管理器。

##### `public List<Webcam> getAvailableCameras()`
获取所有可用的 USB 摄像头列表。

**返回值**: 可用摄像头列表

**示例**:
```java
CameraManager manager = new CameraManager();
List<Webcam> cameras = manager.getAvailableCameras();
for (Webcam cam : cameras) {
    System.out.println("Camera: " + cam.getName());
}
```

##### `public boolean selectCamera(Webcam camera)`
选择指定的摄像头进行使用。

**参数**:
- `camera` - 要选择的摄像头对象

**返回值**: 选择成功返回 `true`，失败返回 `false`

**示例**:
```java
Webcam cam = manager.getAvailableCameras().get(0);
if (manager.selectCamera(cam)) {
    System.out.println("Camera selected");
}
```

##### `public boolean startStreaming()`
启动摄像头视频流。

**返回值**: 启动成功返回 `true`，失败返回 `false`

**示例**:
```java
if (manager.startStreaming()) {
    System.out.println("Camera streaming started");
}
```

##### `public void stopStreaming()`
停止摄像头视频流。

**示例**:
```java
manager.stopStreaming();
```

##### `public BufferedImage captureFrame()`
捕获当前帧图像。

**返回值**: 捕获的图像，失败返回 `null`

**示例**:
```java
BufferedImage image = manager.captureFrame();
if (image != null) {
    ImageIO.write(image, "PNG", new File("capture.png"));
}
```

##### `public boolean setResolution(int width, int height)`
设置摄像头分辨率。

**参数**:
- `width` - 宽度（像素）
- `height` - 高度（像素）

**返回值**: 设置成功返回 `true`，失败返回 `false`

**示例**:
```java
manager.setResolution(1920, 1080);
```

##### `public Dimension[] getSupportedResolutions()`
获取当前摄像头支持的分辨率列表。

**返回值**: 分辨率数组

**示例**:
```java
Dimension[] resolutions = manager.getSupportedResolutions();
for (Dimension res : resolutions) {
    System.out.println(res.width + "x" + res.height);
}
```

##### `public void addFrameListener(FrameListener listener)`
添加帧监听器。

**参数**:
- `listener` - 帧监听器对象

**示例**:
```java
manager.addFrameListener(frame -> {
    System.out.println("Frame received: " + frame);
});
```

##### `public void removeFrameListener(FrameListener listener)`
移除帧监听器。

**参数**:
- `listener` - 要移除的帧监听器对象

---

### 1.2 ImageProcessor

#### 类概述
`ImageProcessor` 提供图像处理功能，包括天体检测、图像增强等。

#### 方法列表

##### `public static Mat bufferedImageToMat(BufferedImage image)`
将 BufferedImage 转换为 OpenCV Mat。

**参数**:
- `image` - 要转换的 BufferedImage

**返回值**: OpenCV Mat 对象

**示例**:
```java
BufferedImage image = manager.captureFrame();
Mat mat = ImageProcessor.bufferedImageToMat(image);
```

##### `public static BufferedImage matToBufferedImage(Mat mat)`
将 OpenCV Mat 转换为 BufferedImage。

**参数**:
- `mat` - 要转换的 OpenCV Mat

**返回值**: BufferedImage 对象

**示例**:
```java
BufferedImage image = ImageProcessor.matToBufferedImage(mat);
ImageIO.write(image, "PNG", new File("output.png"));
```

##### `public List<DetectedObject> detectCelestialObjects(Mat image, double threshold)`
检测图像中的天体对象。

**参数**:
- `image` - 输入图像（Mat 格式）
- `threshold` - 检测阈值（建议值：50-100）

**返回值**: 检测到的天体对象列表

**示例**:
```java
Mat image = ImageProcessor.bufferedImageToMat(capture);
List<DetectedObject> objects = processor.detectCelestialObjects(image, 50);
for (DetectedObject obj : objects) {
    System.out.println(obj);
}
```

**DetectedObject 属性**:
- `x` - X 坐标
- `y` - Y 坐标
- `area` - 面积
- `brightness` - 亮度

##### `public Mat enhanceForAstronomy(Mat image)`
对图像进行天文观测专用增强处理。

**参数**:
- `image` - 输入图像（Mat 格式）

**返回值**: 增强后的图像

**处理流程**:
1. 转换为 HSV 色彩空间
2. 直方图均衡化
3. 锐化处理

**示例**:
```java
Mat original = ImageProcessor.bufferedImageToMat(capture);
Mat enhanced = processor.enhanceForAstronomy(original);
```

##### `public boolean saveImage(BufferedImage image, String directory, String prefix)`
保存图像到指定目录。

**参数**:
- `image` - 要保存的图像
- `directory` - 目标目录路径
- `prefix` - 文件名前缀

**返回值**: 保存成功返回 `true`，失败返回 `false`

**文件名格式**: `{prefix}{timestamp}_{random}.png`

**示例**:
```java
boolean success = processor.saveImage(image, "captures", "moon_");
```

---

### 1.3 ImageStacker

#### 类概述
`ImageStacker` 提供图像叠加功能，用于模拟长曝光效果。

#### 构造函数

##### `public ImageStacker(int maxStackSize)`
创建图像堆栈。

**参数**:
- `maxStackSize` - 最大堆叠图像数量

**示例**:
```java
ImageStacker stacker = new ImageStacker(100);
```

#### 方法列表

##### `public boolean addImage(Mat image)`
添加图像到堆栈。

**参数**:
- `image` - 要添加的图像

**返回值**: 添加成功返回 `true`，失败返回 `false`

**行为**: 当堆栈满时，会自动移除最早的图像。

**示例**:
```java
Mat frame = ImageProcessor.bufferedImageToMat(capture);
stacker.addImage(frame);
```

##### `public Mat stack()`
执行平均叠加。

**返回值**: 叠加后的图像

**示例**:
```java
Mat result = stacker.stack();
```

##### `public Mat medianStack()`
执行中位数叠加（去除异常值）。

**返回值**: 叠加后的图像

**示例**:
```java
Mat result = stacker.medianStack();
```

##### `public Mat kappaSigmaStack(double kappa, double sigma)`
执行 Kappa-Sigma 叠加（最高质量）。

**参数**:
- `kappa` - Kappa 参数（建议值：2.0-3.0）
- `sigma` - Sigma 参数（建议值：1.0-1.5）

**返回值**: 叠加后的图像

**示例**:
```java
Mat result = stacker.kappaSigmaStack(2.5, 1.2);
```

##### `public double calculateSNR(Mat image)`
计算图像的信噪比。

**参数**:
- `image` - 输入图像

**返回值**: 信噪比值

**示例**:
```java
double snr = stacker.calculateSNR(image);
System.out.println("SNR: " + snr);
```

##### `public void clear()`
清空图像堆栈。

**示例**:
```java
stacker.clear();
```

##### `public int getCurrentStackSize()`
获取当前堆栈中的图像数量。

**返回值**: 当前堆栈大小

##### `public int getStackCount()`
获取总堆叠次数。

**返回值**: 总堆叠次数

---

### 1.4 UGreenCameraConfig

#### 类概述
`UGreenCameraConfig` 提供 UGREEN FHD 2K 摄像头的专用配置。

#### 枚举类型

##### `public enum AstronomyPreset`

天文观测预设配置。

**预设类型**:

| 预设 | 分辨率 | 曝光 | 增益 | 亮度 | 适用场景 |
|------|--------|------|------|------|----------|
| MOON | 1920x1080 | 50ms | 30 | 0.3 | 月球观测 |
| PLANET | 1920x1080 | 100ms | 50 | 0.5 | 行星观测 |
| DEEP_SKY | 2560x1440 | 200ms | 100 | 0.7 | 深空天体 |
| STARS | 1920x1080 | 150ms | 75 | 0.4 | 恒星观测 |
| SOLAR | 1920x1080 | 10ms | 1000 | 0.1 | 太阳观测（需滤镜）|

**示例**:
```java
UGreenCameraConfig.AstronomyPreset preset = UGreenCameraConfig.AstronomyPreset.MOON;
int exposure = preset.getExposure();
int gain = preset.getGain();
```

#### 静态方法

##### `public static Dimension getRecommendedResolution(String target)`
根据目标类型获取推荐分辨率。

**参数**:
- `target` - 目标类型（MOON, PLANET, DEEP_SKY, STARS, SOLAR）

**返回值**: 推荐分辨率

**示例**:
```java
Dimension res = UGreenCameraConfig.getRecommendedResolution("MOON");
System.out.println(res.width + "x" + res.height);
```

##### `public static boolean isUGreenCamera(String cameraName)`
检查是否为 UGREEN 摄像头。

**参数**:
- `cameraName` - 摄像头名称

**返回值**: 是 UGREEN 摄像头返回 `true`

**示例**:
```java
if (UGreenCameraConfig.isUGreenCamera("UGREEN FHD 2K Webcam")) {
    System.out.println("UGREEN camera detected");
}
```

---

### 1.5 ObservationSession

#### 类概述
`ObservationSession` 表示一次天文观测会话，记录观测过程的数据。

#### 构造函数

##### `public ObservationSession()`
创建新的观测会话，自动生成会话 ID。

#### 方法列表

##### `public String getId()`
获取会话 ID。

**返回值**: 会话 ID（格式：OBS_YYYYMMDD_HHmmss）

##### `public void setTargetName(String targetName)`
设置观测目标名称。

**参数**:
- `targetName` - 目标名称（如："月球", "火星"）

##### `public void incrementImageCount()`
增加图像计数。

**示例**:
```java
session.incrementImageCount();
```

##### `public void endSession()`
结束观测会话。

**示例**:
```java
session.endSession();
```

##### `public long getDurationSeconds()`
获取观测时长（秒）。

**返回值**: 观测时长

**示例**:
```java
long duration = session.getDurationSeconds();
System.out.println("观测时长: " + duration + " 秒");
```

---

## 2. 事件接口

### 2.1 FrameListener

```java
public interface FrameListener {
    void onFrameReceived(BufferedImage frame);
}
```

当接收到新的摄像头帧时调用。

**参数**:
- `frame` - 新接收的帧图像

**示例**:
```java
cameraManager.addFrameListener(frame -> {
    System.out.println("New frame: " + frame.getWidth() + "x" + frame.getHeight());
});
```

---

## 3. 常量定义

### 3.1 CameraParameters

```java
public static class CameraParameters {
    public static final int MIN_EXPOSURE = 10;      // 最小曝光 (ms)
    public static final int MAX_EXPOSURE = 1000;    // 最大曝光 (ms)
    public static final int MIN_GAIN = 0;           // 最小增益
    public static final int MAX_GAIN = 100;         // 最大增益
    public static final float MIN_BRIGHTNESS = 0.0f; // 最小亮度
    public static final float MAX_BRIGHTNESS = 1.0f;// 最大亮度
}
```

---

## 4. 使用示例

### 4.1 完整观测流程

```java
// 1. 初始化
CameraManager cameraManager = new CameraManager();
ImageProcessor imageProcessor = new ImageProcessor();
ImageStacker stacker = new ImageStacker(50);

// 2. 选择摄像头
List<Webcam> cameras = cameraManager.getAvailableCameras();
if (cameras.isEmpty()) {
    System.err.println("No camera found");
    return;
}

cameraManager.selectCamera(cameras.get(0));
cameraManager.setResolution(1920, 1080);

// 3. 启动摄像头
cameraManager.startStreaming();

// 4. 捕获并叠加图像
for (int i = 0; i < 50; i++) {
    BufferedImage frame = cameraManager.captureFrame();
    Mat mat = ImageProcessor.bufferedImageToMat(frame);
    stacker.addImage(mat);
    Thread.sleep(100);
}

// 5. 执行叠加
Mat stacked = stacker.stack();

// 6. 增强图像
Mat enhanced = imageProcessor.enhanceForAstronomy(stacked);

// 7. 检测天体
List<DetectedObject> objects = imageProcessor.detectCelestialObjects(enhanced, 50);
System.out.println("Detected " + objects.size() + " objects");

// 8. 保存结果
BufferedImage result = ImageProcessor.matToBufferedImage(enhanced);
imageProcessor.saveImage(result, "captures", "observation_");

// 9. 清理
cameraManager.stopStreaming();
stacker.clear();
```

### 4.2 使用天文预设

```java
// 应用月球预设
UGreenCameraConfig.AstronomyPreset preset = UGreenCameraConfig.AstronomyPreset.MOON;

cameraManager.setResolution(preset.getWidth(), preset.getHeight());
// 曝光和增益需要在摄像头层面设置
System.out.println("Recommended exposure: " + preset.getExposure() + "ms");
System.out.println("Recommended gain: " + preset.getGain());
```

---

## 5. 错误处理

### 5.1 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| NullPointerException | 摄像头未选择 | 先调用 selectCamera() |
| IllegalStateException | 摄像头未启动 | 先调用 startStreaming() |
| OutOfMemoryError | 图像过大 | 降低分辨率或增加内存 |

### 5.2 异常处理示例

```java
try {
    cameraManager.selectCamera(camera);
    cameraManager.startStreaming();
    BufferedImage frame = cameraManager.captureFrame();
    // 处理图像...
} catch (Exception e) {
    logger.error("Camera operation failed", e);
    cameraManager.stopStreaming();
}
```
