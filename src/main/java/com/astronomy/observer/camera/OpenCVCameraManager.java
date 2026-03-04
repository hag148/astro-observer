package com.astronomy.observer.camera;

import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 OpenCV VideoCapture 的摄像头管理器
 * 使用最简单的配置以最大化兼容性
 */
public class OpenCVCameraManager {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVCameraManager.class);

    private VideoCapture capture;
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);
    private final List<FrameListener> frameListeners = new CopyOnWriteArrayList<>();
    private int cameraIndex = 0;
    private Dimension currentResolution = new Dimension(640, 480);
    private Thread captureThread;
    private final int preferredBackend;

    public OpenCVCameraManager() {
        this(opencv_videoio.CAP_ANY);
    }

    public OpenCVCameraManager(int backend) {
        this.preferredBackend = backend;
        // 自动检测第一个可用的摄像头
        cameraIndex = findFirstAvailableCamera();
        logger.info("Camera manager initialized with backend: {}, camera index: {}", 
            backend == opencv_videoio.CAP_MSMF ? "MSMF" :
            backend == opencv_videoio.CAP_DSHOW ? "DirectShow" :
            backend == opencv_videoio.CAP_VFW ? "VFW" : "Any", 
            cameraIndex);
    }

    /**
     * 查找第一个可用的摄像头
     */
    private int findFirstAvailableCamera() {
        for (int i = 0; i < 5; i++) {
            VideoCapture testCap = new VideoCapture(i);
            if (testCap.isOpened()) {
                testCap.release();
                return i;
            }
            testCap.release();
        }
        return -1;
    }

    /**
     * 获取所有可用摄像头
     */
    public List<CameraDevice> getAvailableCameras() {
        List<CameraDevice> cameras = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VideoCapture testCap = new VideoCapture(i);
            if (testCap.isOpened()) {
                List<Dimension> resolutions = getSupportedResolutionsForCamera(testCap);
                cameras.add(new CameraDevice(i, "Camera " + i, resolutions));
                testCap.release();
            }
        }
        return cameras;
    }

    /**
     * 获取指定摄像头的支持分辨率
     */
    private List<Dimension> getSupportedResolutionsForCamera(VideoCapture cap) {
        List<Dimension> resolutions = new ArrayList<>();
        
        // 常见分辨率列表
        Dimension[] testResolutions = {
            new Dimension(320, 240),
            new Dimension(640, 480),
            new Dimension(800, 600),
            new Dimension(1280, 720),
            new Dimension(1600, 900),
            new Dimension(1920, 1080)
        };

        for (Dimension res : testResolutions) {
            cap.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, res.width);
            cap.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, res.height);
            
            double actualWidth = cap.get(opencv_videoio.CAP_PROP_FRAME_WIDTH);
            double actualHeight = cap.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT);
            
            if (actualWidth > 0 && actualHeight > 0) {
                Dimension actualRes = new Dimension((int) actualWidth, (int) actualHeight);
                if (!resolutions.contains(actualRes)) {
                    resolutions.add(actualRes);
                }
            }
        }
        
        return resolutions;
    }

    /**
     * 选择摄像头
     */
    public boolean selectCamera(int index) {
        if (isStreaming.get()) {
            stopStreaming();
        }

        VideoCapture testCap = new VideoCapture(index);
        if (testCap.isOpened()) {
            this.cameraIndex = index;
            testCap.release();
            logger.info("Selected camera index: {}", index);
            return true;
        }
        testCap.release();
        logger.error("Cannot open camera at index: {}", index);
        return false;
    }

    public boolean selectDefaultCamera() {
        if (cameraIndex >= 0) {
            return selectCamera(cameraIndex);
        }
        return false;
    }

    /**
     * 启动视频流
     */
    public boolean startStreaming() {
        if (isStreaming.get()) {
            logger.warn("Camera is already streaming");
            return true;
        }

        if (cameraIndex < 0) {
            logger.error("No valid camera index");
            return false;
        }

        try {
            // 简单的初始化
            capture = new VideoCapture(cameraIndex, preferredBackend);

            if (!capture.isOpened()) {
                logger.error("Failed to open camera at index: {}", cameraIndex);
                return false;
            }

            // 基本设置
            capture.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, currentResolution.width);
            capture.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, currentResolution.height);
            capture.set(opencv_videoio.CAP_PROP_FPS, 30);

            // 获取实际配置
            double actualWidth = capture.get(opencv_videoio.CAP_PROP_FRAME_WIDTH);
            double actualHeight = capture.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT);
            
            logger.info("Camera opened: {}x{}", (int) actualWidth, (int) actualHeight);

            // 读取测试帧（等待摄像头初始化）
            Mat testFrame = new Mat();
            boolean success = false;
            
            // 给摄像头足够的时间初始化
            for (int i = 0; i < 10; i++) {
                success = capture.read(testFrame);
                if (success && !testFrame.empty()) {
                    break;
                }
                Thread.sleep(100);
            }
            testFrame.release();

            if (!success) {
                logger.error("Cannot read frames from camera");
                capture.release();
                capture = null;
                return false;
            }

            // 启动捕获线程
            isStreaming.set(true);
            captureThread = new Thread(this::captureLoop, "Camera Capture Thread");
            captureThread.setDaemon(true);
            captureThread.start();

            logger.info("Camera streaming started");
            return true;
        } catch (Exception e) {
            logger.error("Failed to start camera streaming", e);
            if (capture != null) {
                capture.release();
                capture = null;
            }
            isStreaming.set(false);
            return false;
        }
    }

    /**
     * 捕获循环
     */
    private void captureLoop() {
        Mat frame = new Mat();
        int consecutiveErrors = 0;
        int maxErrors = 30; // 1秒的错误容忍（30fps * 1s）

        while (isStreaming.get() && capture != null && capture.isOpened()) {
            try {
                if (capture.read(frame) && !frame.empty()) {
                    BufferedImage image = matToBufferedImage(frame);
                    if (image != null) {
                        consecutiveErrors = 0;
                        for (FrameListener listener : frameListeners) {
                            try {
                                listener.onFrameReceived(image);
                            } catch (Exception e) {
                                logger.error("Error in frame listener", e);
                            }
                        }
                    }
                } else {
                    consecutiveErrors++;
                    if (consecutiveErrors > maxErrors) {
                        logger.error("Too many consecutive frame errors, stopping");
                        break;
                    }
                }

                // 控制帧率
                Thread.sleep(33);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in capture loop", e);
                consecutiveErrors++;
                if (consecutiveErrors > maxErrors) {
                    break;
                }
            }
        }
        frame.release();
        logger.info("Capture loop ended");
    }

    /**
     * 停止视频流
     */
    public void stopStreaming() {
        isStreaming.set(false);

        if (captureThread != null) {
            try {
                captureThread.interrupt();
                captureThread.join(2000);
            } catch (InterruptedException e) {
                logger.error("Interrupted while stopping capture thread", e);
            }
            captureThread = null;
        }

        if (capture != null) {
            capture.release();
            capture = null;
        }

        logger.info("Camera streaming stopped");
    }

    /**
     * 捕获单帧
     */
    public BufferedImage captureFrame() {
        if (capture == null || !capture.isOpened()) {
            logger.error("Camera not available");
            return null;
        }

        Mat frame = new Mat();
        try {
            for (int i = 0; i < 10; i++) {
                if (capture.read(frame) && !frame.empty()) {
                    return matToBufferedImage(frame);
                }
                Thread.sleep(100);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to capture frame", e);
            return null;
        } finally {
            frame.release();
        }
    }

    /**
     * 设置分辨率
     */
    public boolean setResolution(int width, int height) {
        this.currentResolution = new Dimension(width, height);
        if (capture != null && capture.isOpened()) {
            capture.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, width);
            capture.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, height);
            return true;
        }
        return false;
    }

    /**
     * 获取当前分辨率
     */
    public Dimension getCurrentResolution() {
        if (capture != null && capture.isOpened()) {
            double width = capture.get(opencv_videoio.CAP_PROP_FRAME_WIDTH);
            double height = capture.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT);
            return new Dimension((int) width, (int) height);
        }
        return currentResolution;
    }

    /**
     * 获取支持的分辨率
     */
    public Dimension[] getSupportedResolutions() {
        VideoCapture testCap = new VideoCapture(cameraIndex);
        if (testCap.isOpened()) {
            List<Dimension> resolutions = getSupportedResolutionsForCamera(testCap);
            testCap.release();
            return resolutions.toArray(new Dimension[0]);
        }
        return new Dimension[0];
    }

    public boolean isStreaming() {
        return isStreaming.get();
    }

    public void addFrameListener(FrameListener listener) {
        frameListeners.add(listener);
    }

    public void removeFrameListener(FrameListener listener) {
        frameListeners.remove(listener);
    }

    /**
     * 设置对焦值 (0-255)
     * @param focusValue 对焦值，0为最近距离，255为无穷远
     * @return 是否设置成功
     */
    public boolean setFocus(int focusValue) {
        if (capture != null && capture.isOpened()) {
            // 先关闭自动对焦
            capture.set(opencv_videoio.CAP_PROP_AUTOFOCUS, 0);
            // 设置对焦值
            boolean result = capture.set(opencv_videoio.CAP_PROP_FOCUS, focusValue);
            logger.info("Focus set to: {}, success: {}", focusValue, result);
            return result;
        }
        logger.warn("Cannot set focus: camera not available");
        return false;
    }

    /**
     * 获取当前对焦值
     */
    public int getFocus() {
        if (capture != null && capture.isOpened()) {
            return (int) capture.get(opencv_videoio.CAP_PROP_FOCUS);
        }
        return 0;
    }

    /**
     * 启用自动对焦
     */
    public boolean setAutoFocus(boolean enable) {
        if (capture != null && capture.isOpened()) {
            return capture.set(opencv_videoio.CAP_PROP_AUTOFOCUS, enable ? 1 : 0);
        }
        return false;
    }

    /**
     * Mat 转 BufferedImage
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }

        int type = BufferedImage.TYPE_3BYTE_BGR;
        if (mat.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
        byte[] data = dataBuffer.getData();

        mat.data().get(data);
        return image;
    }

    /**
     * 摄像头设备信息
     */
    public static class CameraDevice {
        public final int index;
        public final String name;
        public final List<Dimension> resolutions;

        public CameraDevice(int index, String name, List<Dimension> resolutions) {
            this.index = index;
            this.name = name;
            this.resolutions = resolutions;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public interface FrameListener {
        void onFrameReceived(BufferedImage frame);
    }
}
