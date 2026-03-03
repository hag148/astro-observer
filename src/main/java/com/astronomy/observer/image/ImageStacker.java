package com.astronomy.observer.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图像叠加处理器
 * 用于模拟长时间曝光
 */
public class ImageStacker {
    private static final Logger logger = LoggerFactory.getLogger(ImageStacker.class);

    private List<BufferedImage> stackedImages;
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
    public boolean addImage(BufferedImage image) {
        if (image == null) {
            logger.warn("Cannot add null image to stack");
            return false;
        }

        if (stackedImages.size() >= maxStackSize) {
            stackedImages.remove(0);
        }

        // 复制图像以避免引用问题
        BufferedImage copy = new BufferedImage(
            image.getWidth(), image.getHeight(), image.getType());
        copy.getGraphics().drawImage(image, 0, 0, null);

        stackedImages.add(copy);
        stackCount++;

        logger.debug("Added image to stack. Stack size: {}/{}", stackedImages.size(), maxStackSize);
        return true;
    }

    /**
     * 执行图像叠加（平均叠加）
     */
    public BufferedImage stack() {
        return stack(StackMethod.AVERAGE);
    }

    /**
     * 执行指定方法的图像叠加
     */
    public BufferedImage stack(StackMethod method) {
        if (stackedImages.isEmpty()) {
            logger.warn("No images to stack");
            return null;
        }

        BufferedImage result;
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
            default:
                result = averageStack();
        }

        logger.info("Stacked {} images using {} method", stackedImages.size(), method);
        return result;
    }

    /**
     * 平均叠加
     */
    private BufferedImage averageStack() {
        int width = stackedImages.get(0).getWidth();
        int height = stackedImages.get(0).getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long sumR = 0, sumG = 0, sumB = 0;

                for (BufferedImage image : stackedImages) {
                    int rgb = image.getRGB(x, y);
                    sumR += (rgb >> 16) & 0xFF;
                    sumG += (rgb >> 8) & 0xFF;
                    sumB += rgb & 0xFF;
                }

                int r = (int) (sumR / stackedImages.size());
                int g = (int) (sumG / stackedImages.size());
                int b = (int) (sumB / stackedImages.size());

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 中位数叠加（减少异常值）
     */
    private BufferedImage medianStack() {
        int width = stackedImages.get(0).getWidth();
        int height = stackedImages.get(0).getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                List<Integer> reds = new ArrayList<>();
                List<Integer> greens = new ArrayList<>();
                List<Integer> blues = new ArrayList<>();

                for (BufferedImage image : stackedImages) {
                    int rgb = image.getRGB(x, y);
                    reds.add((rgb >> 16) & 0xFF);
                    greens.add((rgb >> 8) & 0xFF);
                    blues.add(rgb & 0xFF);
                }

                Collections.sort(reds);
                Collections.sort(greens);
                Collections.sort(blues);

                int medianIndex = stackedImages.size() / 2;
                int r = reds.get(medianIndex);
                int g = greens.get(medianIndex);
                int b = blues.get(medianIndex);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * Kappa-Sigma叠加（高质量叠加，去除极端异常值）
     */
    private BufferedImage kappaSigmaStack(double kappa) {
        int width = stackedImages.get(0).getWidth();
        int height = stackedImages.get(0).getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 计算每个通道的值
                List<Integer> reds = new ArrayList<>();
                List<Integer> greens = new ArrayList<>();
                List<Integer> blues = new ArrayList<>();

                for (BufferedImage image : stackedImages) {
                    int rgb = image.getRGB(x, y);
                    reds.add((rgb >> 16) & 0xFF);
                    greens.add((rgb >> 8) & 0xFF);
                    blues.add(rgb & 0xFF);
                }

                // 对每个通道应用Kappa-Sigma
                int r = applyKappaSigma(reds, kappa);
                int g = applyKappaSigma(greens, kappa);
                int b = applyKappaSigma(blues, kappa);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 应用Kappa-Sigma算法
     */
    private int applyKappaSigma(List<Integer> values, double kappa) {
        if (values.isEmpty()) {
            return 0;
        }

        List<Integer> filtered = new ArrayList<>(values);
        boolean changed;
        int maxIterations = 10;

        do {
            changed = false;

            // 计算均值和标准差
            double mean = calculateMean(filtered);
            double stdDev = calculateStdDev(filtered, mean);

            // 去除超出kappa*stdDev范围的值
            List<Integer> newFiltered = new ArrayList<>();
            double threshold = kappa * stdDev;

            for (Integer value : filtered) {
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
        return (int) calculateMean(filtered);
    }

    /**
     * 计算均值
     */
    private double calculateMean(List<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }

        long sum = 0;
        for (Integer value : values) {
            sum += value;
        }

        return (double) sum / values.size();
    }

    /**
     * 计算标准差
     */
    private double calculateStdDev(List<Integer> values, double mean) {
        if (values.isEmpty() || values.size() == 1) {
            return 0;
        }

        double sumSquares = 0;
        for (Integer value : values) {
            double diff = value - mean;
            sumSquares += diff * diff;
        }

        return Math.sqrt(sumSquares / values.size());
    }

    /**
     * 清空堆栈
     */
    public void clear() {
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
     * 堆叠方法枚举
     */
    public enum StackMethod {
        AVERAGE("平均叠加"),
        MEDIAN("中位数叠加"),
        KAPPA_SIGMA("Kappa-Sigma叠加");

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
}
