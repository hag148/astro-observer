package com.astronomy.observer.gui;

import com.astronomy.observer.image.AstronomyImageEnhancer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * 独立的图像增强控制面板组件
 * 负责图像增强算法选择和应用
 */
public class EnhancementControlPanel extends JPanel {
    private JComboBox<AstronomyImageEnhancer.EnhancementAlgorithm> enhancementAlgorithmComboBox;
    private JCheckBox useGPUCheckBox;
    
    public EnhancementControlPanel() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
    }
    
    private void initializeComponents() {
        // 创建增强算法下拉框
        enhancementAlgorithmComboBox = new JComboBox<>(AstronomyImageEnhancer.EnhancementAlgorithm.values());
        enhancementAlgorithmComboBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        enhancementAlgorithmComboBox.setToolTipText("选择图像增强算法");
        
        // GPU 选项
        useGPUCheckBox = new JCheckBox("使用 GPU 加速（需要 CUDA 支持）");
        useGPUCheckBox.setEnabled(false); // 暂时禁用，需要添加 GPU 支持
        useGPUCheckBox.setToolTipText("使用 CUDA GPU 加速（需要 NVIDIA GPU 和 CUDA）");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));
        TitledBorder enhanceBorder = BorderFactory.createTitledBorder("图像增强控制");
        enhanceBorder.setTitleColor(new Color(255, 140, 0));
        setBorder(enhanceBorder);
        
        // 增强算法选择面板
        JPanel algorithmPanel = new JPanel(new BorderLayout(5, 5));
        algorithmPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.add(new JLabel("增强算法:"));
        algorithmPanel.add(labelPanel, BorderLayout.WEST);
        
        algorithmPanel.add(enhancementAlgorithmComboBox, BorderLayout.CENTER);
        algorithmPanel.add(useGPUCheckBox, BorderLayout.SOUTH);
        
        add(algorithmPanel, BorderLayout.NORTH);
        
        // 算法说明区域
        JTextArea descriptionArea = new JTextArea(3, 30);
        descriptionArea.setEditable(false);
        descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        descriptionArea.setBackground(new Color(245, 245, 245));
        descriptionArea.setText(getAlgorithmDescription(AstronomyImageEnhancer.EnhancementAlgorithm.BASIC));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        enhancementAlgorithmComboBox.addActionListener(e -> {
            AstronomyImageEnhancer.EnhancementAlgorithm selected =
                (AstronomyImageEnhancer.EnhancementAlgorithm) enhancementAlgorithmComboBox.getSelectedItem();
            descriptionArea.setText(getAlgorithmDescription(selected));
        });
        
        add(new JScrollPane(descriptionArea), BorderLayout.CENTER);
        
        // 快捷增强按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JButton enhanceStarsButton = new JButton("星点增强");
        enhanceStarsButton.setToolTipText("专门增强星星");
        
        JButton enhanceDeepSkyButton = new JButton("深空增强");
        enhanceDeepSkyButton.setToolTipText("专门增强星云、星系");
        
        JButton enhancePlanetaryButton = new JButton("行星增强");
        enhancePlanetaryButton.setToolTipText("专门增强行星、月球");
        
        JButton enhanceButton = new JButton("应用增强");
        enhanceButton.setToolTipText("应用选择的增强算法");
        
        buttonPanel.add(enhanceStarsButton);
        buttonPanel.add(enhanceDeepSkyButton);
        buttonPanel.add(enhancePlanetaryButton);
        buttonPanel.add(enhanceButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        // Add action listeners for buttons
        // These will be handled by the parent frame
    }
    
    private String getAlgorithmDescription(AstronomyImageEnhancer.EnhancementAlgorithm algorithm) {
        switch (algorithm) {
            case BASIC:
                return "基础增强：直方图均衡化 + 锐化。适合一般天文图像，提高对比度和细节。";
            case CLAHE:
                return "CLAHE：自适应直方图均衡化。保留局部细节，适合增强低对比度区域。";
            case UNSHARP_MASK:
                return "反锐化掩码：增强边缘和细节，适合星点增强，减少模糊。";
            case MORPHOLOGY:
                return "形态学增强：突出亮点，连接断裂星点，适合星点检测。";
            case DENOISE:
                return "降噪增强：双边滤波保留边缘同时降噪，适合高增益图像。";
            case LUCY_RICHARDSON:
                return "Lucy-Richardson：反卷积恢复模糊，适合大气模糊图像。";
            case STRETCHING:
                return "线性拉伸：扩展动态范围，适合深空天体。";
            case GAMMA_CORRECTION:
                return "Gamma 校正：调整亮度和对比度，gamma>1 变暗，<1 变亮。";
            case COMBINED:
                return "综合增强：组合降噪、CLAHE、锐化、形态学和 Gamma，效果最佳。";
            default:
                return "未知算法";
        }
    }
    
    // Getters for access from parent class
    
    public JComboBox<AstronomyImageEnhancer.EnhancementAlgorithm> getEnhancementAlgorithmComboBox() {
        return enhancementAlgorithmComboBox;
    }
    
    public JCheckBox getUseGPUCheckBox() {
        return useGPUCheckBox;
    }
}
