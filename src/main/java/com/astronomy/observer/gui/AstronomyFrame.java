package com.astronomy.observer.gui;

import com.astronomy.observer.camera.CameraManager;
import com.astronomy.observer.image.ImageProcessor;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class AstronomyFrame extends JFrame {
    private final CameraManager cameraManager;
    private final ImageProcessor imageProcessor;

    private JLabel cameraLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton captureButton;
    private JComboBox<Webcam> cameraComboBox;
    private JComboBox<String> resolutionComboBox;
    private JButton enhanceButton;
    private JLabel statusLabel;
    private JLabel objectCountLabel;

    private BufferedImage currentImage;
    private boolean isEnhanced = false;

    public AstronomyFrame() {
        cameraManager = new CameraManager();
        imageProcessor = new ImageProcessor();

        initializeUI();
        loadCameras();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cameraManager.stopStreaming();
            }
        });
    }

    private void initializeUI() {
        setTitle("天文观测系统 - Astronomy Observer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 工具栏
        JToolBar toolBar = createToolBar();
        mainPanel.add(toolBar, BorderLayout.NORTH);

        // 摄像头视图
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.setBorder(BorderFactory.createTitledBorder("实时视图"));

        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cameraLabel.setPreferredSize(new Dimension(800, 600));
        cameraLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scrollPane = new JScrollPane(cameraLabel);
        viewPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(viewPanel, BorderLayout.CENTER);

        // 信息面板
        JPanel infoPanel = createInfoPanel();
        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 定时更新摄像头画面
        Timer timer = new Timer(33, this::updateCameraView);
        timer.start();
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 摄像头选择
        toolBar.add(new JLabel("摄像头: "));
        cameraComboBox = new JComboBox<>();
        cameraComboBox.setPreferredSize(new Dimension(200, 25));
        cameraComboBox.addActionListener(e -> selectCamera());
        toolBar.add(cameraComboBox);

        // 分辨率选择
        toolBar.addSeparator();
        toolBar.add(new JLabel("分辨率: "));
        resolutionComboBox = new JComboBox<>();
        resolutionComboBox.setPreferredSize(new Dimension(150, 25));
        resolutionComboBox.addActionListener(e -> selectResolution());
        toolBar.add(resolutionComboBox);

        toolBar.addSeparator();

        // 控制按钮
        startButton = new JButton("启动");
        startButton.addActionListener(this::startCamera);
        toolBar.add(startButton);

        stopButton = new JButton("停止");
        stopButton.setEnabled(false);
        stopButton.addActionListener(this::stopCamera);
        toolBar.add(stopButton);

        captureButton = new JButton("截图");
        captureButton.setEnabled(false);
        captureButton.addActionListener(this::captureImage);
        toolBar.add(captureButton);

        enhanceButton = new JButton("增强图像");
        enhanceButton.setEnabled(false);
        enhanceButton.addActionListener(this::enhanceImage);
        toolBar.add(enhanceButton);

        toolBar.addSeparator();

        JButton detectButton = new JButton("检测天体");
        detectButton.addActionListener(this::detectObjects);
        toolBar.add(detectButton);

        return toolBar;
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBorder(BorderFactory.createTitledBorder("信息"));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("状态: 准备就绪");
        statusPanel.add(statusLabel);
        infoPanel.add(statusPanel);

        JPanel objectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        objectCountLabel = new JLabel("检测到的天体: 0");
        objectPanel.add(objectCountLabel);
        infoPanel.add(objectPanel);

        return infoPanel;
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

        if (sizes.length > 0) {
            resolutionComboBox.setSelectedIndex(sizes.length - 1);
        }
    }

    private void selectCamera() {
        Webcam selected = (Webcam) cameraComboBox.getSelectedItem();
        if (selected != null) {
            cameraManager.selectCamera(selected);
            updateResolutionList(selected);
            statusLabel.setText("状态: 已选择 " + selected.getName());
        }
    }

    private void selectResolution() {
        String resolution = (String) resolutionComboBox.getSelectedItem();
        if (resolution != null && cameraManager.getCurrentCamera() != null) {
            String[] parts = resolution.split("x");
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            cameraManager.setResolution(width, height);
        }
    }

    private void startCamera(ActionEvent e) {
        if (cameraManager.startStreaming()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            captureButton.setEnabled(true);
            enhanceButton.setEnabled(true);
            statusLabel.setText("状态: 运行中");
        }
    }

    private void stopCamera(ActionEvent e) {
        cameraManager.stopStreaming();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        captureButton.setEnabled(false);
        statusLabel.setText("状态: 已停止");
    }

    private void captureImage(ActionEvent e) {
        if (currentImage != null) {
            String saveDir = "captures";
            String prefix = isEnhanced ? "enhanced_" : "raw_";

            if (imageProcessor.saveImage(currentImage, saveDir, prefix)) {
                statusLabel.setText("状态: 图像已保存");
            } else {
                statusLabel.setText("状态: 保存失败");
            }
        }
    }

    private void enhanceImage(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
                @Override
                protected BufferedImage doInBackground() {
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
                        ex.printStackTrace();
                        statusLabel.setText("状态: 增强失败");
                    }
                }
            };
            worker.execute();
        }
    }

    private void detectObjects(ActionEvent e) {
        if (currentImage != null) {
            SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    statusLabel.setText("状态: 正在检测天体...");

                    org.opencv.core.Mat mat = ImageProcessor.bufferedImageToMat(currentImage);
                    java.util.List<ImageProcessor.DetectedObject> objects =
                        imageProcessor.detectCelestialObjects(mat, 50);
                    mat.release();

                    publish(objects.size());
                    return null;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    for (int count : chunks) {
                        objectCountLabel.setText("检测到的天体: " + count);
                    }
                }

                @Override
                protected void done() {
                    statusLabel.setText("状态: 检测完成");
                }
            };
            worker.execute();
        }
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
            // 缩放图像以适应标签
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            AstronomyFrame frame = new AstronomyFrame();
            frame.setVisible(true);
        });
    }
}
