package com.astronomy.observer.image;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * 转换BufferedImage到OpenCV Mat
     */
    public static Mat bufferedImageToMat(BufferedImage image) {
        if (image == null) {
            return null;
        }

        image = convertToStandardType(image);

        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);

        // BGR to RGB conversion
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);
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
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_BGR2RGB);

        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (rgbMat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }

        byte[] data = new byte[(int) (rgbMat.total() * rgbMat.channels())];
        rgbMat.get(0, 0, data);

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

        // 转换为灰度图
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        // 高斯模糊去噪
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // 自适应阈值处理
        Mat thresholded = new Mat();
        Imgproc.adaptiveThreshold(blurred, thresholded, 255,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2);

        // 查找轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholded, contours, hierarchy,
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // 分析轮廓
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);
            Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true);

            double area = Imgproc.contourArea(contour);

            if (area > 10) { // 过滤小噪点
                Moments moments = Imgproc.moments(contour);
                double cx = moments.get_m10() / moments.get_m00();
                double cy = moments.get_m01() / moments.get_m00();

                DetectedObject obj = new DetectedObject();
                obj.x = cx;
                obj.y = cy;
                obj.area = area;
                obj.brightness = getPixelBrightness(gray, (int) cx, (int) cy);
                objects.add(obj);
            }
        }

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
            double[] pixel = gray.get(y, x);
            return pixel[0];
        }
        return 0;
    }

    /**
     * 增强图像（用于天文观测）
     */
    public Mat enhanceForAstronomy(Mat image) {
        Mat enhanced = new Mat();

        // 转换为HSV色彩空间
        Mat hsv = new Mat();
        Imgproc.cvtColor(image, hsv, Imgproc.COLOR_BGR2HSV);

        // 增加亮度通道的对比度
        Mat[] channels = new Mat[3];
        Core.split(hsv, channels);

        // 直方图均衡化
        Imgproc.equalizeHist(channels[2], channels[2]);

        Core.merge(channels, hsv);
        Imgproc.cvtColor(hsv, enhanced, Imgproc.COLOR_HSV2BGR);

        // 轻微锐化
        Mat kernel = new Mat(3, 3, CvType.CV_32F);
        kernel.put(0, 0,
            0, -1, 0,
            -1, 5, -1,
            0, -1, 0
        );
        Imgproc.filter2D(enhanced, enhanced, -1, kernel);

        hsv.release();
        channels[0].release();
        channels[1].release();
        channels[2].release();
        kernel.release();

        return enhanced;
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
     * 堆叠图像（长曝光模拟）
     */
    public Mat stackImages(List<Mat> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }

        Mat stacked = Mat.zeros(images.get(0).size(), images.get(0).type());

        for (Mat image : images) {
            Core.add(stacked, image, stacked);
        }

        // 除以图像数量得到平均值
        Core.divide(stacked, Scalar.all(images.size()), stacked);

        logger.info("Stacked {} images", images.size());
        return stacked;
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
