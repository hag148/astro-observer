package com.astronomy.observer.gui;

import com.astronomy.observer.camera.OpenCVCameraManager;
import com.astronomy.observer.config.UGreenCameraConfig;
import com.astronomy.observer.image.ImageProcessor;
import com.astronomy.observer.image.OpenCVImageProcessor;
import com.astronomy.observer.image.OpenCVImageStacker;
import com.astronomy.observer.model.ObservationSession;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 专业天文观测系统 - UI 设计遵循专业天文软件标准
 */
public class AdvancedAstronomyFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedAstronomyFrame.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OpenCVCameraManager cameraManager;
    private final ImageProcessor imageProcessor;
    private final OpenCVImageProcessor openCVImageProcessor;
    private final OpenCVImageStacker openCVImageStacker;
    private final ObservationSession observationSession;

    // 组件 - 视图区域
    private JLabel cameraLabel;
    private JLabel zoomStatusLabel;
    private JLabel fpsLabel;

    // 组件 - 工具栏
    private JComboBox<OpenCVCameraManager.CameraDevice> cameraComboBox;
    private JComboBox<String> resolutionComboBox;
    private JComboBox<UGreenCameraConfig.AstronomyPreset> presetComboBox;

    // 组件 - 控制按钮
    private JButton startButton;
    private JButton stopButton;
    private JButton captureButton;
    private JButton stackButton;
    private JButton enhanceButton;
    private JButton detectButton;
    private JButton exportButton;
    private JButton resetZoomButton;

    // 组件 - 参数控制
    private JSlider exposureSlider;
    private JSlider gainSlider;
    private JSlider brightnessSlider;
    private JSlider contrastSlider;
    private JSlider focusSlider;
    private JSpinner exposureSpinner;
    private JSpinner gainSpinner;
    private JSpinner focusSpinner;

    // 组件 - 会话管理
    private JButton newSessionButton;
    private JButton saveSessionButton;
    private JTextField targetNameField;
    private JTextArea notesArea;

    // 组件 - 信息显示
    private JLabel statusLabel;
    private JLabel sessionIdLabel;
    private JLabel durationLabel;
    private JLabel imageCountLabel;
    private JLabel stackProgressLabel;
    private JProgressBar stackProgressBar;
    private JLabel observationStatusLabel;

    // 组件 - 天体检测结果
    private JTable detectedObjectsTable;
    private DefaultTableModel detectedObjectsModel;
    private java.util.List<OpenCVImageProcessor.DetectedObject> detectedObjects;
    private int selectedObjectIndex = -1;

    // 组件 - 图像堆栈信息
    private JLabel stackSizeLabel;
    private JLabel stackQualityLabel;

    private BufferedImage currentImage;
    private boolean isEnhanced = false;
    private int capturedCount = 0;
    private long startTime = 0;

    // 缩放控制 - 类似地图交互
    private double zoomLevel = 1.0;
    private Point viewOffset = new Point(0, 0); // 视图偏移（像素）
    private final double MIN_ZOOM = 1.0;
    private final double MAX_ZOOM = 10.0;
    private final double ZOOM_FACTOR = 1.1; // 每次滚轮缩放10%

    // 拖拽相关
    private Point dragStartPoint = null;
    private Point dragStartOffset = null;
    private long lastDoubleClickTime = 0;
    private Point lastClickPoint = null;

    public AdvancedAstronomyFrame() {
        cameraManager = new OpenCVCameraManager();
        imageProcessor = new ImageProcessor();
        openCVImageProcessor = new OpenCVImageProcessor();
        openCVImageStacker = new OpenCVImageStacker(100);
        observationSession = new ObservationSession();

        initializeUI();
        loadCameras();
        setupKeyboardShortcuts();
        setupSessionListeners();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void initializeUI() {
        setTitle("天文观测系统 Pro - Astronomy Observer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // 主面板 - 使用 BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 顶部工具栏
        mainPanel.add(createMainToolBar(), BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.65);
        mainSplit.setDividerLocation(900);

        // 左侧：视图和操作
        JPanel leftPanel = createLeftPanel();
        mainSplit.setLeftComponent(leftPanel);

        // 右侧：控制和信息
        JPanel rightPanel = createRightPanel();
        mainSplit.setRightComponent(rightPanel);

        mainPanel.add(mainSplit, BorderLayout.CENTER);

        // 底部状态栏
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * 创建主工具栏
     */
    private JPanel createMainToolBar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        // 摄像头选择
        toolBar.add(createLabeledComponent("摄像头: ", cameraComboBox = new JComboBox<>()));
        cameraComboBox.setPreferredSize(new Dimension(180, 25));

        // 分辨率选择
        toolBar.add(createLabeledComponent("分辨率: ", resolutionComboBox = new JComboBox<>()));
        resolutionComboBox.setPreferredSize(new Dimension(130, 25));

        // 天文预设
        toolBar.add(createLabeledComponent("预设: ", presetComboBox = new JComboBox<>(UGreenCameraConfig.AstronomyPreset.values())));
        presetComboBox.setPreferredSize(new Dimension(120, 25));
        presetComboBox.addActionListener(e -> applyPreset());

        toolBar.add(new JSeparator(SwingConstants.VERTICAL));

        // 主控按钮
        startButton = createStyledButton("启动", new Color(76, 175, 80), Color.WHITE);
        stopButton = createStyledButton("停止", new Color(220, 53, 69), Color.WHITE);
        stopButton.setEnabled(false);

        newSessionButton = createStyledButton("新会话", new Color(33, 150, 243), Color.WHITE);
        saveSessionButton = createStyledButton("保存会话", new Color(255, 193, 7), Color.BLACK);

        toolBar.add(startButton);
        toolBar.add(stopButton);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(newSessionButton);
        toolBar.add(saveSessionButton);

        startButton.addActionListener(this::startCamera);
        stopButton.addActionListener(this::stopCamera);
        newSessionButton.addActionListener(this::newSession);
        saveSessionButton.addActionListener(this::saveSession);

        return toolBar;
    }

    /**
     * 创建左侧面板（视图 + 操作）
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));

        // 视图区域
        JPanel viewPanel = createViewPanel();
        leftPanel.add(viewPanel, BorderLayout.CENTER);

        // 操作按钮面板
        JPanel actionPanel = createActionButtonsPanel();
        leftPanel.add(actionPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    /**
     * 创建右侧面板（参数 + 信息）
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setResizeWeight(0.5);
        rightSplit.setDividerLocation(400);

        // 上部：参数控制
        JPanel paramsPanel = createParametersPanel();
        rightSplit.setTopComponent(paramsPanel);

        // 下部：信息显示
        JPanel infoPanel = createInfoPanel();
        rightSplit.setBottomComponent(infoPanel);

        rightPanel.add(rightSplit, BorderLayout.CENTER);

        return rightPanel;
    }

    /**
     * 创建摄像头视图面板
     */
    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createTitledBorder("实时观测视图", Color.BLUE));

        // 摄像头显示
        JPanel cameraContainer = new JPanel(new BorderLayout());
        cameraContainer.setBackground(Color.BLACK);
        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cameraLabel.setVerticalAlignment(SwingConstants.CENTER);
        cameraLabel.setOpaque(true);
        cameraLabel.setBackground(Color.BLACK);

        // 添加地图式交互监听器（拖拽、滚轮、双击）
        cameraLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (currentImage != null) {
                    dragStartPoint = e.getPoint();
                    dragStartOffset = new Point(viewOffset);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                dragStartPoint = null;
                dragStartOffset = null;
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (currentImage != null) {
                    long currentTime = System.currentTimeMillis();
                    long timeDiff = currentTime - lastDoubleClickTime;
                    double distance = 0;
                    if (lastClickPoint != null) {
                        distance = lastClickPoint.distance(e.getPoint());
                    }

                    // 双击检测：500ms内且距离小于10像素
                    if (timeDiff < 500 && distance < 10) {
                        handleDoubleClick(e);
                    }

                    lastDoubleClickTime = currentTime;
                    lastClickPoint = e.getPoint();
                }
            }
        });

        cameraLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (dragStartPoint != null && currentImage != null) {
                    handleDrag(e);
                }
            }
        });

        cameraLabel.addMouseWheelListener(e -> {
            if (currentImage != null) {
                handleMouseWheel(e);
            }
        });

        JScrollPane scrollPane = new JScrollPane(cameraLabel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        cameraContainer.add(scrollPane, BorderLayout.CENTER);

        panel.add(cameraContainer, BorderLayout.CENTER);

        // 视图信息覆盖层
        JPanel overlayPanel = createOverlayPanel();
        cameraContainer.add(overlayPanel, BorderLayout.NORTH);

        return panel;
    }

    /**
     * 创建覆盖信息面板
     */
    private JPanel createOverlayPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(true);
        panel.setBackground(new Color(0, 0, 0, 150));

        fpsLabel = createInfoLabel("FPS: --", Color.GREEN);
        zoomStatusLabel = createInfoLabel("缩放: 1.0x", Color.YELLOW);

        panel.add(fpsLabel);
        panel.add(zoomStatusLabel);

        return panel;
    }

    /**
     * 创建操作按钮面板
     */
    private JPanel createActionButtonsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5));
        panel.setBorder(createTitledBorder("操作面板", Color.DARK_GRAY));

        captureButton = createActionButton("截图", Color.BLUE);
        stackButton = createActionButton("图像叠加", new Color(128, 0, 128)); // 紫色
        enhanceButton = createActionButton("图像增强", Color.ORANGE);
        detectButton = createActionButton("检测天体", Color.RED);
        exportButton = createActionButton("导出图像", Color.CYAN);
        resetZoomButton = createActionButton("重置缩放", new Color(100, 100, 100));

        captureButton.setEnabled(false);
        stackButton.setEnabled(false);
        enhanceButton.setEnabled(false);
        detectButton.setEnabled(false);
        exportButton.setEnabled(false);
        resetZoomButton.setEnabled(false);

        panel.add(captureButton);
        panel.add(stackButton);
        panel.add(enhanceButton);
        panel.add(detectButton);
        panel.add(exportButton);
        panel.add(resetZoomButton);

        captureButton.addActionListener(this::captureImage);
        stackButton.addActionListener(this::stackImages);
        enhanceButton.addActionListener(this::enhanceImage);
        detectButton.addActionListener(this::detectObjects);
        exportButton.addActionListener(this::exportImage);
        resetZoomButton.addActionListener(this::resetZoom);
        detectButton.addActionListener(this::detectObjects);
        exportButton.addActionListener(this::exportImage);

        return panel;
    }

    /**
     * 创建参数控制面板
     */
    private JPanel createParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(createTitledBorder("摄像头参数控制", Color.DARK_GRAY));

        // 参数控制网格
        JPanel paramsGrid = new JPanel(new GridLayout(5, 2, 5, 10));
        paramsGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 曝光时间 - 滑块 + 精确输入
        exposureSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10));
        exposureSlider = new JSlider(10, 1000, 100);
        paramsGrid.add(createSliderControl("曝光时间 (ms)", exposureSpinner, exposureSlider, "ms"));

        // 增益 - 滑块 + 精确输入
        gainSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        gainSlider = new JSlider(0, 100, 50);
        paramsGrid.add(createSliderControl("增益", gainSpinner, gainSlider, ""));

        // 对焦 - 滑块 + 精确输入
        focusSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));
        focusSlider = new JSlider(0, 255, 128);
        paramsGrid.add(createSliderControl("对焦", focusSpinner, focusSlider, ""));

        // 亮度
        JSpinner brightnessSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        brightnessSlider = new JSlider(0, 100, 50);
        paramsGrid.add(createSliderControl("亮度", brightnessSpinner, brightnessSlider, ""));

        // 对比度
        JSpinner contrastSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        contrastSlider = new JSlider(0, 100, 50);
        paramsGrid.add(createSliderControl("对比度", contrastSpinner, contrastSlider, ""));

        panel.add(paramsGrid, BorderLayout.CENTER);

        // 同步滑块和输入框
        syncSpinnerSlider(exposureSpinner, exposureSlider);
        syncSpinnerSlider(gainSpinner, gainSlider);
        syncSpinnerSlider(focusSpinner, focusSlider, true); // 对焦需要特殊处理
        syncSpinnerSlider(brightnessSpinner, brightnessSlider);
        syncSpinnerSlider(contrastSpinner, contrastSlider);

        return panel;
    }

    /**
     * 创建滑块 + 精确输入的组合控件
     */
    private JPanel createSliderControl(String label, JSpinner spinner, JSlider slider, String unit) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));

        // 标签
        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 控件容器
        JPanel controls = new JPanel(new BorderLayout(5, 0));

        // 滑块
        controls.add(slider, BorderLayout.CENTER);

        // 精确输入框
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        spinnerPanel.add(spinner);
        if (!unit.isEmpty()) {
            spinnerPanel.add(new JLabel(unit));
        }
        spinner.setPreferredSize(new Dimension(70, 25));

        controls.add(spinnerPanel, BorderLayout.EAST);

        panel.add(controls, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建信息显示面板
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(createTitledBorder("观测信息", Color.DARK_GRAY));

        // 信息网格
        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 5, 5));
        infoGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 会话信息
        JPanel sessionPanel = createInfoCard("观测会话", new Color(33, 150, 243));
        sessionIdLabel = createInfoLabel("ID: 未开始", Color.WHITE);
        durationLabel = createInfoLabel("时长: 00:00:00", Color.WHITE);
        observationStatusLabel = createInfoLabel("状态: 待机", Color.WHITE);
        
        sessionPanel.add(sessionIdLabel);
        sessionPanel.add(Box.createVerticalStrut(5));
        sessionPanel.add(durationLabel);
        sessionPanel.add(Box.createVerticalStrut(5));
        sessionPanel.add(observationStatusLabel);

        infoGrid.add(sessionPanel);

        // 图像信息
        JPanel imagePanel = createInfoCard("图像统计", new Color(76, 175, 80));
        imageCountLabel = createInfoLabel("已捕获: 0", Color.WHITE);
        stackSizeLabel = createInfoLabel("堆叠: 0/100", Color.WHITE);
        stackQualityLabel = createInfoLabel("质量: --", Color.WHITE);
        
        imagePanel.add(imageCountLabel);
        imagePanel.add(Box.createVerticalStrut(5));
        imagePanel.add(stackSizeLabel);
        imagePanel.add(Box.createVerticalStrut(5));
        imagePanel.add(stackQualityLabel);

        infoGrid.add(imagePanel);

        // 堆叠进度
        JPanel progressPanel = createInfoCard("堆叠进度", new Color(255, 193, 7));
        stackProgressLabel = createInfoLabel("完成: 0%", Color.BLACK);
        stackProgressBar = new JProgressBar(0, 100);
        stackProgressBar.setStringPainted(true);
        stackProgressBar.setForeground(Color.GREEN);
        
        progressPanel.add(stackProgressLabel);
        progressPanel.add(Box.createVerticalStrut(5));
        progressPanel.add(stackProgressBar);

        infoGrid.add(progressPanel);

        // 目标和备注
        JPanel targetPanel = new JPanel(new BorderLayout(5, 5));
        targetPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        targetNameField = new JTextField("观测目标");
        targetNameField.setToolTipText("输入观测目标（如：月球、木星、M31等）");
        
        notesArea = new JTextArea(4, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        
        targetPanel.add(new JLabel("目标:"), BorderLayout.NORTH);
        targetPanel.add(targetNameField, BorderLayout.CENTER);

        infoGrid.add(targetPanel);

        // 天体检测结果表
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        String[] columnNames = {"序号", "X坐标", "Y坐标", "面积", "亮度"};
        detectedObjectsModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        detectedObjectsTable = new JTable(detectedObjectsModel);
        detectedObjectsTable.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        detectedObjectsTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        detectedObjectsTable.setRowHeight(22);
        detectedObjectsTable.setAutoCreateRowSorter(true);

        // 添加表格点击监听器
        detectedObjectsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = detectedObjectsTable.getSelectedRow();
                if (row >= 0 && detectedObjects != null && row < detectedObjects.size()) {
                    selectedObjectIndex = row;
                    updateCameraLabel();
                    statusLabel.setText("已选中天体 #" + (row + 1));
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(detectedObjectsTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 150));

        tablePanel.add(new JLabel("检测结果:"), BorderLayout.NORTH);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        infoGrid.add(tablePanel);

        // 备注
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createTitledBorder("观测备注"));

        panel.add(infoGrid, BorderLayout.CENTER);
        panel.add(notesScroll, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 创建状态栏
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(5, 0));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        statusBar.setPreferredSize(new Dimension(0, 28));

        statusLabel = new JLabel("准备就绪");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusBar.add(statusLabel, BorderLayout.WEST);

        // 内存使用显示
        JLabel memoryLabel = new JLabel();
        memoryLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        memoryLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        Timer memoryTimer = new Timer(1000, e -> {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long max = runtime.maxMemory() / 1024 / 1024;
            memoryLabel.setText(String.format("内存: %d / %d MB", used, max));
        });
        memoryTimer.start();

        statusBar.add(memoryLabel, BorderLayout.EAST);

        return statusBar;
    }

    // ========== 辅助方法 ==========

    /**
     * 创建带标签的组件
     */
    private JPanel createLabeledComponent(String labelText, JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        panel.add(label);
        panel.add(component);
        return panel;
    }

    /**
     * 创建样式化按钮
     */
    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
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

    /**
     * 创建信息标签
     */
    private JLabel createInfoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }

    /**
     * 创建信息卡片
     */
    private JPanel createInfoCard(String title, Color bgColor) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.NORTH);
        return panel;
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
     * 同步 Spinner 和 Slider
     */
    private void syncSpinnerSlider(JSpinner spinner, JSlider slider) {
        spinner.addChangeListener(e -> {
            slider.setValue((Integer) spinner.getValue());
        });
        slider.addChangeListener(e -> {
            spinner.setValue(slider.getValue());
        });
    }

    /**
     * 同步 Spinner 和 Slider（带回调）
     */
    private void syncSpinnerSlider(JSpinner spinner, JSlider slider, boolean applyToCamera) {
        spinner.addChangeListener(e -> {
            slider.setValue((Integer) spinner.getValue());
            if (applyToCamera) {
                applyFocus((Integer) spinner.getValue());
            }
        });
        slider.addChangeListener(e -> {
            spinner.setValue(slider.getValue());
            if (applyToCamera) {
                applyFocus(slider.getValue());
            }
        });
    }

    /**
     * 应用对焦值到摄像头
     */
    private void applyFocus(int focusValue) {
        if (cameraManager.isStreaming()) {
            cameraManager.setFocus(focusValue);
            statusLabel.setText("对焦值: " + focusValue);
        }
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
                if (captureButton.isEnabled()) {
                    captureButton.doClick();
                }
            }
        });

        // S键：停止/启动
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "toggle");
        actionMap.put("toggle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startButton.isEnabled()) {
                    startButton.doClick();
                } else if (stopButton.isEnabled()) {
                    stopButton.doClick();
                }
            }
        });

        // E键：增强
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "enhance");
        actionMap.put("enhance", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (enhanceButton.isEnabled()) {
                    enhanceButton.doClick();
                }
            }
        });

        // D键：检测
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "detect");
        actionMap.put("detect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (detectButton.isEnabled()) {
                    detectButton.doClick();
                }
            }
        });
    }

    // ========== 业务逻辑方法 ==========

    private void loadCameras() {
        cameraComboBox.removeAllItems();

        java.util.List<OpenCVCameraManager.CameraDevice> cameras = cameraManager.getAvailableCameras();
        for (OpenCVCameraManager.CameraDevice cam : cameras) {
            cameraComboBox.addItem(cam);
        }

        if (!cameras.isEmpty()) {
            updateResolutionList(cameras.get(0));
        }

        cameraComboBox.addActionListener(e -> selectCamera());
    }

    private void updateResolutionList(OpenCVCameraManager.CameraDevice camera) {
        resolutionComboBox.removeAllItems();
        java.util.List<java.awt.Dimension> sizes = camera.resolutions;

        for (java.awt.Dimension size : sizes) {
            resolutionComboBox.addItem(size.width + "x" + size.height);
        }

        if (!sizes.isEmpty()) {
            resolutionComboBox.setSelectedIndex(sizes.size() - 1);
        }
    }

    private void selectCamera() {
        OpenCVCameraManager.CameraDevice selected = (OpenCVCameraManager.CameraDevice) cameraComboBox.getSelectedItem();
        if (selected != null) {
            cameraManager.selectCamera(selected.index);
            updateResolutionList(selected);
            statusLabel.setText("已选择摄像头: " + selected.name);
        }
    }

    private void selectResolution() {
        String resolution = (String) resolutionComboBox.getSelectedItem();
        if (resolution != null) {
            String[] parts = resolution.split("x");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            cameraManager.setResolution(width, height);
        }
    }

    private void applyPreset() {
        UGreenCameraConfig.AstronomyPreset preset =
            (UGreenCameraConfig.AstronomyPreset) presetComboBox.getSelectedItem();

        if (preset != null) {
            String resolution = preset.getWidth() + "x" + preset.getHeight();
            resolutionComboBox.setSelectedItem(resolution);

            exposureSpinner.setValue(preset.getExposure());
            gainSpinner.setValue(preset.getGain());

            statusLabel.setText("已应用 " + preset.getDisplayName() + " 预设");
            observationSession.setPreset(preset.getDisplayName());
        }
    }

    private void startCamera(ActionEvent e) {
        // 注册帧监听器
        cameraManager.addFrameListener(frame -> {
            currentImage = frame;
            isEnhanced = false;
            updateFPS();
            SwingUtilities.invokeLater(() -> {
                updateCameraLabel();
                updateFPSLabel();
            });
        });

        if (cameraManager.startStreaming()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            captureButton.setEnabled(true);
            stackButton.setEnabled(true);
            enhanceButton.setEnabled(true);
            detectButton.setEnabled(true);
            exportButton.setEnabled(true);
            resetZoomButton.setEnabled(true);

            observationSession.setStartTime(LocalDateTime.now());
            startTime = System.currentTimeMillis();
            updateSessionInfo();
            updateObservationStatus("观测中");

            Dimension res = cameraManager.getCurrentResolution();
            statusLabel.setText("运行中 - " + res.width + "x" + res.height);
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
        exportButton.setEnabled(false);
        resetZoomButton.setEnabled(false);

        observationSession.setEndTime(LocalDateTime.now());
        updateSessionInfo();
        updateObservationStatus("已停止");
        fpsLabel.setText("FPS: --");
    }

    private void captureImage(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    String prefix = String.format("%s_%s_",
                        targetNameField.getText().isEmpty() ? "capture" : targetNameField.getText(),
                        observationSession.getId());
                    return imageProcessor.saveImage(currentImage, "captures", prefix);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            capturedCount++;
                            observationSession.incrementImageCount();
                            updateSessionInfo();
                            statusLabel.setText("已保存图像 #" + capturedCount);

                            // 添加到堆栈
                            Mat mat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
                            if (mat != null) {
                                openCVImageStacker.addImage(mat);
                                updateStackInfo();
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("保存图像失败", ex);
                        statusLabel.setText("保存失败");
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
                        isEnhanced = true;
                        updateCameraLabel();
                        statusLabel.setText("图像堆叠完成 - 共 " + openCVImageStacker.getStackSize() + " 张");
                        stackQualityLabel.setText("质量: 高");
                    }
                } catch (Exception ex) {
                    logger.error("图像堆叠失败", ex);
                    statusLabel.setText("堆叠失败");
                }
            }
        };
        worker.execute();
        statusLabel.setText("正在堆叠图像...");
    }

    private void enhanceImage(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() {
                    Mat mat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
                    Mat enhanced = openCVImageProcessor.enhanceForAstronomy(mat);
                    BufferedImage result = OpenCVImageProcessor.matToBufferedImage(enhanced);
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
                        statusLabel.setText("图像已增强");
                    } catch (Exception ex) {
                        logger.error("图像增强失败", ex);
                        statusLabel.setText("增强失败");
                    }
                }
            };
            worker.execute();
            statusLabel.setText("正在增强图像...");
        }
    }

    private void detectObjects(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    Mat mat = OpenCVImageProcessor.bufferedImageToMat(currentImage);
                    detectedObjects = openCVImageProcessor.detectCelestialObjects(mat, 50);
                    mat.release();

                    // 更新表格
                    SwingUtilities.invokeLater(() -> {
                        detectedObjectsModel.setRowCount(0);
                        for (int i = 0; i < detectedObjects.size(); i++) {
                            OpenCVImageProcessor.DetectedObject obj = detectedObjects.get(i);
                            detectedObjectsModel.addRow(new Object[]{
                                i + 1,
                                String.format("%.1f", obj.x),
                                String.format("%.1f", obj.y),
                                String.format("%.1f", obj.area),
                                String.format("%.1f", obj.brightness)
                            });
                        }
                    });

                    return detectedObjects.size();
                }

                @Override
                protected void done() {
                    try {
                        int count = get();
                        selectedObjectIndex = -1;
                        statusLabel.setText(String.format("检测到 %d 个天体", count));
                    } catch (Exception ex) {
                        logger.error("天体检测失败", ex);
                        statusLabel.setText("检测失败");
                    }
                }
            };
            worker.execute();
            statusLabel.setText("正在检测天体...");
        }
    }

    private void exportImage(ActionEvent e) {
        if (currentImage != null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("导出图像");
            chooser.setSelectedFile(new java.io.File("export.png"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File file = chooser.getSelectedFile();
                    javax.imageio.ImageIO.write(currentImage, "PNG", file);
                    statusLabel.setText("已导出到: " + file.getName());
                } catch (Exception ex) {
                    logger.error("导出失败", ex);
                    statusLabel.setText("导出失败");
                }
            }
        }
    }

    /**
     * 重置缩放
     */
    private void resetZoom(ActionEvent e) {
        zoomLevel = 1.0;
        viewOffset = new Point(0, 0);
        updateCameraLabel();
        statusLabel.setText("缩放已重置");
    }

    /**
     * 处理拖拽平移（类似地图）
     */
    private void handleDrag(java.awt.event.MouseEvent e) {
        int labelWidth = cameraLabel.getWidth();
        int labelHeight = cameraLabel.getHeight();

        if (labelWidth <= 0 || labelHeight <= 0) return;

        // 计算基础缩放比例
        double baseScale = Math.min(
            (double) labelWidth / currentImage.getWidth(),
            (double) labelHeight / currentImage.getHeight()
        );
        double currentScale = baseScale * zoomLevel;

        // 计算拖拽偏移（在原图坐标系中）
        int dx = (int) ((e.getX() - dragStartPoint.x) / currentScale);
        int dy = (int) ((e.getY() - dragStartPoint.y) / currentScale);

        // 更新偏移
        viewOffset.x = dragStartOffset.x + dx;
        viewOffset.y = dragStartOffset.y + dy;

        // 限制偏移不超出图像范围
        clampViewOffset();

        updateCameraLabel();
    }

    /**
     * 处理鼠标滚轮缩放（类似地图）
     */
    private void handleMouseWheel(java.awt.event.MouseWheelEvent e) {
        int labelWidth = cameraLabel.getWidth();
        int labelHeight = cameraLabel.getHeight();

        if (labelWidth <= 0 || labelHeight <= 0) return;

        // 计算基础缩放比例
        double baseScale = Math.min(
            (double) labelWidth / currentImage.getWidth(),
            (double) labelHeight / currentImage.getHeight()
        );
        double currentScale = baseScale * zoomLevel;

        // 获取鼠标在原图中的坐标（缩放前）
        Point mousePoint = e.getPoint();
        Point originalPoint = screenToImage(mousePoint, currentScale);

        // 滚轮向上 = 放大，向下 = 缩小
        double newZoomLevel;
        if (e.getWheelRotation() < 0) {
            newZoomLevel = zoomLevel * ZOOM_FACTOR;
        } else {
            newZoomLevel = zoomLevel / ZOOM_FACTOR;
        }

        // 限制缩放范围
        newZoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoomLevel));

        // 计算缩放后的新偏移，使鼠标位置保持在同一点
        zoomLevel = newZoomLevel;
        double newScale = baseScale * zoomLevel;

        // 调整偏移以保持鼠标位置不变
        if (newScale != currentScale) {
            double scaleRatio = newScale / currentScale;
            Point newOriginalPoint = screenToImage(mousePoint, newScale);

            viewOffset.x += (int) (originalPoint.x - newOriginalPoint.x);
            viewOffset.y += (int) (originalPoint.y - newOriginalPoint.y);

            clampViewOffset();
        }

        updateCameraLabel();
        statusLabel.setText(String.format("缩放: %.1fx", zoomLevel));
    }

    /**
     * 处理双击（快速放大到3倍或重置）
     */
    private void handleDoubleClick(java.awt.event.MouseEvent e) {
        if (zoomLevel < 1.5) {
            // 如果当前缩放较小，放大到3倍
            zoomLevel = 3.0;

            // 以双击位置为中心
            int labelWidth = cameraLabel.getWidth();
            int labelHeight = cameraLabel.getHeight();
            double baseScale = Math.min(
                (double) labelWidth / currentImage.getWidth(),
                (double) labelHeight / currentImage.getHeight()
            );
            double currentScale = baseScale;

            Point originalPoint = screenToImage(e.getPoint(), currentScale);

            // 设置偏移使该点居中
            viewOffset.x = (int) (originalPoint.x - currentImage.getWidth() / (2 * zoomLevel));
            viewOffset.y = (int) (originalPoint.y - currentImage.getHeight() / (2 * zoomLevel));

            clampViewOffset();
            updateCameraLabel();
            statusLabel.setText(String.format("缩放: %.1fx @(%d, %d)", zoomLevel, originalPoint.x, originalPoint.y));
        } else {
            // 如果已经放大，重置
            zoomLevel = 1.0;
            viewOffset = new Point(0, 0);
            updateCameraLabel();
            statusLabel.setText("缩放已重置");
        }
    }

    /**
     * 将屏幕坐标转换为图像坐标
     */
    private Point screenToImage(Point screenPoint, double scale) {
        int labelWidth = cameraLabel.getWidth();
        int labelHeight = cameraLabel.getHeight();

        double baseScale = Math.min(
            (double) labelWidth / currentImage.getWidth(),
            (double) labelHeight / currentImage.getHeight()
        );

        // 计算在缩放后图像中的坐标
        int imgX = (int) ((screenPoint.x - (labelWidth - currentImage.getWidth() * baseScale * zoomLevel) / 2) / scale + viewOffset.x);
        int imgY = (int) ((screenPoint.y - (labelHeight - currentImage.getHeight() * baseScale * zoomLevel) / 2) / scale + viewOffset.y);

        imgX = Math.max(0, Math.min(imgX, currentImage.getWidth() - 1));
        imgY = Math.max(0, Math.min(imgY, currentImage.getHeight() - 1));

        return new Point(imgX, imgY);
    }

    /**
     * 限制视图偏移不超出图像范围
     */
    private void clampViewOffset() {
        int maxOffsetX = currentImage.getWidth() / 2;
        int maxOffsetY = currentImage.getHeight() / 2;

        viewOffset.x = Math.max(-maxOffsetX, Math.min(maxOffsetX, viewOffset.x));
        viewOffset.y = Math.max(-maxOffsetY, Math.min(maxOffsetY, viewOffset.y));
    }

    private void newSession(ActionEvent e) {
        observationSession.endSession();
        observationSession.setId("OBS_" + LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        observationSession.setStartTime(LocalDateTime.now());
        observationSession.setEndTime(null);
        observationSession.setImageCount(0);
        observationSession.setTargetName(targetNameField.getText());
        notesArea.setText("");
        openCVImageStacker.clear();
        capturedCount = 0;
        
        updateSessionInfo();
        updateStackInfo();
        statusLabel.setText("新观测会话已创建");
        updateObservationStatus("待机");
    }

    private void saveSession(ActionEvent e) {
        observationSession.setNotes(notesArea.getText());
        observationSession.setTargetName(targetNameField.getText());

        try {
            String filename = "sessions/" + observationSession.getId() + ".txt";
            java.io.File sessionFile = new java.io.File(filename);
            sessionFile.getParentFile().mkdirs();

            java.io.PrintWriter writer = new java.io.PrintWriter(sessionFile);
            writer.println("=" .repeat(50));
            writer.println("天文观测会话报告");
            writer.println("=".repeat(50));
            writer.println();
            writer.println("会话ID:      " + observationSession.getId());
            writer.println("观测目标:      " + observationSession.getTargetName());
            writer.println("使用预设:      " + observationSession.getPreset());
            writer.println("开始时间:      " + observationSession.getStartTime());
            writer.println("结束时间:      " + observationSession.getEndTime());
            writer.println("图像数量:      " + observationSession.getImageCount());
            writer.println("观测时长:      " + formatDuration(observationSession.getDurationSeconds()));
            writer.println();
            writer.println("观测备注:");
            writer.println("-".repeat(50));
            writer.println(observationSession.getNotes());
            writer.println("=".repeat(50));

            writer.close();
            statusLabel.setText("会话已保存");
        } catch (Exception ex) {
            logger.error("保存会话失败", ex);
            statusLabel.setText("保存失败");
        }
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0秒";
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        return String.format("%d小时 %d分 %d秒", hours, minutes, secs);
    }

    private void setupSessionListeners() {
        targetNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                observationSession.setTargetName(targetNameField.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                observationSession.setTargetName(targetNameField.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                observationSession.setTargetName(targetNameField.getText());
            }
        });
    }

    private void updateSessionInfo() {
        sessionIdLabel.setText("ID: " + observationSession.getId());
        imageCountLabel.setText("已捕获: " + observationSession.getImageCount());
    }

    private void updateStackInfo() {
        int size = openCVImageStacker.getStackSize();
        int max = openCVImageStacker.getMaxStackSize();
        int percentage = (size * 100) / max;
        
        stackSizeLabel.setText("堆叠: " + size + "/" + max);
        stackProgressLabel.setText("完成: " + percentage + "%");
        stackProgressBar.setValue(percentage);
    }

    private void updateObservationStatus(String status) {
        observationStatusLabel.setText("状态: " + status);
    }

    private long lastFrameTime = 0;
    private int frameCount = 0;
    private int currentFPS = 0;

    private void updateFPS() {
        long now = System.currentTimeMillis();
        frameCount++;

        if (now - lastFrameTime >= 1000) {
            currentFPS = (int) (frameCount * 1000.0 / (now - lastFrameTime));
            frameCount = 0;
            lastFrameTime = now;
        }
    }

    private void updateFPSLabel() {
        fpsLabel.setText("FPS: " + currentFPS);
    }

    private void updateCameraLabel() {
        if (currentImage != null) {
            int labelWidth = cameraLabel.getWidth();
            int labelHeight = cameraLabel.getHeight();

            if (labelWidth > 0 && labelHeight > 0) {
                // 基础缩放比例（适应窗口）
                double baseScale = Math.min(
                    (double) labelWidth / currentImage.getWidth(),
                    (double) labelHeight / currentImage.getHeight()
                );

                // 应用用户缩放
                double finalScale = baseScale * zoomLevel;

                // 创建固定大小的显示图像（避免放大时创建巨大图像）
                BufferedImage displayImage = new BufferedImage(
                    labelWidth, labelHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = displayImage.createGraphics();

                // 启用高质量渲染
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // 填充黑色背景
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, labelWidth, labelHeight);

                // 保存原始变换
                AffineTransform originalTransform = g2d.getTransform();

                // 应用缩放变换（这是关键优化：只处理标签大小的像素）
                g2d.translate(labelWidth / 2, labelHeight / 2);
                g2d.scale(finalScale, finalScale);
                g2d.translate(-currentImage.getWidth() / 2, -currentImage.getHeight() / 2);
                g2d.translate(-viewOffset.x, -viewOffset.y);

                // 直接绘制原图（不创建缩放副本）
                g2d.drawImage(currentImage, 0, 0, null);

                // 恢复原始变换
                g2d.setTransform(originalTransform);

                // 如果有选中的天体，绘制标识
                if (selectedObjectIndex >= 0 && detectedObjects != null &&
                    selectedObjectIndex < detectedObjects.size()) {
                    OpenCVImageProcessor.DetectedObject obj = detectedObjects.get(selectedObjectIndex);

                    // 计算天体在屏幕中的位置
                    double objDisplayX = (obj.x - viewOffset.x) * finalScale + (labelWidth - currentImage.getWidth() * finalScale) / 2;
                    double objDisplayY = (obj.y - viewOffset.y) * finalScale + (labelHeight - currentImage.getHeight() * finalScale) / 2;

                    // 检查是否在可见范围内
                    int displayX = (int) objDisplayX;
                    int displayY = (int) objDisplayY;

                    if (displayX > -100 && displayX < labelWidth + 100 &&
                        displayY > -100 && displayY < labelHeight + 100) {

                        // 计算圆的半径（基于面积）
                        double radius = Math.sqrt(obj.area) * finalScale / 2;

                        // 绘制圆圈标识
                        g2d.setColor(Color.YELLOW);
                        g2d.setStroke(new java.awt.BasicStroke(2));
                        g2d.drawOval(
                            (int) (displayX - radius),
                            (int) (displayY - radius),
                            (int) (radius * 2),
                            (int) (radius * 2)
                        );

                        // 绘制十字准星
                        g2d.setColor(Color.RED);
                        g2d.setStroke(new java.awt.BasicStroke(1));
                        int crossSize = (int) (10 * zoomLevel);
                        g2d.drawLine(displayX - crossSize, displayY, displayX + crossSize, displayY);
                        g2d.drawLine(displayX, displayY - crossSize, displayX, displayY + crossSize);

                        // 绘制标签
                        g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
                        g2d.setColor(Color.GREEN);
                        String label = String.format("天体 #%d", selectedObjectIndex + 1);
                        g2d.drawString(label, displayX + (int) radius + 5, displayY - 5);
                    }
                }

                g2d.dispose();
                cameraLabel.setIcon(new ImageIcon(displayImage));

                zoomStatusLabel.setText(String.format("缩放: %.1fx", zoomLevel));
            }
        }
    }

    private void cleanup() {
        cameraManager.stopStreaming();
        observationSession.endSession();
        openCVImageStacker.clear();
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
