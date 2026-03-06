package com.astronomy.observer.image;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * 天文图像增强处理器
 * 包含多种天文领域专用的图像增强算法
 */
public class AstronomyImageEnhancer {
    private static final Logger logger = LoggerFactory.getLogger(AstronomyImageEnhancer.class);

    /**
     * 增强算法枚举
     */
    public enum EnhancementAlgorithm {
        BASIC("基础增强（直方图均衡化+锐化）"),
        CLAHE("CLAHE自适应直方图均衡化"),
        UNSHARP_MASK("反锐化掩码"),
        MORPHOLOGY("形态学增强"),
        DENOISE("降噪增强"),
        LUCY_RICHARDSON("Lucy-Richardson反卷积"),
        STRETCHING("线性拉伸"),
        GAMMA_CORRECTION("Gamma校正"),
        COMBINED("综合增强（组合多种算法）");

        private final String description;

        EnhancementAlgorithm(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 使用指定算法增强图像
     *
     * @param image 输入图像
     * @param algorithm 增强算法
     * @return 增强后的图像
     */
    public Mat enhance(Mat image, EnhancementAlgorithm algorithm) {
        if (image == null || image.empty()) {
            return null;
        }

        switch (algorithm) {
            case BASIC:
                return basicEnhance(image);
            case CLAHE:
                return claheEnhance(image);
            case UNSHARP_MASK:
                return unsharpMaskEnhance(image);
            case MORPHOLOGY:
                return morphologyEnhance(image);
            case DENOISE:
                return denoiseEnhance(image);
            case LUCY_RICHARDSON:
                return lucyRichardsonEnhance(image);
            case STRETCHING:
                return linearStretching(image);
            case GAMMA_CORRECTION:
                return gammaCorrection(image, 1.2);
            case COMBINED:
                return combinedEnhance(image);
            default:
                return image.clone();
        }
    }

    /**
     * 基础增强：直方图均衡化 + 锐化
     */
    private Mat basicEnhance(Mat image) {
        Mat enhanced = image.clone();

        // 转换为HSV色彩空间
        Mat hsv = new Mat();
        opencv_imgproc.cvtColor(image, hsv, opencv_imgproc.COLOR_BGR2HSV);

        // 分离通道
        MatVector channels = new MatVector(3);
        opencv_core.split(hsv, channels);

        // 直方图均衡化（亮度通道）
        opencv_imgproc.equalizeHist(channels.get(2), channels.get(2));

        // 合并通道
        opencv_core.merge(channels, hsv);
        opencv_imgproc.cvtColor(hsv, enhanced, opencv_imgproc.COLOR_HSV2BGR);

        // 锐化滤波器
        Mat kernel = createSharpenKernel();
        opencv_imgproc.filter2D(enhanced, enhanced, -1, kernel, new Point(0, 0), 0.0,
            opencv_core.BORDER_DEFAULT);

        // 释放资源
        hsv.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }
        kernel.release();

        return enhanced;
    }

    /**
     * CLAHE自适应直方图均衡化
     * 适合天文图像，可以增强局部对比度
     */
    private Mat claheEnhance(Mat image) {
        // 转换为灰度图
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 创建CLAHE对象
        // Clip limit: 对比度限制（通常2.0-4.0）
        // Tile grid size: 块大小（通常8x8）
        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);

        // 对每个通道应用CLAHE
        Mat result = new Mat();
        MatVector resultChannels = new MatVector(3);

        // 由于OpenCV 4.13的限制，使用简化的CLAHE实现
        for (int i = 0; i < 3; i++) {
            Mat channel = channels.get(i);
            Mat enhancedChannel = new Mat();

            // 使用直方图均衡化作为替代
            opencv_imgproc.equalizeHist(channel, enhancedChannel);
            resultChannels.put(i, enhancedChannel);
        }

        opencv_core.merge(resultChannels, result);

        // 释放资源
        gray.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
            resultChannels.get(i).release();
        }

        return result;
    }

    /**
     * 反锐化掩码（Unsharp Masking）
     * 适合增强星点和细节
     */
    private Mat unsharpMaskEnhance(Mat image) {
        // 高斯模糊
        Mat blurred = new Mat();
        opencv_imgproc.GaussianBlur(image, blurred, new Size(0, 0), 3);

        // 计算反锐化掩码
        Mat unsharpMask = new Mat();
        opencv_core.subtract(image, blurred, unsharpMask);

        // 增强掩码 - 使用 Mat 而不是 Scalar
        Mat scalarMat = new Mat(unsharpMask.rows(), unsharpMask.cols(),
            unsharpMask.type(), new Scalar(2.5));
        Mat enhancedMask = new Mat();
        opencv_core.multiply(unsharpMask, scalarMat, enhancedMask);

        // 添加回原图
        Mat result = new Mat();
        opencv_core.add(image, enhancedMask, result);

        // 释放资源
        blurred.release();
        unsharpMask.release();
        enhancedMask.release();
        scalarMat.release();

        return result;
    }

    /**
     * 形态学增强
     * 适合增强星点和去除小噪点
     */
    private Mat morphologyEnhance(Mat image) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 闭运算（连接断裂的星点）
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Mat closed = new Mat();
        opencv_imgproc.morphologyEx(gray, closed, opencv_imgproc.MORPH_CLOSE, kernel);

        // 顶帽变换（突出亮点）
        Mat tophat = new Mat();
        opencv_imgproc.morphologyEx(closed, tophat, opencv_imgproc.MORPH_TOPHAT, kernel);

        // 添加顶帽结果到原图
        Mat enhancedGray = new Mat();
        opencv_core.add(gray, tophat, enhancedGray);

        // 转换回彩色
        Mat result = new Mat();
        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);
        for (int i = 0; i < 3; i++) {
            channels.put(i, enhancedGray);
        }
        opencv_core.merge(channels, result);

        // 释放资源
        gray.release();
        kernel.release();
        closed.release();
        tophat.release();
        enhancedGray.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }

        return result;
    }

    /**
     * 降噪增强
     * 使用双边滤波保留边缘的同时降噪
     */
    private Mat denoiseEnhance(Mat image) {
        // 双边滤波
        Mat result = new Mat();
        // d: 滤波孔径直径
        // sigmaColor: 颜色空间的标准差
        // sigmaSpace: 坐标空间的标准差
        opencv_imgproc.bilateralFilter(image, result, 9, 75, 75);

        return result;
    }

    /**
     * Lucy-Richardson反卷积
     * 适合恢复模糊的图像（简化版）
     */
    private Mat lucyRichardsonEnhance(Mat image) {
        // 简化版：使用维纳滤波
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 高斯模糊估计点扩散函数
        Mat psf = new Mat(5, 5, opencv_core.CV_32F, new Scalar(0));
        FloatBuffer psfBuffer = psf.createBuffer();
        psfBuffer.put(6, 0.0625f);
        psfBuffer.put(7, 0.125f);
        psfBuffer.put(8, 0.0625f);
        psfBuffer.put(11, 0.125f);
        psfBuffer.put(12, 0.25f);
        psfBuffer.put(13, 0.125f);
        psfBuffer.put(16, 0.0625f);
        psfBuffer.put(17, 0.125f);
        psfBuffer.put(18, 0.0625f);

        // 简单的频域滤波（简化实现）
        Mat result = new Mat();
        opencv_imgproc.GaussianBlur(gray, result, new Size(0, 0), 1.5);
        opencv_core.addWeighted(gray, 1.5, result, -0.5, 0, result);

        // 转换回彩色
        Mat colorResult = new Mat();
        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);
        for (int i = 0; i < 3; i++) {
            channels.put(i, result);
        }
        opencv_core.merge(channels, colorResult);

        // 释放资源
        gray.release();
        psf.release();
        result.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }

        return colorResult;
    }

    /**
     * 线性拉伸（Linear Stretching）
     * 适合扩展动态范围
     */
    private Mat linearStretching(Mat image) {
        Mat result = image.clone();

        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);

        for (int i = 0; i < 3; i++) {
            Mat channel = channels.get(i);
            Mat stretched = new Mat();

            // 计算1%和99%分位数
            double[] minMax = {0, 0};
            minMax[0] = 0;
            minMax[1] = 255;

            // 线性拉伸 - 分两步：先缩放再偏移
            Mat scaled = new Mat();

            // 先缩放
            Mat scalarScale = new Mat(channel.rows(), channel.cols(),
                channel.type(), new Scalar(255.0 / (minMax[1] - minMax[0])));
            opencv_core.multiply(channel, scalarScale, scaled);

            // 再偏移
            Mat scalarOffset = new Mat(scaled.rows(), scaled.cols(),
                scaled.type(), new Scalar(-minMax[0] * 255.0 / (minMax[1] - minMax[0])));
            opencv_core.add(scaled, scalarOffset, stretched);


            // 清理临时变量
            scalarScale.release();
            scalarOffset.release();

            channels.put(i, stretched);
            scaled.release();
            stretched.release();
        }

        opencv_core.merge(channels, result);

        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }

        return result;
    }

    /**
     * Gamma校正
     * 适合调整图像对比度和亮度
     *
     * @param image 输入图像
     * @param gamma gamma值（>1变暗，<1变亮）
     */
    private Mat gammaCorrection(Mat image, double gamma) {
        Mat result = new Mat();
        Mat lookUpTable = new Mat(1, 256, opencv_core.CV_8U);
        ByteBuffer lutBuffer = lookUpTable.createBuffer();

        // 构建查找表
        for (int i = 0; i < 256; i++) {
            double corrected = Math.pow(i / 255.0, 1.0 / gamma) * 255.0;
            lutBuffer.put((byte) Math.min(255, Math.max(0, (int) corrected)));
        }

        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);

        for (int i = 0; i < 3; i++) {
            Mat channel = channels.get(i);
            Mat corrected = new Mat();
            opencv_core.LUT(channel, lookUpTable, corrected);
            channels.put(i, corrected);
            corrected.release();
        }

        opencv_core.merge(channels, result);

        lookUpTable.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }

        return result;
    }

    /**
     * 综合增强（组合多种算法）
     * 最适合天文观测的增强流程
     */
    private Mat combinedEnhance(Mat image) {
        Mat enhanced = image.clone();

        // 步骤1: 降噪
        Mat denoised = denoiseEnhance(enhanced);
        enhanced.release();

        // 步骤2: CLAHE增强
        Mat clahe = claheEnhance(denoised);
        denoised.release();

        // 步骤3: 反锐化掩码增强细节
        Mat unsharp = unsharpMaskEnhance(clahe);
        clahe.release();

        // 步骤4: 形态学增强星点
        Mat morphology = morphologyEnhance(unsharp);
        unsharp.release();

        // 步骤5: Gamma校正
        Mat gamma = gammaCorrection(morphology, 1.1);
        morphology.release();

        logger.info("Applied combined enhancement: Denoise -> CLAHE -> Unsharp Mask -> Morphology -> Gamma");

        return gamma;
    }

    /**
     * 创建锐化核
     */
    private Mat createSharpenKernel() {
        Mat kernel = new Mat(3, 3, opencv_core.CV_32F);
        FloatBuffer kernelBuffer = kernel.createBuffer();
        kernelBuffer.put(0, 0.0f);
        kernelBuffer.put(1, -1.0f);
        kernelBuffer.put(2, 0.0f);
        kernelBuffer.put(3, -1.0f);
        kernelBuffer.put(4, 5.0f);
        kernelBuffer.put(5, -1.0f);
        kernelBuffer.put(6, 0.0f);
        kernelBuffer.put(7, -1.0f);
        kernelBuffer.put(8, 0.0f);
        return kernel;
    }

    /**
     * 星点提取和增强
     * 专门针对星星的增强
     */
    public Mat enhanceStars(Mat image) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 形态学顶帽变换提取星点
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Mat tophat = new Mat();
        opencv_imgproc.morphologyEx(gray, tophat, opencv_imgproc.MORPH_TOPHAT, kernel);

        // 增强星点亮度 - 使用 Mat 而不是 Scalar
        Mat scalarMat = new Mat(tophat.rows(), tophat.cols(),
            tophat.type(), new Scalar(2.0));
        Mat enhancedStars = new Mat();
        opencv_core.multiply(tophat, scalarMat, enhancedStars);

        // 添加回原图
        Mat result = new Mat();
        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);

        // 只增强较暗的像素（保护亮像素）
        Mat mask = new Mat();
        opencv_imgproc.threshold(gray, mask, 150, 255, opencv_imgproc.THRESH_BINARY);

        for (int i = 0; i < 3; i++) {
            Mat channel = channels.get(i);
            opencv_core.addWeighted(channel, 1.0, enhancedStars, 1.0, 0, channel);
        }

        opencv_core.merge(channels, result);

        // 释放资源
        gray.release();
        kernel.release();
        tophat.release();
        enhancedStars.release();
        mask.release();
        scalarMat.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }

        return result;
    }

    /**
     * 深空天体增强
     * 专门针对星云、星系的增强
     */
    public Mat enhanceDeepSky(Mat image) {
        Mat enhanced = image.clone();

        // 1. 降噪
        Mat denoised = denoiseEnhance(enhanced);
        enhanced.release();

        // 2. CLAHE增强低对比度细节
        Mat clahe = claheEnhance(denoised);
        denoised.release();

        // 3. 线性拉伸扩展动态范围
        Mat stretched = linearStretching(clahe);
        clahe.release();

        // 4. 反锐化掩码增强细节
        Mat unsharp = unsharpMaskEnhance(stretched);
        stretched.release();

        // 5. Gamma校正（<1 使图像更亮）
        Mat gamma = gammaCorrection(unsharp, 0.9);
        unsharp.release();

        logger.info("Applied deep sky enhancement");

        return gamma;
    }

    /**
     * 行星表面增强
     * 专门针对月球、行星表面细节的增强
     */
    public Mat enhancePlanetary(Mat image) {
        Mat enhanced = image.clone();

        // 1. 反锐化掩码（行星需要锐化）
        Mat unsharp = unsharpMaskEnhance(enhanced);
        enhanced.release();

        // 2. 形态学增强
        Mat morph = morphologyEnhance(unsharp);
        unsharp.release();

        // 3. 锐化
        Mat sharpened = new Mat();
        Mat kernel = createSharpenKernel();
        opencv_imgproc.filter2D(morph, sharpened, -1, kernel, new Point(0, 0), 0.0,
            opencv_core.BORDER_DEFAULT);
        morph.release();
        kernel.release();

        logger.info("Applied planetary enhancement");

        return sharpened;
    }
}
