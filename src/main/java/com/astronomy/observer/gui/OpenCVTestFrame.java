package com.astronomy.observer.gui;

import com.astronomy.observer.camera.OpenCVCameraManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * 测试 OpenCV VideoCapture 的简单 GUI
 */
public class OpenCVTestFrame extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVTestFrame.class);

    private final OpenCVCameraManager cameraManager;
    private JLabel cameraLabel;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;

    private BufferedImage currentImage;

    public OpenCVTestFrame() {
        cameraManager = new OpenCVCameraManager();

        initializeUI();
        loadCameras();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cameraManager.stopStreaming();
            }
        });

        cameraManager.addFrameListener(frame -> {
            currentImage = frame;
            SwingUtilities.invokeLater(this::updateCameraLabel);
        });
    }

    private void initializeUI() {
        setTitle("OpenCV Camera Test");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("启动");
        startButton.addActionListener(this::startCamera);
        toolBar.add(startButton);

        stopButton = new JButton("停止");
        stopButton.setEnabled(false);
        stopButton.addActionListener(this::stopCamera);
        toolBar.add(stopButton);

        mainPanel.add(toolBar, BorderLayout.NORTH);

        // 摄像头视图
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.setBorder(BorderFactory.createTitledBorder("实时视图"));

        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cameraLabel.setPreferredSize(new Dimension(640, 480));
        cameraLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        viewPanel.add(cameraLabel, BorderLayout.CENTER);
        mainPanel.add(viewPanel, BorderLayout.CENTER);

        // 状态栏
        statusLabel = new JLabel("状态: 准备就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadCameras() {
        var cameras = cameraManager.getAvailableCameras();
        logger.info("Found {} camera(s)", cameras.size());
        if (!cameras.isEmpty()) {
            statusLabel.setText("状态: 找到 " + cameras.size() + " 个摄像头");
        } else {
            statusLabel.setText("状态: 未找到摄像头");
        }
    }

    private void startCamera(ActionEvent e) {
        if (cameraManager.selectDefaultCamera() && cameraManager.startStreaming()) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("状态: 运行中 - " + cameraManager.getCurrentResolution());
        } else {
            statusLabel.setText("状态: 启动失败");
        }
    }

    private void stopCamera(ActionEvent e) {
        cameraManager.stopStreaming();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("状态: 已停止");
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OpenCVTestFrame frame = new OpenCVTestFrame();
            frame.setVisible(true);
        });
    }
}
