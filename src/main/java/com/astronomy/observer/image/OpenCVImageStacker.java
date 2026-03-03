package com.astronomy.observer.image;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OpenCV版本的图像叠加处理器
 * 用于模拟长时间曝光，提供更高质量的叠加算法
 *
 * 优势：
 * - 性能更高（使用原生库）
 * - 支持图像配准（alignment）
 * - 更精确的信噪比计算
 * - 更好的内存管理
 */
public class OpenCVImageStacker {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVImageStacker.class);

    private final List<Mat> stackedImages;
    private final int maxStackSize;
    private int stackCount;

    public OpenCVImageStacker(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        this.stackedImages = new ArrayList<>();
        this.stackCount = 0;
    }

    /**
     * 添加图像到堆栈
     */
    public boolean addImage(Mat image) {
        if (image == null || image.empty()) {
            logger.warn("Cannot add null or empty image to stack");
            return false;
        }

        if (stackedImages.size() >= maxStackSize) {
            // 释放最早添加的图像
            stackedImages.get(0).release();
            stackedImages.remove(0);
        }

        // 克隆图像以避免引用问题
        Mat clone = image.clone();
        stackedImages.add(clone);
        stackCount++;

        logger.debug("Added image to stack. Stack size: {}/{}", stackedImages.size(), maxStackSize);
        return true;
    }

    /**
     * 执行图像叠加（平均叠加）
     */
    public Mat stack() {
        return stack(StackMethod.AVERAGE);
    }

    /**
     * 执行指定方法的图像叠加
     */
    public Mat stack(StackMethod method) {
        if (stackedImages.isEmpty()) {
            logger.warn("No images to stack");
            return null;
        }

        Mat result;
        switch (method) {
            case AVERAGE:
                result = averageStack();
                break;
            case MEDIAN:
                result = medianStack();
                break;
            case KAPPA_SIGMA:
                result = kappaSigmaStack(2.0);
                break;
            case AVERAGE_WITH_ALIGNMENT:
                result = averageStackWithAlignment();
                break;
            default:
                result = averageStack();
        }

        logger.info("Stacked {} images using {} method", stackedImages.size(), method);
        return result;
    }

    /**
     * 平均叠加
     */
    private Mat averageStack() {
        int width = stackedImages.get(0).cols();
        int height = stackedImages.get(0).rows();
        int type = stackedImages.get(0).type();

        Mat result = Mat.zeros(height, width, type).asMat();

        // 累加所有图像
        for (Mat image : stackedImages) {
            opencv_core.add(result, image, result);
        }

        // 除以图像数量
        opencv_core.divide(result, Scalar.all(stackedImages.size()), result);

        return result;
    }

    /**
     * 中位数叠加（去除异常值）
     */
    private Mat medianStack() {
        int width = stackedImages.get(0).cols();
        int height = stackedImages.get(0).rows();
        int type = stackedImages.get(0).type();
        int channels = stackedImages.get(0).channels();

        Mat result = new Mat(height, width, type);

        // 对每个像素计算中位数
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    double[] values = new double[stackedImages.size()];
                    for (int i = 0; i < stackedImages.size(); i++) {
                        values[i] = stackedImages.get(i).ptr(y, c).getDouble(x);
                    }
                    double median = computeMedian(values);
                    result.ptr(y, c).put(x, median);
                }
            }
        }

        return result;
    }

    /**
     * Kappa-Sigma叠加（高质量叠加，去除极端异常值）
     */
    private Mat kappaSigmaStack(double kappa) {
        int width = stackedImages.get(0).cols();
        int height = stackedImages.get(0).rows();
        int type = stackedImages.get(0).type();
        int channels = stackedImages.get(0).channels();

        Mat result = new Mat(height, width, type);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    // 收集所有值
                    double[] values = new double[stackedImages.size()];
                    for (int i = 0; i < stackedImages.size(); i++) {
                        values[i] = stackedImages.get(i).ptr(y, c).getDouble(x);
                    }

                    // 应用Kappa-Sigma算法
                    double filteredValue = applyKappaSigma(values, kappa);
                    result.ptr(y, c).put(x, filteredValue);
                }
            }
        }

        return result;
    }

    /**
     * 应用Kappa-Sigma算法
     */
    private double applyKappaSigma(double[] values, double kappa) {
        if (values.length == 0) {
            return 0;
        }

        List<Double> filtered = new ArrayList<>();
        for (double value : values) {
            filtered.add(value);
        }

        boolean changed;
        int maxIterations = 10;

        do {
            changed = false;

            // 计算均值和标准差
            double mean = calculateMean(filtered);
            double stdDev = calculateStdDev(filtered, mean);

            // 去除超出kappa*stdDev范围的值
            List<Double> newFiltered = new ArrayList<>();
            double threshold = kappa * stdDev;

            for (Double value : filtered) {
                double deviation = Math.abs(value - mean);
                if (deviation <= threshold) {
                    newFiltered.add(value);
                } else {
                    changed = true;
                }
            }

            filtered = newFiltered;
            maxIterations--;

        } while (changed && maxIterations > 0 && !filtered.isEmpty());

        // 返回剩余值的平均值
        return calculateMean(filtered);
    }

    /**
     * 带配准的平均叠加（最高质量）
     * 首先对齐所有图像，然后进行平均叠加
     */
    private Mat averageStackWithAlignment() {
        if (stackedImages.size() < 2) {
            return stackedImages.get(0).clone();
        }

        Mat reference = stackedImages.get(0);
        List<Mat> alignedImages = new ArrayList<>();

        // 添加参考图像
        alignedImages.add(reference.clone());

        // 对齐其他图像
        for (int i = 1; i < stackedImages.size(); i++) {
            Mat aligned = alignImage(reference, stackedImages.get(i));
            if (aligned != null) {
                alignedImages.add(aligned);
            } else {
                // 如果配准失败，添加原始图像
                alignedImages.add(stackedImages.get(i).clone());
            }
        }

        // 计算对齐后的平均值
        int width = reference.cols();
        int height = reference.rows();
        int type = reference.type();

        Mat result = Mat.zeros(height, width, type).asMat();

        for (Mat image : alignedImages) {
            opencv_core.add(result, image, result);
        }

        opencv_core.divide(result, Scalar.all(alignedImages.size()), result);

        // 释放临时图像
        for (Mat image : alignedImages) {
            image.release();
        }

        return result;
    }

    /**
     * 图像配准（使用ECC算法）
     */
    private Mat alignImage(Mat reference, Mat target) {
        // 转换为灰度
        Mat gray1 = new Mat();
        Mat gray2 = new Mat();
        opencv_imgproc.cvtColor(reference, gray1, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.cvtColor(target, gray2, opencv_imgproc.COLOR_BGR2GRAY);

        // 定义变换矩阵
        Mat warpMatrix = opencv_core.eye(3, 3, opencv_core.CV_32F).asMat();

        // 使用ECC算法寻找变换
        double cc = opencv_imgproc.findTransformECC(gray1, gray2, warpMatrix,
            opencv_imgproc.MOTION_AFFINE,
            new TermCriteria(opencv_core.TERM_CRITERIA_EPS | opencv_core.TERM_CRITERIA_COUNT,
                            50, 1e-6));

        gray1.release();
        gray2.release();

        if (cc < 0) {
            logger.warn("Image alignment failed for frame");
            warpMatrix.release();
            return null;
        }

        // 应用变换
        Mat aligned = new Mat();
        opencv_imgproc.warpAffine(target, aligned, warpMatrix, target.size());

        warpMatrix.release();

        return aligned;
    }

    /**
     * 计算中位数
     */
    private double computeMedian(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);

        if (sorted.length % 2 == 0) {
            return (sorted[sorted.length / 2] + sorted[sorted.length / 2 - 1]) / 2.0;
        } else {
            return sorted[sorted.length / 2];
        }
    }

    /**
     * 计算均值
     */
    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (Double value : values) {
            sum += value;
        }

        return sum / values.size();
    }

    /**
     * 计算标准差
     */
    private double calculateStdDev(List<Double> values, double mean) {
        if (values.isEmpty() || values.size() == 1) {
            return 0;
        }

        double sumSquares = 0;
        for (Double value : values) {
            double diff = value - mean;
            sumSquares += diff * diff;
        }

        return Math.sqrt(sumSquares / values.size());
    }

    /**
     * 清空堆栈并释放资源
     */
    public void clear() {
        for (Mat mat : stackedImages) {
            mat.release();
        }
        stackedImages.clear();
        stackCount = 0;
        logger.info("Stack cleared");
    }

    /**
     * 获取堆栈大小
     */
    public int getStackSize() {
        return stackedImages.size();
    }

    /**
     * 获取已添加的图像总数
     */
    public int getStackCount() {
        return stackCount;
    }

    /**
     * 获取最大堆栈大小
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * 计算堆栈后的信噪比提升
     */
    public double getSNRImprovement() {
        if (stackedImages.size() < 2) {
            return 0;
        }

        // 理论上，平均N帧图像可以提高sqrt(N)倍的信噪比
        return Math.sqrt(stackedImages.size());
    }

    /**
     * 计算堆栈的实际信噪比（需要先执行stack()方法）
     */
    public double calculateStackedSNR(Mat stackedImage) {
        if (stackedImage == null || stackedImage.empty()) {
            return 0;
        }

        // 转换为灰度
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(stackedImage, gray, opencv_imgproc.COLOR_BGR2GRAY);

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
     * 获取堆栈中所有图像的统计信息
     */
    public String getStatistics() {
        if (stackedImages.isEmpty()) {
            return "Stack is empty";
        }

        double[] totalSNR = new double[stackedImages.size()];
        for (int i = 0; i < stackedImages.size(); i++) {
            totalSNR[i] = calculateSingleImageSNR(stackedImages.get(i));
        }

        double avgSNR = Arrays.stream(totalSNR).average().orElse(0);
        double maxSNR = Arrays.stream(totalSNR).max().orElse(0);
        double minSNR = Arrays.stream(totalSNR).min().orElse(0);

        return String.format(
            "Stack: %d/%d images, %d total added\n" +
            "Single image SNR: Avg=%.2f dB, Min=%.2f dB, Max=%.2f dB\n" +
            "Expected SNR improvement: %.2fx",
            getStackSize(), getMaxStackSize(), getStackCount(),
            avgSNR, minSNR, maxSNR, getSNRImprovement());
    }

    /**
     * 计算单张图像的信噪比
     */
    private double calculateSingleImageSNR(Mat image) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        Mat meanMat = new Mat();
        Mat stddevMat = new Mat();
        opencv_core.meanStdDev(gray, meanMat, stddevMat);

        double mean = meanMat.ptr().getDouble();
        double stddev = stddevMat.ptr().getDouble();

        double snr = (stddev > 0) ? 20 * Math.log10(mean / stddev) : 0;

        gray.release();
        meanMat.release();
        stddevMat.release();

        return snr;
    }

    /**
     * 堆叠方法枚举
     */
    public enum StackMethod {
        AVERAGE("平均叠加"),
        MEDIAN("中位数叠加"),
        KAPPA_SIGMA("Kappa-Sigma叠加"),
        AVERAGE_WITH_ALIGNMENT("带配准的平均叠加");

        private final String displayName;

        StackMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * 获取堆栈信息
     */
    public String getStackInfo() {
        return String.format("Stack: %d/%d images, %d total added, SNR improvement: %.2fx",
            getStackSize(), getMaxStackSize(), getStackCount(), getSNRImprovement());
    }

    /**
     * 资源清理
     */
    public void dispose() {
        clear();
    }
}
