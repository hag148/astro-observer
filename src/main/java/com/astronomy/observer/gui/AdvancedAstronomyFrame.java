package com.astronomy.observer.gui;

import com.astronomy.observer.camera.CameraManager;
import com.astronomy.observer.config.UGreenCameraConfig;
import com.astronomy.observer.image.ImageProcessor;
import com.astronomy.observer.image.ImageStacker;
import com.astronomy.observer.model.ObservationSession;
import com.github.sarxos.webcam.Webcam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class AdvancedAstronomyFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedAstronomyFrame.class);

    private final CameraManager cameraManager;
    private final ImageProcessor imageProcessor;
    private final ImageStacker imageStacker;
    private final ObservationSession observationSession;

    private JLabel cameraLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton captureButton;
    private JButton stackButton;
    private JButton enhanceButton;
    private JButton detectButton;
    private JComboBox<Webcam> cameraComboBox;
    private JComboBox<String> resolutionComboBox;
    private JComboBox<UGreenCameraConfig.AstronomyPreset> presetComboBox;
    private JSlider exposureSlider;
    private JSlider gainSlider;
    private JSlider brightnessSlider;
    private JLabel statusLabel;
    private JLabel sessionLabel;
    private JLabel stackInfoLabel;
    private JTextArea notesArea;

    private BufferedImage currentImage;
    private boolean isEnhanced = false;
    private int capturedCount = 0;

    public AdvancedAstronomyFrame() {
        cameraManager = new CameraManager();
        imageProcessor = new ImageProcessor();
        imageStacker = new ImageStacker(100); // 最多堆叠100张
        observationSession = new ObservationSession();

        initializeUI();
        loadCameras();
        setupSessionListeners();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void initializeUI() {
        setTitle("天文观测系统 Pro - UGREEN FHD 2K");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 工具栏
        JToolBar toolBar = createToolBar();
        mainPanel.add(toolBar, BorderLayout.NORTH);

        // 分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // 左侧：摄像头视图
        JPanel viewPanel = createViewPanel();
        splitPane.setLeftComponent(viewPanel);

        // 右侧：控制面板
        JPanel controlPanel = createControlPanel();
        splitPane.setRightComponent(controlPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);

        // 定时更新
        Timer timer = new Timer(33, this::updateCameraView);
        timer.start();
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(new JLabel("摄像头: "));
        cameraComboBox = new JComboBox<>();
        cameraComboBox.setPreferredSize(new Dimension(200, 25));
        cameraComboBox.addActionListener(e -> selectCamera());
        toolBar.add(cameraComboBox);

        toolBar.addSeparator();
        toolBar.add(new JLabel("分辨率: "));
        resolutionComboBox = new JComboBox<>();
        resolutionComboBox.setPreferredSize(new Dimension(150, 25));
        toolBar.add(resolutionComboBox);

        toolBar.addSeparator();
        toolBar.add(new JLabel("天文预设: "));
        presetComboBox = new JComboBox<>(UGreenCameraConfig.AstronomyPreset.values());
        presetComboBox.addActionListener(e -> applyPreset());
        toolBar.add(presetComboBox);

        toolBar.addSeparator();

        startButton = new JButton("启动");
        startButton.addActionListener(this::startCamera);
        toolBar.add(startButton);

        stopButton = new JButton("停止");
        stopButton.setEnabled(false);
        stopButton.addActionListener(this::stopCamera);
        toolBar.add(stopButton);

        toolBar.addSeparator();

        JButton newSessionButton = new JButton("新观测会话");
        newSessionButton.addActionListener(this::newSession);
        toolBar.add(newSessionButton);

        return toolBar;
    }

    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("实时观测"));

        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cameraLabel.setBackground(Color.BLACK);
        cameraLabel.setOpaque(true);

        JScrollPane scrollPane = new JScrollPane(cameraLabel);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 底部信息栏
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel("状态: 准备就绪");
        sessionLabel = new JLabel("观测会话: 未开始");
        stackInfoLabel = new JLabel("图像堆栈: 0/100");

        infoPanel.add(statusLabel);
        infoPanel.add(sessionLabel);
        infoPanel.add(stackInfoLabel);

        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setPreferredSize(new Dimension(350, 0));

        // 参数控制
        JPanel paramsPanel = createParametersPanel();
        panel.add(paramsPanel, BorderLayout.NORTH);

        // 操作按钮
        JPanel actionsPanel = createActionsPanel();
        panel.add(actionsPanel, BorderLayout.CENTER);

        // 备注区域
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder("观测备注"));
        notesArea = new JTextArea(5, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(notesArea);
        notesPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(notesPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("摄像头参数"));

        // 曝光时间
        JPanel exposurePanel = new JPanel(new BorderLayout());
        exposurePanel.add(new JLabel("曝光: "), BorderLayout.WEST);
        exposureSlider = new JSlider(UGreenCameraConfig.CameraParameters.MIN_EXPOSURE,
            UGreenCameraConfig.CameraParameters.MAX_EXPOSURE, 100);
        exposureSlider.addChangeListener(e -> updateExposureLabel());
        exposurePanel.add(exposureSlider, BorderLayout.CENTER);
        panel.add(exposurePanel);

        // 增益
        JPanel gainPanel = new JPanel(new BorderLayout());
        gainPanel.add(new JLabel("增益: "), BorderLayout.WEST);
        gainSlider = new JSlider(UGreenCameraConfig.CameraParameters.MIN_GAIN,
            UGreenCameraConfig.CameraParameters.MAX_GAIN, 50);
        gainSlider.addChangeListener(e -> updateGainLabel());
        gainPanel.add(gainSlider, BorderLayout.CENTER);
        panel.add(gainPanel);

        // 亮度
        JPanel brightnessPanel = new JPanel(new BorderLayout());
        brightnessPanel.add(new JLabel("亮度: "), BorderLayout.WEST);
        brightnessSlider = new JSlider(0, 100, 50);
        brightnessSlider.addChangeListener(e -> updateBrightnessLabel());
        brightnessPanel.add(brightnessSlider, BorderLayout.CENTER);
        panel.add(brightnessPanel);

        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("操作"));

        captureButton = new JButton("截图");
        captureButton.setEnabled(false);
        captureButton.addActionListener(this::captureImage);
        panel.add(captureButton);

        stackButton = new JButton("图像叠加");
        stackButton.setEnabled(false);
        stackButton.addActionListener(this::stackImages);
        panel.add(stackButton);

        enhanceButton = new JButton("增强图像");
        enhanceButton.setEnabled(false);
        enhanceButton.addActionListener(this::enhanceImage);
        panel.add(enhanceButton);

        detectButton = new JButton("检测天体");
        detectButton.setEnabled(false);
        detectButton.addActionListener(this::detectObjects);
        panel.add(detectButton);

        JButton saveSessionButton = new JButton("保存会话");
        saveSessionButton.addActionListener(this::saveSession);
        panel.add(saveSessionButton);

        return panel;
    }

    private void loadCameras() {
        cameraComboBox.removeAllItems();

        java.util.List<Webcam> cameras = cameraManager.getAvailableCameras();
        for (Webcam cam : cameras) {
            cameraComboBox.addItem(cam);
        }

        if (!cameras.isEmpty()) {
            updateResolutionList(cameras.get(0));
        }
    }

    private void updateResolutionList(Webcam camera) {
        resolutionComboBox.removeAllItems();
        java.awt.Dimension[] sizes = camera.getCustomSizes();

        for (java.awt.Dimension size : sizes) {
            resolutionComboBox.addItem(size.width + "x" + size.height);
        }
    }

    private void selectCamera() {
        Webcam selected = (Webcam) cameraComboBox.getSelectedItem();
        if (selected != null) {
            cameraManager.selectCamera(selected);
            updateResolutionList(selected);

            // 检查是否为UGREEN摄像头
            boolean isUGreen = UGreenCameraConfig.isUGreenCamera(selected.getName());
            if (isUGreen) {
                statusLabel.setText("状态: 检测到 UGREEN FHD 2K 摄像头");
            }
        }
    }

    private void applyPreset() {
        UGreenCameraConfig.AstronomyPreset preset =
            (UGreenCameraConfig.AstronomyPreset) presetComboBox.getSelectedItem();

        if (preset != null) {
            // 设置分辨率
            String resolution = preset.getWidth() + "x" + preset.getHeight();
            resolutionComboBox.setSelectedItem(resolution);

            // 设置曝光
            exposureSlider.setValue(preset.getExposure());

            // 设置增益
            gainSlider.setValue(preset.getGain());

            statusLabel.setText("状态: 已应用 " + preset.getDisplayName() + " 预设");
        }
    }

    private void updateExposureLabel() {
        statusLabel.setText(String.format("曝光: %d", exposureSlider.getValue()));
    }

    private void updateGainLabel() {
        statusLabel.setText(String.format("增益: %d", gainSlider.getValue()));
    }

    private void updateBrightnessLabel() {
        statusLabel.setText(String.format("亮度: %d", brightnessSlider.getValue()));
    }

    private void startCamera(ActionEvent e) {
        if (cameraManager.startStreaming()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            captureButton.setEnabled(true);
            stackButton.setEnabled(true);
            enhanceButton.setEnabled(true);
            detectButton.setEnabled(true);

            observationSession.setStartTime(java.time.LocalDateTime.now());
            updateSessionLabel();

            statusLabel.setText("状态: 运行中");
        }
    }

    private void stopCamera(ActionEvent e) {
        cameraManager.stopStreaming();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        captureButton.setEnabled(false);
        stackButton.setEnabled(false);
        enhanceButton.setEnabled(false);
        detectButton.setEnabled(false);

        observationSession.setEndTime(java.time.LocalDateTime.now());
        updateSessionLabel();

        statusLabel.setText("状态: 已停止");
    }

    private void captureImage(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    String prefix = String.format("%s_%s_",
                        observationSession.getTargetName() != null ? observationSession.getTargetName() : "capture",
                        observationSession.getId());
                    return imageProcessor.saveImage(currentImage, "captures", prefix);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            capturedCount++;
                            observationSession.incrementImageCount();
                            updateSessionLabel();
                            statusLabel.setText("状态: 已保存图像 " + capturedCount);

                            // 添加到堆栈
                            if (currentImage != null) {
                                org.opencv.core.Mat mat = ImageProcessor.bufferedImageToMat(currentImage);
                                if (mat != null) {
                                    imageStacker.addImage(mat);
                                    stackInfoLabel.setText(String.format("图像堆栈: %d/%d",
                                        imageStacker.getCurrentStackSize(),
                                        imageStacker.getMaxStackSize()));
                                }
                            }
                        } else {
                            statusLabel.setText("状态: 保存失败");
                        }
                    } catch (Exception ex) {
                        logger.error("保存图像失败", ex);
                        statusLabel.setText("状态: 保存失败");
                    }
                }
            };
            worker.execute();
        }
    }

    private void stackImages(ActionEvent e) {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                statusLabel.setText("状态: 正在堆叠图像...");
                org.opencv.core.Mat stacked = imageStacker.stack();
                if (stacked != null) {
                    BufferedImage result = ImageProcessor.matToBufferedImage(stacked);
                    stacked.release();
                    return result;
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    BufferedImage result = get();
                    if (result != null) {
                        currentImage = result;
                        isEnhanced = true;
                        updateCameraLabel();
                        statusLabel.setText("状态: 图像堆叠完成");
                    }
                } catch (Exception ex) {
                    logger.error("图像堆叠失败", ex);
                    statusLabel.setText("状态: 堆叠失败");
                }
            }
        };
        worker.execute();
    }

    private void enhanceImage(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() {
                    statusLabel.setText("状态: 正在增强图像...");
                    org.opencv.core.Mat mat = ImageProcessor.bufferedImageToMat(currentImage);
                    org.opencv.core.Mat enhanced = imageProcessor.enhanceForAstronomy(mat);
                    BufferedImage result = ImageProcessor.matToBufferedImage(enhanced);
                    mat.release();
                    enhanced.release();
                    return result;
                }

                @Override
                protected void done() {
                    try {
                        currentImage = get();
                        isEnhanced = true;
                        updateCameraLabel();
                        statusLabel.setText("状态: 图像已增强");
                    } catch (Exception ex) {
                        logger.error("图像增强失败", ex);
                        statusLabel.setText("状态: 增强失败");
                    }
                }
            };
            worker.execute();
        }
    }

    private void detectObjects(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    statusLabel.setText("状态: 正在检测天体...");
                    org.opencv.core.Mat mat = ImageProcessor.bufferedImageToMat(currentImage);
                    java.util.List<ImageProcessor.DetectedObject> objects =
                        imageProcessor.detectCelestialObjects(mat, 50);
                    mat.release();
                    return objects.size();
                }

                @Override
                protected void done() {
                    try {
                        int count = get();
                        statusLabel.setText(String.format("状态: 检测到 %d 个天体", count));
                    } catch (Exception ex) {
                        logger.error("天体检测失败", ex);
                        statusLabel.setText("状态: 检测失败");
                    }
                }
            };
            worker.execute();
        }
    }

    private void newSession(ActionEvent e) {
        observationSession.endSession();
        observationSession.setId("OBS_" + java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        observationSession.setStartTime(java.time.LocalDateTime.now());
        observationSession.setEndTime(null);
        observationSession.setImageCount(0);
        observationSession.setTargetName(notesArea.getText());
        notesArea.setText("");
        imageStacker.clear();
        capturedCount = 0;
        updateSessionLabel();
        stackInfoLabel.setText("图像堆栈: 0/100");
        statusLabel.setText("状态: 新观测会话已创建");
    }

    private void saveSession(ActionEvent e) {
        observationSession.setNotes(notesArea.getText());

        // 保存会话信息
        try {
            String filename = "sessions/" + observationSession.getId() + ".txt";
            java.io.File sessionFile = new java.io.File(filename);
            sessionFile.getParentFile().mkdirs();

            java.io.PrintWriter writer = new java.io.PrintWriter(sessionFile);
            writer.println("观测会话信息");
            writer.println("=============");
            writer.println("会话ID: " + observationSession.getId());
            writer.println("目标: " + observationSession.getTargetName());
            writer.println("预设: " + observationSession.getPreset());
            writer.println("开始时间: " + observationSession.getStartTime());
            writer.println("结束时间: " + observationSession.getEndTime());
            writer.println("图像数量: " + observationSession.getImageCount());
            writer.println("持续时间: " + observationSession.getDurationSeconds() + " 秒");
            writer.println("备注: " + observationSession.getNotes());
            writer.close();

            statusLabel.setText("状态: 会话已保存");
        } catch (Exception ex) {
            logger.error("保存会话失败", ex);
            statusLabel.setText("状态: 保存失败");
        }
    }

    private void setupSessionListeners() {
        notesArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                observationSession.setTargetName(notesArea.getText().split("\n")[0]);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                observationSession.setTargetName(notesArea.getText().split("\n")[0]);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                observationSession.setTargetName(notesArea.getText().split("\n")[0]);
            }
        });
    }

    private void updateSessionLabel() {
        sessionLabel.setText(String.format("观测会话: %s | 图像: %d | 时长: %ds",
            observationSession.getId(),
            observationSession.getImageCount(),
            observationSession.getDurationSeconds()));
    }

    private void updateCameraView(ActionEvent e) {
        if (cameraManager.isStreaming()) {
            currentImage = cameraManager.captureFrame();
            if (currentImage != null) {
                isEnhanced = false;
                updateCameraLabel();
            }
        }
    }

    private void updateCameraLabel() {
        if (currentImage != null) {
            int labelWidth = cameraLabel.getWidth();
            int labelHeight = cameraLabel.getHeight();

            if (labelWidth > 0 && labelHeight > 0) {
                double scale = Math.min(
                    (double) labelWidth / currentImage.getWidth(),
                    (double) labelHeight / currentImage.getHeight()
                );
                int newWidth = (int) (currentImage.getWidth() * scale);
                int newHeight = (int) (currentImage.getHeight() * scale);

                java.awt.Image scaled = currentImage.getScaledInstance(
                    newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
                cameraLabel.setIcon(new ImageIcon(scaled));
            }
        }
    }

    private void cleanup() {
        cameraManager.stopStreaming();
        observationSession.endSession();
        imageStacker.clear();
        logger.info("应用已关闭");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            AdvancedAstronomyFrame frame = new AdvancedAstronomyFrame();
            frame.setVisible(true);
        });
    }
}
