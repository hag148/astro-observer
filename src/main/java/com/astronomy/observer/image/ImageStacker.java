package com.astronomy.observer.image;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 图像叠加处理器
 * 用于模拟长时间曝光
 */
public class ImageStacker {
    private static final Logger logger = LoggerFactory.getLogger(ImageStacker.class);

    private List<Mat> stackedImages;
    private int maxStackSize;
    private int stackCount;

    public ImageStacker(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        this.stackedImages = new ArrayList<>();
        this.stackCount = 0;
    }

    /**
     * 添加图像到堆栈
     */
    public boolean addImage(Mat image) {
        if (stackedImages.size() >= maxStackSize) {
            stackedImages.remove(0);
        }

        // 复制图像以避免引用问题
        Mat copy = image.clone();
        stackedImages.add(copy);
        stackCount++;

        logger.debug("Added image to stack. Stack size: {}/{}", stackedImages.size(), maxStackSize);
        return true;
    }

    /**
     * 执行图像叠加
     */
    public Mat stack() {
        if (stackedImages.isEmpty()) {
            logger.warn("No images to stack");
            return null;
        }

        Mat result = Mat.zeros(stackedImages.get(0).size(), stackedImages.get(0).type());

        for (Mat image : stackedImages) {
            Core.add(result, image, result);
        }

        // 平均化
        Core.divide(result, Scalar.all(stackedImages.size()), result);

        logger.info("Stacked {} images", stackedImages.size());
        return result;
    }

    /**
     * 使用中位数叠加（减少异常值）
     */
    public Mat medianStack() {
        if (stackedImages.isEmpty()) {
            return null;
        }

        int width = stackedImages.get(0).cols();
        int height = stackedImages.get(0).rows();
        int channels = stackedImages.get(0).channels();
        Mat result = new Mat(height, width, stackedImages.get(0).type());

        // 对每个像素计算中位数
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    double[] values = new double[stackedImages.size()];
                    for (int i = 0; i < stackedImages.size(); i++) {
                        values[i] = stackedImages.get(i).get(y, x)[c];
                    }
                    double median = computeMedian(values);
                    result.put(y, x, c, median);
                }
            }
        }

        logger.info("Median stacked {} images", stackedImages.size());
        return result;
    }

    /**
     * 计算中位数
     */
    private double computeMedian(double[] values) {
        java.util.Arrays.sort(values);
        return values[values.length / 2];
    }

    /**
     * Kappa-Sigma 叠加（去除异常值）
     */
    public Mat kappaSigmaStack(double kappa, double sigma) {
        if (stackedImages.isEmpty()) {
            return null;
        }

        // 先计算平均值和标准差
        Mat mean = Mat.zeros(stackedImages.get(0).size(), stackedImages.get(0).type());
        Mat stddev = Mat.zeros(stackedImages.get(0).size(), CvType.CV_32F);

        for (Mat image : stackedImages) {
            Core.accumulate(image, mean);
        }
        Core.divide(mean, Scalar.all(stackedImages.size()), mean);

        // 计算标准差
        for (Mat image : stackedImages) {
            Mat diff = new Mat();
            Core.subtract(image, mean, diff);
            Core.multiply(diff, diff, diff);
            Core.accumulate(diff, stddev);
            diff.release();
        }
        Core.divide(stddev, Scalar.all(stackedImages.size()), stddev);
        Core.sqrt(stddev, stddev);

        // 过滤异常值并重新叠加
        Mat result = Mat.zeros(stackedImages.get(0).size(), stackedImages.get(0).type());
        int validCount = 0;

        for (Mat image : stackedImages) {
            Mat diff = new Mat();
            Core.subtract(image, mean, diff);
            Core.absdiff(diff, Scalar.all(0), diff);

            Mat mask = new Mat();
            Core.compare(diff, stddev.mul(kappa), mask, Core.CMP_LE);

            Mat masked = new Mat();
            Core.bitwise_and(image, mask, masked);

            Core.add(result, masked, result);
            validCount++;

            diff.release();
            mask.release();
            masked.release();
        }

        if (validCount > 0) {
            Core.divide(result, Scalar.all(validCount), result);
        }

        mean.release();
        stddev.release();

        logger.info("Kappa-Sigma stacked {} images (kappa={}, sigma={})", stackedImages.size(), kappa, sigma);
        return result;
    }

    /**
     * 计算信噪比
     */
    public double calculateSNR(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(gray, mean, stddev);

        double snr = mean.get(0, 0)[0] / (stddev.get(0, 0)[0] + 1e-10);

        gray.release();
        return snr;
    }

    /**
     * 清空堆栈
     */
    public void clear() {
        for (Mat image : stackedImages) {
            image.release();
        }
        stackedImages.clear();
        stackCount = 0;
        logger.info("Image stack cleared");
    }

    /**
     * 获取当前堆栈大小
     */
    public int getCurrentStackSize() {
        return stackedImages.size();
    }

    /**
     * 获取总堆叠次数
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
}
