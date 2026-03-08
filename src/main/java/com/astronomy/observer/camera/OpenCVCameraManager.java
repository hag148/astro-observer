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
    private static final int MAX_CAMERA_INDEX = 5;
    private static final int FRAME_ERROR_TOLERANCE = 30;
    private static final int FRAME_RETRY_ATTEMPTS = 10;
    private static final int FRAME_RETRY_DELAY_MS = 100;
    private static final int STREAMING_STOP_TIMEOUT_MS = 2000;

    public OpenCVCameraManager() {
        // 在 Windows 上，首先尝试 CAP_ANY（自动选择），因为它会尝试所有可用的后端
        // 这样可以找到最兼容且支持共享访问的后端
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
        // 尝试不同的后端，优先使用兼容性好且支持共享访问的后端
        int[] backends = {
            opencv_videoio.CAP_ANY,      // 自动选择（会尝试所有后端）
            opencv_videoio.CAP_DSHOW,    // DirectShow (Windows)
            opencv_videoio.CAP_MSMF,     // Media Foundation (Windows 10+)
            opencv_videoio.CAP_VFW       // Video for Windows
        };
        
        for (int backend : backends) {
            for (int i = 0; i < MAX_CAMERA_INDEX; i++) {
                VideoCapture testCap = new VideoCapture(i, backend);
                try {
                    if (testCap.isOpened()) {
                        logger.info("Found camera at index {} with backend {}", i, 
                            backend == opencv_videoio.CAP_ANY ? "AUTO" :
                            backend == opencv_videoio.CAP_DSHOW ? "DSHOW" :
                            backend == opencv_videoio.CAP_MSMF ? "MSMF" :
                            backend == opencv_videoio.CAP_VFW ? "VFW" : "UNKNOWN");
                        return i;
                    }
                } finally {
                    testCap.release();
                }
            }
        }
        return -1;
    }

    /**
     * 获取所有可用摄像头
     */
    public List<CameraDevice> getAvailableCameras() {
        List<CameraDevice> cameras = new ArrayList<>();
        for (int i = 0; i < MAX_CAMERA_INDEX; i++) {
            VideoCapture testCap = new VideoCapture(i);
            try {
                if (testCap.isOpened()) {
                    List<Dimension> resolutions = getSupportedResolutionsForCamera(testCap);
                    cameras.add(new CameraDevice(i, "Camera " + i, resolutions));
                }
            } finally {
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
            try {
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
            } catch (Exception e) {
                logger.debug("Error testing resolution {}:{}", res.width, res.height, e);
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
        try {
            if (testCap.isOpened()) {
                this.cameraIndex = index;
                logger.info("Selected camera index: {}", index);
                return true;
            } else {
                logger.error("Cannot open camera at index: {}", index);
                return false;
            }
        } finally {
            testCap.release();
        }
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
            logger.error("No valid camera index - please check if camera is connected and not in use by other apps");
            return false;
        }

        // 检测是否在 Windows 系统上
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");
        
        logger.info("Starting camera on {}: cameraIndex={}, preferredBackend={}", 
            osName, cameraIndex, 
            preferredBackend == opencv_videoio.CAP_DSHOW ? "DSHOW" :
            preferredBackend == opencv_videoio.CAP_MSMF ? "MSMF" :
            preferredBackend == opencv_videoio.CAP_VFW ? "VFW" : "ANY");

        try {
            // 在 Windows 上，如果首选后端失败，尝试 MSMF（可能支持更好的共享访问）
            VideoCapture tempCapture = null;
            
            if (isWindows && preferredBackend == opencv_videoio.CAP_DSHOW) {
                // 首先尝试 DSHOW
                tempCapture = new VideoCapture(cameraIndex, opencv_videoio.CAP_DSHOW);
                if (!tempCapture.isOpened()) {
                    logger.info("DSHOW failed, trying MSMF for better compatibility...");
                    tempCapture.release();
                    tempCapture = new VideoCapture(cameraIndex, opencv_videoio.CAP_MSMF);
                }
            } else {
                tempCapture = new VideoCapture(cameraIndex, preferredBackend);
            }

            capture = tempCapture;

            if (!capture.isOpened()) {
                logger.error("Failed to open camera at index: {}", cameraIndex);
                logger.error("Please close any other applications using the camera (e.g., Windows Camera, Zoom, Teams)");
                capture.release();
                capture = null;
                return false;
            }

            // 基本设置
            capture.set(opencv_videoio.CAP_PROP_FRAME_WIDTH, currentResolution.width);
            capture.set(opencv_videoio.CAP_PROP_FRAME_HEIGHT, currentResolution.height);
            capture.set(opencv_videoio.CAP_PROP_FPS, 30);
            
            // 尝试设置其他可能改善兼容性的属性
            try {
                capture.set(opencv_videoio.CAP_PROP_AUTO_EXPOSURE, 3); // 自动曝光模式
                capture.set(opencv_videoio.CAP_PROP_BRIGHTNESS, 128);  // 亮度
                capture.set(opencv_videoio.CAP_PROP_CONTRAST, 128);    // 对比度
                capture.set(opencv_videoio.CAP_PROP_SATURATION, 128);  // 饱和度
            } catch (Exception e) {
                logger.debug("Some camera properties could not be set: {}", e.getMessage());
            }

            // 获取实际配置
            double actualWidth = capture.get(opencv_videoio.CAP_PROP_FRAME_WIDTH);
            double actualHeight = capture.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT);

            logger.info("Camera successfully opened: {}x{} @ {} FPS", 
                (int) actualWidth, (int) actualHeight, 
                capture.get(opencv_videoio.CAP_PROP_FPS));

            // 读取测试帧（等待摄像头初始化）
            Mat testFrame = new Mat();
            boolean success = false;
            int attempts = 0;

            // 给摄像头足够的时间初始化
            while (attempts < FRAME_RETRY_ATTEMPTS && !success) {
                success = capture.read(testFrame);
                if (success && !testFrame.empty()) {
                    logger.info("Successfully captured test frame after {} attempts", attempts + 1);
                    break;
                }
                logger.debug("Attempt {} failed - retrying...", attempts + 1);
                attempts++;
                Thread.sleep(FRAME_RETRY_DELAY_MS);
            }
            
            if (!success) {
                logger.error("Cannot read frames from camera after {} attempts", attempts);
                logger.error("Possible causes:");
                logger.error("  1. Camera is being used by another application");
                logger.error("  2. Camera driver issue");
                logger.error("  3. Hardware problem");
                testFrame.release();
                capture.release();
                capture = null;
                return false;
            }
            
            // 将第一帧传递给监听器
            BufferedImage firstFrame = matToBufferedImage(testFrame);
            if (firstFrame != null) {
                logger.info("First frame received: {}x{}", firstFrame.getWidth(), firstFrame.getHeight());
                if (!frameListeners.isEmpty()) {
                    for (FrameListener listener : frameListeners) {
                        try {
                            listener.onFrameReceived(firstFrame);
                        } catch (Exception e) {
                            logger.error("Error in frame listener on first frame", e);
                        }
                    }
                }
            } else {
                logger.warn("Failed to convert first frame to BufferedImage");
            }
            testFrame.release();

            // 启动捕获线程
            isStreaming.set(true);
            captureThread = new Thread(this::captureLoop, "Camera Capture Thread");
            captureThread.setDaemon(true);
            captureThread.start();

            logger.info("Camera streaming started successfully");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while starting camera", e);
            if (capture != null) {
                capture.release();
                capture = null;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to start camera streaming", e);
            if (capture != null) {
                capture.release();
                capture = null;
            }
            return false;
        }
    }

    /**
     * 捕获循环
     */
    private void captureLoop() {
        Mat frame = new Mat();
        int consecutiveErrors = 0;
        
        logger.info("Capture loop started, capture={}, isStreaming={}, isOpened={}", 
            capture, isStreaming.get(), capture != null && capture.isOpened());

        while (isStreaming.get() && capture != null && capture.isOpened()) {
            try {
                boolean grabbed = capture.read(frame);
                if (grabbed && !frame.empty()) {
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
                    logger.warn("Frame capture failed: grabbed={}, empty={}, consecutiveErrors={}", 
                        grabbed, frame.empty(), consecutiveErrors);
                    if (consecutiveErrors > FRAME_ERROR_TOLERANCE) {
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
                if (consecutiveErrors > FRAME_ERROR_TOLERANCE) {
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
                captureThread.join(STREAMING_STOP_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while stopping capture thread", e);
            } finally {
                captureThread = null;
            }
        }

        if (capture != null) {
            try {
                capture.release();
                logger.debug("Camera capture released");
            } catch (Exception e) {
                logger.error("Error releasing camera capture", e);
            } finally {
                capture = null;
            }
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
            for (int i = 0; i < FRAME_RETRY_ATTEMPTS; i++) {
                if (capture.read(frame) && !frame.empty()) {
                    return matToBufferedImage(frame);
                }
                Thread.sleep(FRAME_RETRY_DELAY_MS);
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while capturing frame", e);
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
     * 安全关闭摄像头管理器，释放所有资源
     */
    public void close() {
        logger.info("Closing camera manager");
        stopStreaming();

        if (capture != null) {
            try {
                capture.release();
                logger.debug("Camera capture released");
            } catch (Exception e) {
                logger.error("Error releasing camera capture", e);
            } finally {
                capture = null;
            }
        }

        frameListeners.clear();
        logger.info("Camera manager closed successfully");
    }

    /**
     * 检查摄像头是否可用
     */
    public boolean isAvailable() {
        return capture != null && capture.isOpened();
    }

    /**
     * 获取摄像头错误信息
     */
    public String getErrorMessage() {
        if (capture == null) {
            return "Camera not initialized";
        }
        if (!capture.isOpened()) {
            return "Camera is not open";
        }
        return null;
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
