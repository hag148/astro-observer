package com.astronomy.observer.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * 独立的信息显示面板组件
 * 负责观测会话信息、图像统计、堆叠进度和天体检测结果的显示
 */
public class InfoPanel extends JPanel {
    private JLabel sessionIdLabel;
    private JLabel durationLabel;
    private JLabel observationStatusLabel;
    
    private JLabel imageCountLabel;
    private JLabel stackSizeLabel;
    private JLabel stackQualityLabel;
    
    private JLabel stackProgressLabel;
    private JProgressBar stackProgressBar;
    
    private JTextField targetNameField;
    private JTextArea notesArea;
    
    private JTable detectedObjectsTable;
    private DefaultTableModel detectedObjectsModel;
    
    public InfoPanel() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }
    
    private void initializeComponents() {
        // 会话信息标签
        sessionIdLabel = createInfoLabel("ID: 未开始", Color.WHITE);
        durationLabel = createInfoLabel("时长: 00:00:00", Color.WHITE);
        observationStatusLabel = createInfoLabel("状态: 待机", Color.WHITE);
        
        // 图像统计标签
        imageCountLabel = createInfoLabel("已捕获: 0", Color.WHITE);
        stackSizeLabel = createInfoLabel("堆叠: 0/100", Color.WHITE);
        stackQualityLabel = createInfoLabel("质量: --", Color.WHITE);
        
        // 堆叠进度标签和进度条
        stackProgressLabel = createInfoLabel("完成: 0%", Color.BLACK);
        stackProgressBar = new JProgressBar(0, 100);
        stackProgressBar.setStringPainted(true);
        stackProgressBar.setForeground(Color.GREEN);
        
        // 目标和备注
        targetNameField = new JTextField("观测目标");
        targetNameField.setToolTipText("输入观测目标（如：月球、木星、M31等）");
        
        notesArea = new JTextArea(4, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        
        // 天体检测结果表
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
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));
        TitledBorder infoBorder = BorderFactory.createTitledBorder("观测信息");
        infoBorder.setTitleColor(Color.DARK_GRAY);
        setBorder(infoBorder);
        
        // 信息网格
        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 5, 5));
        infoGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 会话信息面板
        JPanel sessionPanel = createInfoCard("观测会话", new Color(33, 150, 243));
        sessionPanel.add(sessionIdLabel);
        sessionPanel.add(Box.createVerticalStrut(5));
        sessionPanel.add(durationLabel);
        sessionPanel.add(Box.createVerticalStrut(5));
        sessionPanel.add(observationStatusLabel);
        infoGrid.add(sessionPanel);
        
        // 图像信息面板
        JPanel imagePanel = createInfoCard("图像统计", new Color(76, 175, 80));
        imagePanel.add(imageCountLabel);
        imagePanel.add(Box.createVerticalStrut(5));
        imagePanel.add(stackSizeLabel);
        imagePanel.add(Box.createVerticalStrut(5));
        imagePanel.add(stackQualityLabel);
        infoGrid.add(imagePanel);
        
        // 堆叠进度面板
        JPanel progressPanel = createInfoCard("堆叠进度", new Color(255, 193, 7));
        progressPanel.add(stackProgressLabel);
        progressPanel.add(Box.createVerticalStrut(5));
        progressPanel.add(stackProgressBar);
        infoGrid.add(progressPanel);
        
        // 目标面板
        JPanel targetPanel = new JPanel(new BorderLayout(5, 5));
        targetPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        targetPanel.add(new JLabel("目标:"), BorderLayout.NORTH);
        targetPanel.add(targetNameField, BorderLayout.CENTER);
        infoGrid.add(targetPanel);
        
        // 天体检测结果面板
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        tablePanel.add(new JLabel("检测结果:"), BorderLayout.NORTH);
        
        JScrollPane tableScrollPane = new JScrollPane(detectedObjectsTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 150));
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        infoGrid.add(tablePanel);
        
        add(infoGrid, BorderLayout.CENTER);
        
        // 备注面板
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createTitledBorder("观测备注"));
        add(notesScroll, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        // 添加表格点击监听器
        detectedObjectsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = detectedObjectsTable.getSelectedRow();
                if (row >= 0 && row < detectedObjectsModel.getRowCount()) {
                    // Selection handled by parent frame
                }
            }
        });
    }
    
    private JLabel createInfoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        label.setForeground(color);
        return label;
    }
    
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
    
    // Getters for access from parent class
    
    public JLabel getSessionIdLabel() { return sessionIdLabel; }
    public JLabel getDurationLabel() { return durationLabel; }
    public JLabel getObservationStatusLabel() { return observationStatusLabel; }
    
    public JLabel getImageCountLabel() { return imageCountLabel; }
    public JLabel getStackSizeLabel() { return stackSizeLabel; }
    public JLabel getStackQualityLabel() { return stackQualityLabel; }
    
    public JLabel getStackProgressLabel() { return stackProgressLabel; }
    public JProgressBar getStackProgressBar() { return stackProgressBar; }
    
    public JTextField getTargetNameField() { return targetNameField; }
    public JTextArea getNotesArea() { return notesArea; }
    
    public JTable getDetectedObjectsTable() { return detectedObjectsTable; }
    public DefaultTableModel getDetectedObjectsModel() { return detectedObjectsModel; }
}
