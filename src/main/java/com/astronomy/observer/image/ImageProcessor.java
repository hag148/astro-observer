package com.astronomy.observer.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);

    /**
     * 检测天体对象（星星、行星等）- 纯Java实现
     */
    public List<DetectedObject> detectCelestialObjects(BufferedImage image, double threshold) {
        List<DetectedObject> objects = new ArrayList<>();

        if (image == null) {
            return objects;
        }

        // 转换为灰度图
        BufferedImage grayImage = toGrayScale(image);

        // 应用阈值检测
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        // 简单的亮度检测
        boolean[][] visited = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!visited[y][x]) {
                    int brightness = getBrightness(grayImage, x, y);
                    if (brightness > threshold) {
                        // 检测到一个明亮点
                        DetectedObject obj = new DetectedObject();
                        obj.x = x;
                        obj.y = y;
                        obj.brightness = brightness;
                        obj.area = calculateBrightArea(grayImage, x, y, threshold, visited);
                        objects.add(obj);
                    }
                }
            }
        }

        logger.info("Detected {} celestial objects", objects.size());
        return objects;
    }

    /**
     * 计算明亮区域面积
     */
    private int calculateBrightArea(BufferedImage grayImage, int startX, int startY, double threshold, boolean[][] visited) {
        int area = 0;
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        // 使用简单的区域增长算法
        for (int y = startY - 5; y <= startY + 5 && y < height; y++) {
            for (int x = startX - 5; x <= startX + 5 && x < width; x++) {
                if (y >= 0 && x >= 0 && !visited[y][x]) {
                    int brightness = getBrightness(grayImage, x, y);
                    if (brightness > threshold) {
                        visited[y][x] = true;
                        area++;
                    }
                }
            }
        }

        return area;
    }

    /**
     * 获取像素亮度
     */
    private int getBrightness(BufferedImage image, int x, int y) {
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return 0;
        }
        int rgb = image.getRGB(x, y);
        return (rgb >> 16) & 0xFF; // 使用红色通道
    }

    /**
     * 转换为灰度图
     */
    private BufferedImage toGrayScale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    /**
     * 增强图像（用于天文观测）
     */
    public BufferedImage enhanceForAstronomy(BufferedImage image) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 创建直方图均衡化
        BufferedImage enhanced = histogramEqualization(image);

        // 应用锐化
        BufferedImage sharpened = applySharpen(enhanced);

        return sharpened;
    }

    /**
     * 直方图均衡化
     */
    private BufferedImage histogramEqualization(BufferedImage image) {
        BufferedImage result = new BufferedImage(
            image.getWidth(), image.getHeight(), image.getType());

        int width = image.getWidth();
        int height = image.getHeight();

        // 计算直方图
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                histogram[gray]++;
            }
        }

        // 计算累积分布
        int[] cdf = new int[256];
        int sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += histogram[i];
            cdf[i] = sum;
        }

        // 归一化
        int totalPixels = width * height;
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = (int) (((double) cdf[i] / totalPixels) * 255);
        }

        // 应用LUT
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, x);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = lut[r];
                g = lut[g];
                b = lut[b];

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 应用锐化滤镜
     */
    private BufferedImage applySharpen(BufferedImage image) {
        float[] sharpenKernel = {
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
        };

        return applyConvolution(image, sharpenKernel);
    }

    /**
     * 应用卷积滤镜
     */
    private BufferedImage applyConvolution(BufferedImage image, float[] kernel) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        int kernelSize = 3;
        int halfKernel = kernelSize / 2;

        for (int y = halfKernel; y < height - halfKernel; y++) {
            for (int x = halfKernel; x < width - halfKernel; x++) {
                float sumR = 0, sumG = 0, sumB = 0;

                for (int ky = 0; ky < kernelSize; ky++) {
                    for (int kx = 0; kx < kernelSize; kx++) {
                        int pixel = image.getRGB(x + kx - halfKernel, y + ky - halfKernel);
                        float kernelValue = kernel[ky * kernelSize + kx];

                        sumR += ((pixel >> 16) & 0xFF) * kernelValue;
                        sumG += ((pixel >> 8) & 0xFF) * kernelValue;
                        sumB += (pixel & 0xFF) * kernelValue;
                    }
                }

                int r = Math.min(255, Math.max(0, (int) sumR));
                int g = Math.min(255, Math.max(0, (int) sumG));
                int b = Math.min(255, Math.max(0, (int) sumB));

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 调整对比度
     */
    public BufferedImage adjustContrast(BufferedImage image, double contrast) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = Math.min(255, Math.max(0, (int) ((r - 128) * contrast + 128)));
                g = Math.min(255, Math.max(0, (int) ((g - 128) * contrast + 128)));
                b = Math.min(255, Math.max(0, (int) ((b - 128) * contrast + 128)));

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 调整亮度
     */
    public BufferedImage adjustBrightness(BufferedImage image, int brightness) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = Math.min(255, Math.max(0, r + brightness));
                g = Math.min(255, Math.max(0, g + brightness));
                b = Math.min(255, Math.max(0, b + brightness));

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 保存图像
     */
    public boolean saveImage(BufferedImage image, String directory, String prefix) {
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
            javax.imageio.ImageIO.write(image, "PNG", outputFile);

            logger.info("Image saved to: {}", outputFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error("Failed to save image", e);
            return false;
        }
    }

    /**
     * 堆叠图像（长曝光模拟）- 纯Java实现
     */
    public BufferedImage stackImages(List<BufferedImage> images, StackMethod method) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        BufferedImage stacked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        switch (method) {
            case AVERAGE:
                return stackAverage(images);
            case MEDIAN:
                return stackMedian(images);
            default:
                return stackAverage(images);
        }
    }

    /**
     * 平均叠加
     */
    private BufferedImage stackAverage(List<BufferedImage> images) {
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long sumR = 0, sumG = 0, sumB = 0;

                for (BufferedImage image : images) {
                    int rgb = image.getRGB(x, y);
                    sumR += (rgb >> 16) & 0xFF;
                    sumG += (rgb >> 8) & 0xFF;
                    sumB += rgb & 0xFF;
                }

                int r = (int) (sumR / images.size());
                int g = (int) (sumG / images.size());
                int b = (int) (sumB / images.size());

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 中位数叠加（去除异常值）
     */
    private BufferedImage stackMedian(List<BufferedImage> images) {
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                List<Integer> reds = new ArrayList<>();
                List<Integer> greens = new ArrayList<>();
                List<Integer> blues = new ArrayList<>();

                for (BufferedImage image : images) {
                    int rgb = image.getRGB(x, y);
                    reds.add((rgb >> 16) & 0xFF);
                    greens.add((rgb >> 8) & 0xFF);
                    blues.add(rgb & 0xFF);
                }

                java.util.Collections.sort(reds);
                java.util.Collections.sort(greens);
                java.util.Collections.sort(blues);

                int medianIndex = images.size() / 2;
                int r = reds.get(medianIndex);
                int g = greens.get(medianIndex);
                int b = blues.get(medianIndex);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * 计算图像信噪比
     */
    public double calculateSNR(BufferedImage image) {
        if (image == null) {
            return 0;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        double sumSignal = 0;
        double sumNoise = 0;
        int count = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = image.getRGB(x, y) & 0xFF;
                int left = image.getRGB(x - 1, y) & 0xFF;
                int right = image.getRGB(x + 1, y) & 0xFF;
                int top = image.getRGB(x, y - 1) & 0xFF;
                int bottom = image.getRGB(x, y + 1) & 0xFF;

                double localMean = (center + left + right + top + bottom) / 5.0;
                double signal = Math.abs(center - 128);
                double noise = Math.abs(center - localMean);

                sumSignal += signal;
                sumNoise += noise;
                count++;
            }
        }

        double meanSignal = sumSignal / count;
        double meanNoise = sumNoise / count;

        if (meanNoise == 0) {
            return 0;
        }

        return 20 * Math.log10(meanSignal / meanNoise);
    }

    /**
     * 堆叠方法枚举
     */
    public enum StackMethod {
        AVERAGE,
        MEDIAN
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
