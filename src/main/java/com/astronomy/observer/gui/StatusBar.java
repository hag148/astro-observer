package com.astronomy.observer.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 独立的状态栏组件
 * 负责状态信息和内存使用显示
 */
public class StatusBar extends JPanel {
    private JLabel statusLabel;
    
    public StatusBar() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }
    
    private void initializeComponents() {
        statusLabel = new JLabel("准备就绪");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(5, 0));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        setPreferredSize(new Dimension(0, 28));
        
        add(statusLabel, BorderLayout.WEST);
        
        // 内存使用显示
        JLabel memoryLabel = new JLabel();
        memoryLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        memoryLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        Timer memoryTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runtime runtime = Runtime.getRuntime();
                long used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                long max = runtime.maxMemory() / 1024 / 1024;
                memoryLabel.setText(String.format("内存: %d / %d MB", used, max));
            }
        });
        memoryTimer.start();
        
        add(memoryLabel, BorderLayout.EAST);
    }
    
    private void setupEventListeners() {
        // Event listeners will be added by parent frame
    }
    
    // Getters for access from parent class
    
    public JLabel getStatusLabel() { return statusLabel; }
}
