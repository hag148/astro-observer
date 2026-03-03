package com.astronomy.observer.camera;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraManager implements WebcamListener {
    private static final Logger logger = LoggerFactory.getLogger(CameraManager.class);

    private Webcam currentCamera;
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);
    private final List<FrameListener> frameListeners = new CopyOnWriteArrayList<>();

    public CameraManager() {
        Webcam.addDiscoveryListener(new WebcamDiscoveryListener() {
            @Override
            public void webcamFound(WebcamEvent we) {
                logger.info("Camera discovered: {}", we.getSource().getName());
            }

            @Override
            public void webcamDisconnected(WebcamEvent we) {
                logger.info("Camera disconnected: {}", we.getSource().getName());
            }
        });
    }

    public List<Webcam> getAvailableCameras() {
        return Webcam.getWebcams();
    }

    public boolean selectCamera(Webcam camera) {
        if (isStreaming.get()) {
            stopStreaming();
        }
        this.currentCamera = camera;
        if (camera != null) {
            camera.addWebcamListener(this);
            return true;
        }
        return false;
    }

    public boolean selectDefaultCamera() {
        Webcam defaultCam = Webcam.getDefault();
        if (defaultCam != null) {
            return selectCamera(defaultCam);
        }
        logger.warn("No default camera found");
        return false;
    }

    public boolean startStreaming() {
        if (currentCamera == null) {
            logger.error("No camera selected");
            return false;
        }

        if (isStreaming.get()) {
            logger.warn("Camera is already streaming");
            return true;
        }

        try {
            currentCamera.open(true);
            isStreaming.set(true);
            logger.info("Camera streaming started: {}", currentCamera.getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to start camera streaming", e);
            return false;
        }
    }

    public void stopStreaming() {
        if (currentCamera != null && currentCamera.isOpen()) {
            currentCamera.close();
            isStreaming.set(false);
            logger.info("Camera streaming stopped");
        }
    }

    public BufferedImage captureFrame() {
        if (currentCamera == null || !currentCamera.isOpen()) {
            logger.error("Camera not available");
            return null;
        }

        try {
            return currentCamera.getImage();
        } catch (Exception e) {
            logger.error("Failed to capture frame", e);
            return null;
        }
    }

    public Webcam getCurrentCamera() {
        return currentCamera;
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

    public boolean setResolution(int width, int height) {
        if (currentCamera != null) {
            Dimension[] sizes = currentCamera.getViewSizes();
            for (Dimension size : sizes) {
                if (size.width == width && size.height == height) {
                    currentCamera.setViewSize(size);
                    logger.info("Resolution set to: {}x{}", width, height);
                    return true;
                }
            }
            logger.warn("Resolution {}x{} not supported", width, height);
        }
        return false;
    }

    public Dimension[] getSupportedResolutions() {
        if (currentCamera != null) {
            return currentCamera.getViewSizes();
        }
        return new Dimension[0];
    }

    @Override
    public void webcamOpen(WebcamEvent we) {
        logger.info("Camera opened");
    }

    @Override
    public void webcamClosed(WebcamEvent we) {
        logger.info("Camera closed");
        isStreaming.set(false);
    }

    @Override
    public void webcamDisposed(WebcamEvent we) {
        logger.info("Camera disposed");
    }

    @Override
    public void webcamImageObtained(WebcamEvent we) {
        BufferedImage image = we.getImage();
        for (FrameListener listener : frameListeners) {
            listener.onFrameReceived(image);
        }
    }

    public interface FrameListener {
        void onFrameReceived(BufferedImage frame);
    }
}
