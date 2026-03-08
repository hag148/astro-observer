package com.astronomy.observer.gui;

import javax.swing.*;
import java.awt.*;

/**
 * 独立的覆盖信息面板组件
 * 负责在摄像头视图上显示FPS和缩放状态
 */
public class OverlayPanel extends JPanel {
    private JLabel fpsLabel;
    private JLabel zoomStatusLabel;
    
    public OverlayPanel() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        fpsLabel = createInfoLabel("FPS: --", Color.GREEN);
        zoomStatusLabel = createInfoLabel("缩放: 1.0x", Color.YELLOW);
    }
    
    private void setupLayout() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setOpaque(true);
        setBackground(new Color(0, 0, 0, 150));
        
        add(fpsLabel);
        add(zoomStatusLabel);
    }
    
    private JLabel createInfoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }
    
    // Getters for access from parent class
    
    public JLabel getFpsLabel() { return fpsLabel; }
    public JLabel getZoomStatusLabel() { return zoomStatusLabel; }
}
