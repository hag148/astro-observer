package com.astronomy.observer.gui;

import com.astronomy.observer.camera.OpenCVCameraManager;
import com.astronomy.observer.config.UGreenCameraConfig;
import com.astronomy.observer.image.OpenCVImageProcessor;
import com.astronomy.observer.image.OpenCVImageStacker;
import com.astronomy.observer.image.AstronomyImageEnhancer;
import com.astronomy.observer.model.ObservationSession;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 专业天文观测系统 - 重构版本
 * 使用独立的 GUI 组件类，降低耦合度
 */
public class AdvancedAstronomyFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedAstronomyFrame.class);

    // 核心业务组件
    private final OpenCVCameraManager cameraManager;
    private final OpenCVImageProcessor openCVImageProcessor;
    private final OpenCVImageStacker openCVImageStacker;
    private final ObservationSession observationSession;
    private final AstronomyImageEnhancer imageEnhancer;

    // GUI 组件 - 使用独立类
    private CameraViewPanel cameraViewPanel;
    private OverlayPanel overlayPanel;
    private MainToolBar mainToolBar;
    private ParametersControlPanel parametersControlPanel;
    private EnhancementControlPanel enhancementControlPanel;
    private InfoPanel infoPanel;
    private StatusBar statusBar;
    
    // 控制按钮引用
    private JButton captureButton;
    private JButton stackButton;
    private JButton detectButton;
    private JButton exportButton;
    private JButton resetZoomButton;
    
    // 状态数据
    private BufferedImage currentImage;
    private int capturedCount = 0;
    private List<OpenCVImageProcessor.DetectedObject> detectedObjects;
    
    // FPS 计算
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private int currentFPS = 0;

    public AdvancedAstronomyFrame() {
        cameraManager = new OpenCVCameraManager();
        openCVImageProcessor = new OpenCVImageProcessor();
        openCVImageStacker = new OpenCVImageStacker(100);
        observationSession = new ObservationSession();
        imageEnhancer = new AstronomyImageEnhancer();

        initializeUI();
        loadCameras();
        setupEventHandlers();
        setupKeyboardShortcuts();
        setupSessionListeners();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    /**
     * 初始化 UI - 使用独立组件类
     */
    private void initializeUI() {
        setTitle("天文观测系统 Pro - Astronomy Observer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 顶部工具栏 - 使用独立组件
        mainToolBar = new MainToolBar();
        mainPanel.add(mainToolBar, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.65);
        mainSplit.setDividerLocation(900);

        // 左侧：视图和操作 - 使用独立组件
        JPanel leftPanel = createLeftPanel();
        mainSplit.setLeftComponent(leftPanel);

        // 右侧：控制和信息 - 使用独立组件
        JPanel rightPanel = createRightPanel();
        mainSplit.setRightComponent(rightPanel);

        mainPanel.add(mainSplit, BorderLayout.CENTER);

        // 底部状态栏 - 使用独立组件
        statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 创建左侧面板（视图区域）
     */
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 摄像头视图容器
        JPanel cameraContainer = new JPanel(new BorderLayout());
        cameraContainer.setBackground(Color.BLACK);
        
        // 使用独立的 CameraViewPanel
        cameraViewPanel = new CameraViewPanel();
        cameraContainer.add(cameraViewPanel, BorderLayout.CENTER);
        
        // 使用独立的 OverlayPanel 显示 FPS 和缩放状态
        overlayPanel = new OverlayPanel();
        cameraContainer.add(overlayPanel, BorderLayout.NORTH);
        
        panel.add(cameraContainer, BorderLayout.CENTER);
        
        // 添加操作按钮面板
        panel.add(createActionButtonsPanel(), BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * 创建右侧面板（控制和信息）
     */
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        // 使用独立的参数控制面板
        parametersControlPanel = new ParametersControlPanel();
        panel.add(parametersControlPanel, BorderLayout.NORTH);
        
        // 使用独立的图像增强控制面板
        enhancementControlPanel = new EnhancementControlPanel();
        panel.add(enhancementControlPanel, BorderLayout.CENTER);
        
        // 使用独立的信息面板
        infoPanel = new InfoPanel();
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * 创建操作按钮面板
     */
    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5));
        panel.setBorder(createTitledBorder("操作面板", Color.DARK_GRAY));

        JButton captureButton = createActionButton("截图", Color.BLUE);
        JButton stackButton = createActionButton("图像叠加", new Color(128, 0, 128));
        JButton detectButton = createActionButton("检测天体", Color.RED);
        JButton exportButton = createActionButton("导出图像", Color.CYAN);
        JButton resetZoomButton = createActionButton("重置缩放", new Color(100, 100, 100));

        // 初始禁用
        captureButton.setEnabled(false);
        stackButton.setEnabled(false);
        detectButton.setEnabled(false);
        exportButton.setEnabled(false);
        resetZoomButton.setEnabled(false);

        panel.add(captureButton);
        panel.add(stackButton);
        panel.add(detectButton);
        panel.add(exportButton);
        panel.add(resetZoomButton);

        // 保存引用以便后续更新状态
        this.captureButton = captureButton;
        this.stackButton = stackButton;
        this.detectButton = detectButton;
        this.exportButton = exportButton;
        this.resetZoomButton = resetZoomButton;

        // 绑定事件处理
        captureButton.addActionListener(this::captureImage);
        stackButton.addActionListener(this::stackImages);
        detectButton.addActionListener(this::detectObjects);
        exportButton.addActionListener(this::exportImage);
        resetZoomButton.addActionListener(e -> {
            cameraViewPanel.resetZoom();
            statusBar.getStatusLabel().setText("缩放已重置");
        });

        return panel;
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 工具栏按钮事件
        mainToolBar.getStartButton().addActionListener(this::startCamera);
        mainToolBar.getStopButton().addActionListener(this::stopCamera);
        mainToolBar.getNewSessionButton().addActionListener(this::newSession);
        mainToolBar.getSaveSessionButton().addActionListener(this::saveSession);
        mainToolBar.getCameraComboBox().addActionListener(e -> selectCamera());
        mainToolBar.getPresetComboBox().addActionListener(e -> applyPreset());

        // 参数面板事件 - 对焦控制
        parametersControlPanel.getFocusSlider().addChangeListener(e -> {
            if (cameraManager.isStreaming()) {
                cameraManager.setFocus(parametersControlPanel.getFocusSlider().getValue());
                statusBar.getStatusLabel().setText("对焦值：" + parametersControlPanel.getFocusSlider().getValue());
            }
        });

        // 增强面板事件
        enhancementControlPanel.getEnhancementAlgorithmComboBox().addActionListener(e -> {
            // 算法说明更新已在 EnhancementControlPanel 内部处理
        });
        
        // 增强按钮事件
        Component[] components = enhancementControlPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                Component[] subComponents = ((JPanel) comp).getComponents();
                for (Component subComp : subComponents) {
                    if (subComp instanceof JButton) {
                        JButton button = (JButton) subComp;
                        if ("星点增强".equals(button.getText())) {
                            button.addActionListener(e -> enhanceForTarget("stars"));
                        } else if ("深空增强".equals(button.getText())) {
                            button.addActionListener(e -> enhanceForTarget("deepsky"));
                        } else if ("行星增强".equals(button.getText())) {
                            button.addActionListener(e -> enhanceForTarget("planetary"));
                        } else if ("应用增强".equals(button.getText())) {
                            button.addActionListener(e -> applyEnhancement());
                        }
                    }
                }
            }
        }

        // 信息面板表格点击事件
        infoPanel.getDetectedObjectsTable().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = infoPanel.getDetectedObjectsTable().getSelectedRow();
                if (row >= 0 && detectedObjects != null && row < detectedObjects.size()) {
                    cameraViewPanel.setSelectedObjectIndex(row);
                    statusBar.getStatusLabel().setText("已选中天体 #" + (row + 1));
                }
            }
        });
    }

    /**
     * 加载摄像头列表
     */
    private void loadCameras() {
        mainToolBar.getCameraComboBox().removeAllItems();

        List<OpenCVCameraManager.CameraDevice> cameras = cameraManager.getAvailableCameras();
        for (OpenCVCameraManager.CameraDevice cam : cameras) {
            mainToolBar.getCameraComboBox().addItem(cam);
        }

        if (!cameras.isEmpty()) {
            updateResolutionList(cameras.get(0));
        }
    }

    /**
     * 更新分辨率列表
     */
    private void updateResolutionList(OpenCVCameraManager.CameraDevice camera) {
        mainToolBar.getResolutionComboBox().removeAllItems();
        List<Dimension> sizes = camera.resolutions;

        for (Dimension size : sizes) {
            mainToolBar.getResolutionComboBox().addItem(size.width + "x" + size.height);
        }

        if (!sizes.isEmpty()) {
            mainToolBar.getResolutionComboBox().setSelectedIndex(sizes.size() - 1);
        }
    }

    /**
     * 选择摄像头
     */
    private void selectCamera() {
        OpenCVCameraManager.CameraDevice selected = 
            (OpenCVCameraManager.CameraDevice) mainToolBar.getCameraComboBox().getSelectedItem();
        if (selected != null) {
            cameraManager.selectCamera(selected.index);
            updateResolutionList(selected);
            statusBar.getStatusLabel().setText("已选择摄像头：" + selected.name);
        }
    }

    /**
     * 应用预设
     */
    private void applyPreset() {
        UGreenCameraConfig.AstronomyPreset preset = 
            (UGreenCameraConfig.AstronomyPreset) mainToolBar.getPresetComboBox().getSelectedItem();

        if (preset != null) {
            String resolution = preset.getWidth() + "x" + preset.getHeight();
            mainToolBar.getResolutionComboBox().setSelectedItem(resolution);

            parametersControlPanel.getExposureSpinner().setValue(preset.getExposure());
            parametersControlPanel.getGainSpinner().setValue(preset.getGain());

            statusBar.getStatusLabel().setText("已应用 " + preset.getDisplayName() + " 预设");
            observationSession.setPreset(preset.getDisplayName());
        }
    }

    /**
     * 启动摄像头
     */
    private void startCamera(ActionEvent e) {
        // 先注册帧监听器，确保不会错过任何帧
        cameraManager.addFrameListener(frame -> {
            currentImage = frame;
            updateFPS();
            SwingUtilities.invokeLater(() -> {
                cameraViewPanel.setCurrentImage(frame);
                updateFPSLabel();
            });
        });

        if (cameraManager.startStreaming()) {
            // 更新按钮状态
            mainToolBar.getStartButton().setEnabled(false);
            mainToolBar.getStopButton().setEnabled(true);
            setActionButtonsEnabled(true);

            // 更新会话信息
            observationSession.setStartTime(LocalDateTime.now());
            updateSessionInfo();
            updateObservationStatus("观测中");

            Dimension res = cameraManager.getCurrentResolution();
            statusBar.getStatusLabel().setText("运行中 - " + res.width + "x" + res.height);
        } else {
            statusBar.getStatusLabel().setText("摄像头启动失败");
        }
    }

    /**
     * 停止摄像头
     */
    private void stopCamera(ActionEvent e) {
        cameraManager.stopStreaming();
        
        // 更新按钮状态
        mainToolBar.getStartButton().setEnabled(true);
        mainToolBar.getStopButton().setEnabled(false);
        setActionButtonsEnabled(false);

        // 更新会话信息
        observationSession.setEndTime(LocalDateTime.now());
        updateSessionInfo();
        updateObservationStatus("已停止");
        
        overlayPanel.getFpsLabel().setText("FPS: --");
    }

    /**
     * 设置操作按钮启用状态
     */
    private void setActionButtonsEnabled(boolean enabled) {
        if (captureButton != null) captureButton.setEnabled(enabled);
        if (stackButton != null) stackButton.setEnabled(enabled);
        if (detectButton != null) detectButton.setEnabled(enabled);
        if (exportButton != null) exportButton.setEnabled(enabled);
        if (resetZoomButton != null) resetZoomButton.setEnabled(enabled);
    }

    /**
     * 捕获图像
     */
    private void captureImage(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        String timestamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        String prefix = String.format("%s_%s_",
                            infoPanel.getTargetNameField().getText().isEmpty() 
                                ? "capture" 
                                : infoPanel.getTargetNameField().getText(),
                            observationSession.getId());
                        String filename = String.format("%s%s.png", prefix, timestamp);

                        File dir = new File("captures");
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        File outputFile = new File(dir, filename);
                        javax.imageio.ImageIO.write(currentImage, "PNG", outputFile);

                        logger.info("Image saved to: {}", outputFile.getAbsolutePath());
                        return true;
                    } catch (Exception e) {
                        logger.error("Failed to save image", e);
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            capturedCount++;
                            observationSession.incrementImageCount();
                            updateSessionInfo();
                            statusBar.getStatusLabel().setText("已保存图像 #" + capturedCount);

                            // 添加到堆栈
                            Mat mat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
                            if (mat != null) {
                                openCVImageStacker.addImage(mat);
                                updateStackInfo();
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("保存图像失败", ex);
                        statusBar.getStatusLabel().setText("保存失败");
                    }
                }
            };
            worker.execute();
        }
    }

    /**
     * 堆叠图像
     */
    private void stackImages(ActionEvent e) {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                Mat stacked = openCVImageStacker.stack();
                if (stacked != null) {
                    BufferedImage result = OpenCVImageProcessor.matToBufferedImage(stacked);
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
                        cameraViewPanel.setCurrentImage(result);
                        statusBar.getStatusLabel().setText(
                            "图像堆叠完成 - 共 " + openCVImageStacker.getStackSize() + " 张");
                        infoPanel.getStackQualityLabel().setText("质量：高");
                    }
                } catch (Exception ex) {
                    logger.error("图像堆叠失败", ex);
                    statusBar.getStatusLabel().setText("堆叠失败");
                }
            }
        };
        worker.execute();
        statusBar.getStatusLabel().setText("正在堆叠图像...");
    }

    /**
     * 检测天体
     */
    private void detectObjects(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    Mat mat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
                    
                    try {
                        // 检测天体
                        detectedObjects = openCVImageProcessor.detectCelestialObjects(mat, 50);
                        
                        // 更新表格
                        DefaultTableModel model = infoPanel.getDetectedObjectsModel();
                        model.setRowCount(0);
                        for (int i = 0; i < detectedObjects.size(); i++) {
                            OpenCVImageProcessor.DetectedObject obj = detectedObjects.get(i);
                            model.addRow(new Object[]{
                                i + 1,
                                String.format("%.1f", obj.x),
                                String.format("%.1f", obj.y),
                                String.format("%.1f", obj.area),
                                String.format("%.1f", obj.brightness)
                            });
                        }
                        
                        // 更新视图显示检测结果
                        cameraViewPanel.setDetectedObjects(detectedObjects);
                        
                        return detectedObjects.size();
                    } finally {
                        mat.release();
                    }
                }

                @Override
                protected void done() {
                    try {
                        int count = get();
                        cameraViewPanel.setSelectedObjectIndex(-1);
                        statusBar.getStatusLabel().setText(String.format("检测到 %d 个天体", count));
                    } catch (Exception ex) {
                        logger.error("天体检测失败", ex);
                        statusBar.getStatusLabel().setText("检测失败");
                    }
                }
            };
            worker.execute();
            statusBar.getStatusLabel().setText("正在检测天体...");
        }
    }

    /**
     * 导出图像
     */
    private void exportImage(ActionEvent e) {
        if (currentImage != null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("导出图像");
            chooser.setSelectedFile(new java.io.File("export.png"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File file = chooser.getSelectedFile();
                    javax.imageio.ImageIO.write(currentImage, "PNG", file);
                    statusBar.getStatusLabel().setText("已导出到：" + file.getName());
                } catch (Exception ex) {
                    logger.error("导出失败", ex);
                    statusBar.getStatusLabel().setText("导出失败");
                }
            }
        }
    }

    /**
     * 为特定目标增强
     */
    private void enhanceForTarget(String target) {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this,
                "请先捕获或加载图像",
                "提示",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Mat imageMat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
        Mat enhanced = null;

        try {
            switch (target) {
                case "stars":
                    enhanced = imageEnhancer.enhanceStars(imageMat);
                    statusBar.getStatusLabel().setText("已应用星点增强");
                    break;
                case "deepsky":
                    enhanced = imageEnhancer.enhanceDeepSky(imageMat);
                    statusBar.getStatusLabel().setText("已应用深空天体增强");
                    break;
                case "planetary":
                    enhanced = imageEnhancer.enhancePlanetary(imageMat);
                    statusBar.getStatusLabel().setText("已应用行星表面增强");
                    break;
            }

            if (enhanced != null) {
                currentImage = OpenCVImageProcessor.matToBufferedImage(enhanced);
                cameraViewPanel.setCurrentImage(currentImage);
                enhanced.release();
            }
        } finally {
            imageMat.release();
        }
    }

    /**
     * 应用选择的增强算法
     */
    private void applyEnhancement() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this,
                "请先捕获或加载图像",
                "提示",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        AstronomyImageEnhancer.EnhancementAlgorithm selected =
            (AstronomyImageEnhancer.EnhancementAlgorithm) 
                enhancementControlPanel.getEnhancementAlgorithmComboBox().getSelectedItem();

        Mat imageMat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
        Mat enhanced = null;

        try {
            long startTime = System.currentTimeMillis();
            enhanced = imageEnhancer.enhance(imageMat, selected);
            long processingTime = System.currentTimeMillis() - startTime;

            if (enhanced != null) {
                currentImage = OpenCVImageProcessor.matToBufferedImage(enhanced);
                cameraViewPanel.setCurrentImage(currentImage);
                statusBar.getStatusLabel().setText(String.format("应用%s增强，耗时：%dms",
                    selected.getDescription(), processingTime));
            }
        } finally {
            imageMat.release();
            if (enhanced != null) {
                enhanced.release();
            }
        }
    }

    /**
     * 创建新会话
     */
    private void newSession(ActionEvent e) {
        observationSession.endSession();
        observationSession.setId("OBS_" + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        observationSession.setStartTime(LocalDateTime.now());
        observationSession.setEndTime(null);
        observationSession.setImageCount(0);
        observationSession.setTargetName(infoPanel.getTargetNameField().getText());
        infoPanel.getNotesArea().setText("");
        openCVImageStacker.clear();
        capturedCount = 0;
        detectedObjects = null;
        
        updateSessionInfo();
        updateStackInfo();
        cameraViewPanel.setDetectedObjects(null);
        cameraViewPanel.setSelectedObjectIndex(-1);
        statusBar.getStatusLabel().setText("新观测会话已创建");
        updateObservationStatus("待机");
    }

    /**
     * 保存会话
     */
    private void saveSession(ActionEvent e) {
        observationSession.setNotes(infoPanel.getNotesArea().getText());
        observationSession.setTargetName(infoPanel.getTargetNameField().getText());

        try {
            String filename = "sessions/" + observationSession.getId() + ".txt";
            java.io.File sessionFile = new java.io.File(filename);
            sessionFile.getParentFile().mkdirs();

            java.io.PrintWriter writer = new java.io.PrintWriter(sessionFile);
            writer.println("=".repeat(50));
            writer.println("天文观测会话报告");
            writer.println("=".repeat(50));
            writer.println();
            writer.println("会话 ID:      " + observationSession.getId());
            writer.println("观测目标：      " + observationSession.getTargetName());
            writer.println("使用预设：      " + observationSession.getPreset());
            writer.println("开始时间：      " + observationSession.getStartTime());
            writer.println("结束时间：      " + observationSession.getEndTime());
            writer.println("图像数量：      " + observationSession.getImageCount());
            writer.println("观测时长：      " + formatDuration(observationSession.getDurationSeconds()));
            writer.println();
            writer.println("观测备注:");
            writer.println("-".repeat(50));
            writer.println(observationSession.getNotes());
            writer.println("=".repeat(50));

            writer.close();
            statusBar.getStatusLabel().setText("会话已保存");
        } catch (Exception ex) {
            logger.error("保存会话失败", ex);
            statusBar.getStatusLabel().setText("保存失败");
        }
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0 秒";
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        return String.format("%d 小时 %d 分 %d 秒", hours, minutes, secs);
    }

    /**
     * 设置会话监听器
     */
    private void setupSessionListeners() {
        infoPanel.getTargetNameField().getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    observationSession.setTargetName(infoPanel.getTargetNameField().getText());
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    observationSession.setTargetName(infoPanel.getTargetNameField().getText());
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    observationSession.setTargetName(infoPanel.getTargetNameField().getText());
                }
            });
    }

    /**
     * 更新会话信息
     */
    private void updateSessionInfo() {
        infoPanel.getSessionIdLabel().setText("ID: " + observationSession.getId());
        infoPanel.getImageCountLabel().setText("已捕获：" + observationSession.getImageCount());
    }

    /**
     * 更新堆叠信息
     */
    private void updateStackInfo() {
        int size = openCVImageStacker.getStackSize();
        int max = openCVImageStacker.getMaxStackSize();
        int percentage = (size * 100) / max;
        
        infoPanel.getStackSizeLabel().setText("堆叠：" + size + "/" + max);
        infoPanel.getStackProgressLabel().setText("完成：" + percentage + "%");
        infoPanel.getStackProgressBar().setValue(percentage);
    }

    /**
     * 更新观测状态
     */
    private void updateObservationStatus(String status) {
        infoPanel.getObservationStatusLabel().setText("状态：" + status);
    }

    /**
     * 更新 FPS
     */
    private void updateFPS() {
        long now = System.currentTimeMillis();
        frameCount++;

        if (now - lastFrameTime >= 1000) {
            currentFPS = (int) (frameCount * 1000.0 / (now - lastFrameTime));
            frameCount = 0;
            lastFrameTime = now;
        }
    }

    /**
     * 更新 FPS 标签
     */
    private void updateFPSLabel() {
        overlayPanel.getFpsLabel().setText("FPS: " + currentFPS);
    }

    /**
     * 设置键盘快捷键
     */
    private void setupKeyboardShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // 空格键：截图
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "capture");
        actionMap.put("capture", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (captureButton != null && captureButton.isEnabled()) {
                    captureButton.doClick();
                }
            }
        });

        // S 键：停止/启动
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "toggle");
        actionMap.put("toggle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mainToolBar.getStartButton().isEnabled()) {
                    mainToolBar.getStartButton().doClick();
                } else if (mainToolBar.getStopButton().isEnabled()) {
                    mainToolBar.getStopButton().doClick();
                }
            }
        });

        // E 键：增强
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "enhance");
        actionMap.put("enhance", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyEnhancement();
            }
        });

        // D 键：检测
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "detect");
        actionMap.put("detect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (detectButton != null && detectButton.isEnabled()) {
                    detectButton.doClick();
                }
            }
        });
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        cameraManager.stopStreaming();
        observationSession.endSession();
        openCVImageStacker.clear();
        logger.info("应用已关闭");
    }

    /**
     * 创建带颜色的标题边框
     */
    private TitledBorder createTitledBorder(String title, Color color) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(color, 2),
            title
        );
        border.setTitleFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        border.setTitleColor(color);
        return border;
    }

    /**
     * 创建操作按钮
     */
    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
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
