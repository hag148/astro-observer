package com.astronomy.observer.gui;

import com.astronomy.observer.camera.OpenCVCameraManager;
import com.astronomy.observer.config.UGreenCameraConfig;
import javax.swing.*;
import java.awt.*;

/**
 * 独立的主工具栏组件
 * 负责摄像头选择、分辨率选择、天文预设和主控按钮
 */
public class MainToolBar extends JPanel {
    private JComboBox<OpenCVCameraManager.CameraDevice> cameraComboBox;
    private JComboBox<String> resolutionComboBox;
    private JComboBox<UGreenCameraConfig.AstronomyPreset> presetComboBox;
    
    private JButton startButton;
    private JButton stopButton;
    private JButton newSessionButton;
    private JButton saveSessionButton;
    
    public MainToolBar() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }
    
    private void initializeComponents() {
        // 摄像头选择
        cameraComboBox = new JComboBox<>();
        cameraComboBox.setPreferredSize(new Dimension(180, 25));
        
        // 分辨率选择
        resolutionComboBox = new JComboBox<>();
        resolutionComboBox.setPreferredSize(new Dimension(130, 25));
        
        // 天文预设
        presetComboBox = new JComboBox<>(UGreenCameraConfig.AstronomyPreset.values());
        presetComboBox.setPreferredSize(new Dimension(120, 25));
        
        // 主控按钮
        startButton = createStyledButton("启动", new Color(76, 175, 80), Color.WHITE);
        stopButton = createStyledButton("停止", new Color(220, 53, 69), Color.WHITE);
        stopButton.setEnabled(false);
        
        newSessionButton = createStyledButton("新会话", new Color(33, 150, 243), Color.WHITE);
        saveSessionButton = createStyledButton("保存会话", new Color(255, 193, 7), Color.BLACK);
    }
    
    private void setupLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        
        // 摄像头选择
        add(createLabeledComponent("摄像头: ", cameraComboBox));
        
        // 分辨率选择
        add(createLabeledComponent("分辨率: ", resolutionComboBox));
        
        // 天文预设
        add(createLabeledComponent("预设: ", presetComboBox));
        
        add(new JSeparator(SwingConstants.VERTICAL));
        
        // 主控按钮
        add(startButton);
        add(stopButton);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(newSessionButton);
        add(saveSessionButton);
    }
    
    private void setupEventListeners() {
        // Event listeners will be added by parent frame
    }
    
    private JPanel createLabeledComponent(String labelText, JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        panel.add(label);
        panel.add(component);
        return panel;
    }
    
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
    
    // Getters for access from parent class
    
    public JComboBox<OpenCVCameraManager.CameraDevice> getCameraComboBox() { return cameraComboBox; }
    public JComboBox<String> getResolutionComboBox() { return resolutionComboBox; }
    public JComboBox<UGreenCameraConfig.AstronomyPreset> getPresetComboBox() { return presetComboBox; }
    
    public JButton getStartButton() { return startButton; }
    public JButton getStopButton() { return stopButton; }
    public JButton getNewSessionButton() { return newSessionButton; }
    public JButton getSaveSessionButton() { return saveSessionButton; }
}
