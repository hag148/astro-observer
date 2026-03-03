package com.astronomy.observer.image;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_photo;
import org.bytedeco.opencv.opencv_core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCV版本的图像处理器
 * 使用最新的 org.bytedeco:opencv-platform 依赖
 *
 * 优势：
 * - 性能更高
 * - 更多高级算法
 * - 更好的图像质量
 */
public class OpenCVImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVImageProcessor.class);

    static {
        // 加载OpenCV本地库
        try {
            opencv_core.randn(new Mat(), new Scalar(), new Scalar()); // 触发本地库加载
            logger.info("OpenCV loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load OpenCV", e);
            throw new RuntimeException("OpenCV initialization failed", e);
        }
    }

    /**
     * 转换BufferedImage到OpenCV Mat
     */
    public static Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) {
            return null;
        }

        // 转换为标准类型
        image = convertToStandardType(image);

        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), opencv_core.CV_8UC3);
        mat.data().put(pixels);

        // BGR到RGB转换
        Mat rgbMat = new Mat();
        opencv_imgproc.cvtColor(mat, rgbMat, opencv_imgproc.COLOR_BGR2RGB);

        mat.release();
        return rgbMat;
    }

    /**
     * 转换为标准图像类型
     */
    private static BufferedImage convertToStandardType(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return image;
        }

        BufferedImage converted = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(image, 0, 0, null);
        return converted;
    }

    /**
     * 转换OpenCV Mat到BufferedImage
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }

        Mat rgbMat = new Mat();
        opencv_imgproc.cvtColor(mat, rgbMat, opencv_imgproc.COLOR_BGR2RGB);

        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (rgbMat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }

        byte[] data = new byte[(int) (rgbMat.total() * rgbMat.channels())];
        rgbMat.data().get(data);

        BufferedImage image = new BufferedImage(
            rgbMat.cols(), rgbMat.rows(), type);
        image.getRaster().setDataElements(0, 0, rgbMat.cols(), rgbMat.rows(), data);

        rgbMat.release();
        return image;
    }

    /**
     * 检测天体对象（星星、行星等）
     */
    public List<DetectedObject> detectCelestialObjects(Mat image, double threshold) {
        List<DetectedObject> objects = new ArrayList<>();

        if (image == null || image.empty()) {
            return objects;
        }

        // 转换为灰度图
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 高斯模糊去噪
        Mat blurred = new Mat();
        opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // 自适应阈值处理
        Mat thresholded = new Mat();
        opencv_imgproc.adaptiveThreshold(blurred, thresholded, 255,
            opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            opencv_imgproc.THRESH_BINARY_INV, 11, 2);

        // 查找轮廓
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        opencv_imgproc.findContours(thresholded, contours, hierarchy,
            opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

        // 分析轮廓
        for (long i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);

            double area = opencv_imgproc.contourArea(contour);

            if (area > 10) { // 过滤小噪点
                Moments moments = opencv_imgproc.moments(contour);
                double cx = moments.m10() / moments.m00();
                double cy = moments.m01() / moments.m00();

                DetectedObject obj = new DetectedObject();
                obj.x = cx;
                obj.y = cy;
                obj.area = area;
                obj.brightness = getPixelBrightness(gray, (int) cx, (int) cy);
                objects.add(obj);
            }

            contour.release();
        }

        // 释放资源
        gray.release();
        blurred.release();
        thresholded.release();
        hierarchy.release();

        logger.info("Detected {} celestial objects", objects.size());
        return objects;
    }

    /**
     * 获取像素亮度
     */
    private static double getPixelBrightness(Mat gray, int x, int y) {
        if (x >= 0 && x < gray.cols() && y >= 0 && y < gray.rows()) {
            ByteBuffer buffer = gray.ptr(y).position(x).asByteBuffer();
            return buffer.get() & 0xFF;
        }
        return 0;
    }

    /**
     * 增强图像（用于天文观测）
     */
    public Mat enhanceForAstronomy(Mat image) {
        if (image == null || image.empty()) {
            return null;
        }

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
        
        opencv_imgproc.filter2D(enhanced, enhanced, -1, kernel, new Point(0, 0), 0.0,
            opencv_core.BORDER_DEFAULT, new Scalar());

        // 释放资源
        hsv.release();
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }
        kernel.release();

        return enhanced;
    }

    /**
     * 调整对比度
     */
    public Mat adjustContrast(Mat image, double alpha) {
        if (image == null || image.empty()) {
            return null;
        }

        Mat result = new Mat();
        image.convertTo(result, -1, alpha, 0.0);

        return result;
    }

    /**
     * 调整亮度
     */
    public Mat adjustBrightness(Mat image, double beta) {
        if (image == null || image.empty()) {
            return null;
        }

        Mat result = new Mat();
        image.convertTo(result, -1, 1.0, beta);

        return result;
    }

    /**
     * 自动白平衡
     */
    public Mat autoWhiteBalance(Mat image) {
        if (image == null || image.empty()) {
            return null;
        }

        Mat result = image.clone();

        // 使用灰度世界算法
        MatVector channels = new MatVector(3);
        opencv_core.split(image, channels);

        double[] means = new double[3];
        for (int i = 0; i < 3; i++) {
            Scalar mean = opencv_core.mean(channels.get(i));
            means[i] = mean.get(0);
        }

        double avgMean = (means[0] + means[1] + means[2]) / 3.0;

        for (int i = 0; i < 3; i++) {
            double alpha = avgMean / means[i];
            Mat scalarMat = new Mat(channels.get(i).rows(), channels.get(i).cols(), 
                channels.get(i).type(), new Scalar(alpha, alpha, alpha));
            opencv_core.multiply(channels.get(i), scalarMat, channels.get(i));
            scalarMat.release();
        }

        opencv_core.merge(channels, result);

        // 释放资源
        for (int i = 0; i < 3; i++) {
            channels.get(i).release();
        }

        return result;
    }

    /**
     * 降噪（非局部均值）
     */
    public Mat denoise(Mat image) {
        if (image == null || image.empty()) {
            return null;
        }

        Mat result = new Mat();
        opencv_photo.fastNlMeansDenoisingColored(image, result, 10.0, 10.0, 7, 21);

        return result;
    }

    /**
     * 保存图像
     */
    public boolean saveImage(Mat image, String directory, String prefix) {
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s%s_%s.png", prefix, timestamp,
                (int)(Math.random() * 1000));

            File outputFile = new File(dir, filename);
            opencv_imgcodecs.imwrite(outputFile.getAbsolutePath(), image);

            logger.info("Image saved to: {}", outputFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error("Failed to save image", e);
            return false;
        }
    }

    /**
     * 计算信噪比
     */
    public double calculateSNR(Mat image) {
        if (image == null || image.empty()) {
            return 0;
        }

        // 转换为灰度
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 计算均值和标准差
        Mat meanMat = new Mat();
        Mat stddevMat = new Mat();
        opencv_core.meanStdDev(gray, meanMat, stddevMat);

        double mean = meanMat.ptr().getDouble();
        double stddev = stddevMat.ptr().getDouble();

        // SNR = 20 * log10(mean / stddev)
        double snr = (stddev > 0) ? 20 * Math.log10(mean / stddev) : 0;

        // 释放资源
        gray.release();
        meanMat.release();
        stddevMat.release();

        return snr;
    }

    /**
     * 星点提取（Star extraction）
     */
    public Mat extractStars(Mat image, double threshold) {
        if (image == null || image.empty()) {
            return null;
        }

        // 转换为灰度
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 使用形态学变换突出星点
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Mat tophat = new Mat();
        opencv_imgproc.morphologyEx(gray, tophat, opencv_imgproc.MORPH_TOPHAT, kernel);

        // 应用阈值
        Mat thresholded = new Mat();
        opencv_imgproc.threshold(tophat, thresholded, threshold, 255, opencv_imgproc.THRESH_BINARY);

        // 释放资源
        gray.release();
        kernel.release();
        tophat.release();

        return thresholded;
    }

    /**
     * 图像配准（用于多帧叠加）
     */
    public Mat alignImages(Mat reference, Mat target) {
        if (reference == null || target == null || reference.empty() || target.empty()) {
            return null;
        }

        // 使用ECC算法进行配准
        Mat warpMatrix = new Mat(2, 3, opencv_core.CV_32F);
        Mat eyeMatrix = opencv_core.eye(2, 3, opencv_core.CV_32F).asMat();
        warpMatrix.put(eyeMatrix);
        eyeMatrix.release();
        
        Mat gray1 = new Mat();
        Mat gray2 = new Mat();

        opencv_imgproc.cvtColor(reference, gray1, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.cvtColor(target, gray2, opencv_imgproc.COLOR_BGR2GRAY);

        // 寻找变换矩阵 (4.13.0不支持MOTION_AFFINE，使用MOTION_TRANSLATION)
        double cc = opencv_imgproc.findTransformECC(gray1, gray2, warpMatrix,
            opencv_imgproc.MOTION_TRANSLATION, new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 50, 1e-6));

        if (cc < 0) {
            logger.warn("Image alignment failed");
            gray1.release();
            gray2.release();
            warpMatrix.release();
            return target.clone();
        }

        // 应用变换
        Mat aligned = new Mat();
        opencv_imgproc.warpAffine(target, aligned, warpMatrix, target.size());

        // 释放资源
        gray1.release();
        gray2.release();
        warpMatrix.release();

        return aligned;
    }

    /**
     * 释放Mat资源
     */
    public static void releaseMat(Mat... mats) {
        if (mats != null) {
            for (Mat mat : mats) {
                if (mat != null && !mat.empty()) {
                    mat.release();
                }
            }
        }
    }

    /**
     * 检测到的天体对象
     */
    public static class DetectedObject {
        public double x;
        public double y;
        public double area;
        public double brightness;

        @Override
        public String toString() {
            return String.format("Object at (%.1f, %.1f), area=%.1f, brightness=%.1f",
                x, y, area, brightness);
        }
    }
}
