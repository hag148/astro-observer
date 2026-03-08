package com.astronomy.observer.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 独立的参数控制面板组件
 * 负责摄像头参数（曝光、增益、对焦、亮度、对比度）的控制
 */
public class ParametersControlPanel extends JPanel {
    private JSlider exposureSlider;
    private JSlider gainSlider;
    private JSlider brightnessSlider;
    private JSlider contrastSlider;
    private JSlider focusSlider;
    
    private JSpinner exposureSpinner;
    private JSpinner gainSpinner;
    private JSpinner brightnessSpinner;
    private JSpinner contrastSpinner;
    private JSpinner focusSpinner;
    
    public ParametersControlPanel() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }
    
    private void initializeComponents() {
        // 曝光时间
        exposureSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10));
        exposureSlider = new JSlider(10, 1000, 100);
        
        // 增益
        gainSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        gainSlider = new JSlider(0, 100, 50);
        
        // 对焦
        focusSpinner = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));
        focusSlider = new JSlider(0, 255, 128);
        
        // 亮度
        brightnessSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        brightnessSlider = new JSlider(0, 100, 50);
        
        // 对比度
        contrastSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        contrastSlider = new JSlider(0, 100, 50);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));
        TitledBorder paramsBorder = BorderFactory.createTitledBorder("摄像头参数控制");
        paramsBorder.setTitleColor(Color.DARK_GRAY);
        setBorder(paramsBorder);
        
        // 参数控制网格
        JPanel paramsGrid = new JPanel(new GridLayout(5, 2, 5, 10));
        paramsGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 曝光时间
        paramsGrid.add(createSliderControl("曝光时间 (ms)", exposureSpinner, exposureSlider, "ms"));
        
        // 增益
        paramsGrid.add(createSliderControl("增益", gainSpinner, gainSlider, ""));
        
        // 对焦
        paramsGrid.add(createSliderControl("对焦", focusSpinner, focusSlider, ""));
        
        // 亮度
        paramsGrid.add(createSliderControl("亮度", brightnessSpinner, brightnessSlider, ""));
        
        // 对比度
        paramsGrid.add(createSliderControl("对比度", contrastSpinner, contrastSlider, ""));
        
        add(paramsGrid, BorderLayout.CENTER);
    }
    
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
    
    private void setupEventListeners() {
        // 同步滑块和输入框
        syncSpinnerSlider(exposureSpinner, exposureSlider);
        syncSpinnerSlider(gainSpinner, gainSlider);
        syncSpinnerSlider(focusSpinner, focusSlider);
        syncSpinnerSlider(brightnessSpinner, brightnessSlider);
        syncSpinnerSlider(contrastSpinner, contrastSlider);
    }
    
    private void syncSpinnerSlider(JSpinner spinner, JSlider slider) {
        spinner.addChangeListener(e -> {
            slider.setValue((Integer) spinner.getValue());
        });
        slider.addChangeListener(e -> {
            spinner.setValue(slider.getValue());
        });
    }
    
    // Getters for access from parent class
    
    public JSlider getExposureSlider() { return exposureSlider; }
    public JSlider getGainSlider() { return gainSlider; }
    public JSlider getBrightnessSlider() { return brightnessSlider; }
    public JSlider getContrastSlider() { return contrastSlider; }
    public JSlider getFocusSlider() { return focusSlider; }
    
    public JSpinner getExposureSpinner() { return exposureSpinner; }
    public JSpinner getGainSpinner() { return gainSpinner; }
    public JSpinner getBrightnessSpinner() { return brightnessSpinner; }
    public JSpinner getContrastSpinner() { return contrastSpinner; }
    public JSpinner getFocusSpinner() { return focusSpinner; }
}
