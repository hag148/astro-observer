package com.astronomy.observer.config;

import java.awt.Dimension;

/**
 * UGREEN FHD 2K 摄像头配置
 */
public class UGreenCameraConfig {
    private static final String CAMERA_NAME = "UGREEN FHD 2K";

    /**
     * 支持的分辨率
     */
    public static final Dimension[] SUPPORTED_RESOLUTIONS = {
        new Dimension(2560, 1440),  // 2K QHD
        new Dimension(1920, 1080),  // FHD
        new Dimension(1280, 720),   // HD
        new Dimension(640, 480)    // VGA
    };

    /**
     * 天文观测预设配置
     */
    public enum AstronomyPreset {
        MOON("月球", 1920, 1080, 50, 30, 0.3f),
        PLANET("行星", 1920, 1080, 100, 50, 0.5f),
        DEEP_SKY("深空天体", 2560, 1440, 200, 100, 0.7f),
        STARS("恒星", 1920, 1080, 150, 75, 0.4f),
        SOLAR("太阳", 1920, 1080, 10, 1000, 0.1f);

        private final String displayName;
        private final int width;
        private final int height;
        private final int exposure;
        private final int gain;
        private final float brightness;

        AstronomyPreset(String displayName, int width, int height, int exposure, int gain, float brightness) {
            this.displayName = displayName;
            this.width = width;
            this.height = height;
            this.exposure = exposure;
            this.gain = gain;
            this.brightness = brightness;
        }

        public String getDisplayName() { return displayName; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getExposure() { return exposure; }
        public int getGain() { return gain; }
        public float getBrightness() { return brightness; }
    }

    /**
     * 摄像头参数范围
     */
    public static class CameraParameters {
        public static final int MIN_EXPOSURE = 10;
        public static final int MAX_EXPOSURE = 1000;
        public static final int MIN_GAIN = 0;
        public static final int MAX_GAIN = 100;
        public static final float MIN_BRIGHTNESS = 0.0f;
        public static final float MAX_BRIGHTNESS = 1.0f;
        public static final float MIN_CONTRAST = 0.0f;
        public static final float MAX_CONTRAST = 1.0f;
    }

    /**
     * 获取推荐分辨率
     */
    public static Dimension getRecommendedResolution(String target) {
        for (AstronomyPreset preset : AstronomyPreset.values()) {
            if (preset.name().equals(target.toUpperCase())) {
                return new Dimension(preset.getWidth(), preset.getHeight());
            }
        }
        return SUPPORTED_RESOLUTIONS[1]; // 默认 FHD
    }

    /**
     * 检查是否为UGREEN摄像头
     */
    public static boolean isUGreenCamera(String cameraName) {
        if (cameraName == null) return false;
        return cameraName.toUpperCase().contains("UGREEN") ||
               cameraName.toUpperCase().contains("FHD") ||
               cameraName.toUpperCase().contains("2K");
    }
}
