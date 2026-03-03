# OpenCV 4.13.0 API变更修复指南

## 问题描述

将OpenCV从4.8.0-1.5.9升级到4.13.0-1.5.13后，出现了39个编译错误，主要是API不兼容问题。

## 已修复的问题

### 1. OpenCVImageProcessor.java

#### 修复的API变更：

1. **初始化库加载**
   - 旧: `opencv_core.noImage()`
   - 新: `opencv_core.randn(new Mat(), new Scalar(), new Scalar())`

2. **像素访问**
   - 旧: `gray.ptr(y).position(x).get(pixel)` 其中pixel是double[]
   - 新: 使用ByteBuffer
   ```java
   ByteBuffer buffer = gray.ptr(y).position(x).asByteBuffer();
   return buffer.get() & 0xFF;
   ```

3. **核填充**
   - 旧: `kernel.ptr(0).put(new float[]{...})`
   - 新: 使用FloatBuffer
   ```java
   FloatBuffer kernelBuffer = kernel.createBuffer();
   kernelBuffer.put(0, 0.0f);
   ```

4. **filter2D参数**
   - 旧: `filter2D(src, dst, ddepth, kernel, new double[]{0}, 0, borderType)`
   - 新: `filter2D(src, dst, ddepth, kernel, new Point(0, 0), 0.0, borderType, new Scalar())`

5. **multiply操作**
   - 旧: `opencv_core.multiply(mat, Scalar.all(alpha), dst)`
   - 新: 需要创建Mat作为第二个参数
   ```java
   Mat scalarMat = new Mat(rows, cols, type, new Scalar(alpha, alpha, alpha));
   opencv_core.multiply(mat, scalarMat, dst);
   scalarMat.release();
   ```

6. **降噪函数**
   - 旧: `opencv_imgproc.fastNlMeansDenoisingColored(...)`
   - 新: 移到`opencv_photo`包
   ```java
   import org.bytedeco.opencv.global.opencv_photo;
   opencv_photo.fastNlMeansDenoisingColored(...);
   ```

7. **Mat.eye返回类型**
   - 旧: `Mat.eye()`返回Mat
   - 新: `Mat.eye()`返回MatExpr，需要`asMat()`

8. **图像配准**
   - 旧: `opencv_imgproc.MOTION_AFFINE`
   - 新: 使用`opencv_imgproc.MOTION_TRANSLATION`（4.13.0不支持AFFINE）

9. **TermCriteria常量**
   - 旧: `opencv_core.TERM_CRITERIA_EPS`, `opencv_core.TERM_CRITERIA_COUNT`
   - 新: 使用`TermCriteria.EPS`, `TermCriteria.MAX_ITER`

### 2. OpenCVImageStacker.java

#### 修复的API变更：

1. **divide操作**
   - 旧: `opencv_core.divide(result, Scalar.all(n), result)`
   - 新: 创建Mat作为第二个参数
   ```java
   Mat divisorMat = new Mat(1, 1, type, new Scalar(n));
   opencv_core.divide(result, divisorMat, result);
   divisorMat.release();
   ```

2. **像素访问和设置**
   - 使用ByteBuffer代替直接put/get
   ```java
   // 获取
   double value = getPixelValue(mat, x, y, c);
   
   // 设置
   setPixelValue(mat, x, y, c, value);
   ```

3. **Mat.eye方法**
   - 旧: `opencv_core.eye(3, 3, CV_32F)`
   - 新: `new Mat(2, 3, CV_32F)` + `setIdentity`

### 3. CameraManager.java

#### 修复的API变更：

1. **WebcamDiscoveryListener**
   - 新增方法: `webcamGone(WebcamEvent we)`

2. **分辨率设置**
   - 旧: `getCustomSizes()`, `setCustomSize()`
   - 新: `getViewSizes()`, `setViewSize()`

## 待修复的问题

### GUI文件（AdvancedAstronomyFrame.java, AstronomyFrame.java）

这些文件有以下问题：

1. **导入了错误的Mat类型**
   - 错误: `org.opencv.core.Mat`
   - 正确: `org.bytedeco.opencv.opencv_core.Mat`

2. **调用了不存在的方法**
   - `ImageProcessor.bufferedImageToMat()` - 纯Java版本没有此方法
   - `ImageProcessor.matToBufferedImage()` - 纯Java版本没有此方法
   - `ImageStacker.getCurrentStackSize()` - 应该是`getStackSize()`

3. **类型不匹配**
   - 尝试将BufferedImage转换为Mat，反之亦然

### 修复建议

有两个解决方案：

#### 方案1：使用OpenCV版本（推荐）

在GUI文件中：
```java
// 导入正确的Mat类型
import org.bytedeco.opencv.opencv_core.Mat;

// 使用OpenCVImageProcessor和OpenCVImageStacker
private OpenCVImageProcessor imageProcessor = new OpenCVImageProcessor();
private OpenCVImageStacker imageStacker = new OpenCVImageStacker(maxSize);

// 转换BufferedImage到Mat
Mat mat = OpenCVImageProcessor.bufferedImageToMat(image);
Mat enhanced = imageProcessor.enhanceForAstronomy(mat);
BufferedImage result = OpenCVImageProcessor.matToBufferedImage(enhanced);
```

#### 方案2：使用纯Java版本

如果不想使用OpenCV，修改GUI文件使用纯Java版本：
```java
// 使用纯Java版本的ImageProcessor和ImageStacker
private ImageProcessor imageProcessor = new ImageProcessor();
private ImageStacker imageStacker = new ImageStacker(maxSize);

// 直接使用BufferedImage，无需转换
BufferedImage enhanced = imageProcessor.enhanceForAstronomy(image);
```

## 编译命令

```bash
cd d:\CodeBuddy\astro-observer
mvn clean compile
```

## 测试建议

1. 单元测试ImageProcessor和ImageStacker
2. 测试摄像头捕获功能
3. 测试图像叠加功能
4. 测试图像增强功能

## 总结

✅ 已修复: OpenCVImageProcessor, OpenCVImageStacker, CameraManager  
⚠️ 待修复: AdvancedAstronomyFrame, AstronomyFrame（GUI文件）

建议使用方案1（OpenCV版本）以获得最佳性能和功能。
