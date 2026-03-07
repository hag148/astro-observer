# GUI修复说明

## 问题分析

GUI文件（AdvancedAstronomyFrame.java, AstronomyFrame.java）目前存在以下问题：

1. 调用了不存在的方法：
   - `getCurrentStackSize()` - 应该是 `getStackSize()`
   - `bufferedImageToMat()` - 只有OpenCV版本有
   - `matToBufferedImage()` - 只有OpenCV版本有

2. 类型不匹配：
   - GUI使用纯Java的ImageProcessor/ImageStacker
   - 但尝试使用BufferedImage和Mat之间的转换

## 修复方案

由于GUI已经使用纯Java版本的ImageProcessor和ImageStacker，只需修复方法调用错误：

### 方法1：快速修复（推荐）

只修改方法名，不改变功能：

```java
// 将 getCurrentStackSize() 改为 getStackSize()
int size = imageStacker.getStackSize();
```

### 方法2：添加OpenCV支持（高级）

如果需要使用OpenCV的高级功能：

1. 在GUI中添加OpenCV版本的支持
2. 添加配置选项选择使用哪个版本
3. 根据选择调用相应的处理器

### 建议的修复步骤

#### AdvancedAstronomyFrame.java

需要修改的方法：

1. **第389行** - 修改方法名
```java
// 错误:
// int size = imageStacker.getCurrentStackSize();

// 正确:
int size = imageStacker.getStackSize();
```

2. **第385-387行, 411-413行, 445-447行, 476-478行**
   - 这些代码试图使用OpenCV的Mat转换
   - 由于使用纯Java版本，应该直接使用BufferedImage
   - 建议注释掉或删除这些OpenCV特定的代码

#### AstronomyFrame.java

类似的问题需要修复：
- 第233-235行, 第265-267行 - OpenCV Mat转换代码

## 推荐的修复方法

对于纯Java版本，GUI代码应该这样修改：

```java
// 图像增强（纯Java版本）
BufferedImage enhanced = imageProcessor.enhanceForAstronomy(currentImage);
isEnhanced = true;
updateCameraLabel(enhanced);

// 图像叠加（纯Java版本）
BufferedImage stacked = imageStacker.stack();
if (stacked != null) {
    updateCameraLabel(stacked);
}
```

## 完整的修复示例

### ImageStacker.java 添加getCurrentStackSize方法

为了保持兼容性，可以在ImageStacker中添加：

```java
/**
 * 获取当前堆栈大小（别名方法，保持向后兼容）
 */
@Deprecated
public int getCurrentStackSize() {
    return getStackSize();
}
```

## 注意事项

1. **纯Java版本** - 不需要Mat转换
2. **OpenCV版本** - 需要Mat转换，使用OpenCVImageProcessor和OpenCVImageStacker
3. 不要混用两种版本的类

## 下一步

建议用户：
1. 如果只需要基本功能，使用纯Java版本（已修复）
2. 如果需要高级功能（如星点检测、图像配准），使用OpenCV版本
3. 可以添加配置选项在两个版本之间切换
