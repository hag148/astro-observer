package com.astronomy.observer.gui;

import com.astronomy.observer.image.OpenCVImageProcessor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 独立的摄像头视图面板组件
 * 负责图像显示、缩放、拖拽和天体检测结果绘制
 */
public class CameraViewPanel extends JPanel {
    private JLabel cameraLabel;
    private BufferedImage currentImage;
    
    // 缩放控制
    private double zoomLevel = 1.0;
    private Point viewOffset = new Point(0, 0);
    private final double MIN_ZOOM = 1.0;
    private final double MAX_ZOOM = 100.0;
    private final double ZOOM_FACTOR = 1.1;
    
    // 拖拽相关
    private Point dragStartPoint = null;
    private Point dragStartOffset = null;
    
    // 天体检测结果
    private List<OpenCVImageProcessor.DetectedObject> detectedObjects;
    private int selectedObjectIndex = -1;
    
    public CameraViewPanel() {
        initializeComponents();
        setupEventListeners();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        // 摄像头显示标签
        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cameraLabel.setVerticalAlignment(SwingConstants.CENTER);
        cameraLabel.setOpaque(true);
        cameraLabel.setBackground(Color.BLACK);
        
        // 添加滚动支持
        JScrollPane scrollPane = new JScrollPane(cameraLabel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupEventListeners() {
        // 鼠标监听器
        cameraLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentImage != null) {
                    dragStartPoint = e.getPoint();
                    dragStartOffset = new Point(viewOffset);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartPoint = null;
                dragStartOffset = null;
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // 双击处理在父类中处理
            }
        });
        
        cameraLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
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
    }
    
    /**
     * 处理拖拽平移（类似地图）
     */
    private void handleDrag(MouseEvent e) {
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
    private void handleMouseWheel(MouseWheelEvent e) {
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
            Point newOriginalPoint = screenToImage(mousePoint, newScale);
            
            viewOffset.x += (int) (originalPoint.x - newOriginalPoint.x);
            viewOffset.y += (int) (originalPoint.y - newOriginalPoint.y);
            
            clampViewOffset();
        }
        
        updateCameraLabel();
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
    
    /**
     * 更新摄像头标签显示
     */
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
                
                // 创建固定大小的显示图像
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
                
                // 应用缩放变换
                g2d.translate(labelWidth / 2, labelHeight / 2);
                g2d.scale(finalScale, finalScale);
                g2d.translate(-currentImage.getWidth() / 2, -currentImage.getHeight() / 2);
                g2d.translate(-viewOffset.x, -viewOffset.y);
                
                // 直接绘制原图
                g2d.drawImage(currentImage, 0, 0, null);
                
                // 恢复原始变换
                g2d.setTransform(originalTransform);
                
                // 绘制天体检测结果
                drawDetectedObjects(g2d, labelWidth, labelHeight, finalScale);
                
                g2d.dispose();
                cameraLabel.setIcon(new ImageIcon(displayImage));
            }
        }
    }
    
    /**
     * 绘制检测到的天体
     */
    private void drawDetectedObjects(Graphics2D g2d, int labelWidth, int labelHeight, double finalScale) {
        if (detectedObjects != null && !detectedObjects.isEmpty()) {
            // 先绘制所有检测到的天体（绿色小圈）
            for (int i = 0; i < detectedObjects.size(); i++) {
                OpenCVImageProcessor.DetectedObject obj = detectedObjects.get(i);
                
                // 计算天体在屏幕中的位置
                double scaledX = (obj.x - viewOffset.x) * finalScale;
                double scaledY = (obj.y - viewOffset.y) * finalScale;
                double displayX = scaledX + (labelWidth - currentImage.getWidth() * finalScale) / 2;
                double displayY = scaledY + (labelHeight - currentImage.getHeight() * finalScale) / 2;
                
                // 检查是否在可见范围内
                if (displayX > -100 && displayX < labelWidth + 100 &&
                    displayY > -100 && displayY < labelHeight + 100) {
                    
                    // 计算圆的半径（基于面积，并应用缩放）
                    double radius = Math.sqrt(obj.area) * finalScale / 2;
                    int markerSize = Math.max(5, (int) (5 * zoomLevel));
                    
                    // 绘制绿色小圈标记所有检测到的天体
                    g2d.setColor(new Color(0, 255, 0, 150));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval((int) displayX - markerSize, (int) displayY - markerSize,
                                 markerSize * 2, markerSize * 2);
                }
            }
            
            // 如果有选中的天体，绘制详细标识
            if (selectedObjectIndex >= 0 && selectedObjectIndex < detectedObjects.size()) {
                OpenCVImageProcessor.DetectedObject obj = detectedObjects.get(selectedObjectIndex);
                
                // 计算天体在屏幕中的位置
                double scaledX = (obj.x - viewOffset.x) * finalScale;
                double scaledY = (obj.y - viewOffset.y) * finalScale;
                double displayX = scaledX + (labelWidth - currentImage.getWidth() * finalScale) / 2;
                double displayY = scaledY + (labelHeight - currentImage.getHeight() * finalScale) / 2;
                
                if (displayX > -100 && displayX < labelWidth + 100 &&
                    displayY > -100 && displayY < labelHeight + 100) {
                    
                    // 计算圆的半径（基于面积，并应用缩放）
                    double radius = Math.sqrt(obj.area) * finalScale / 2;
                    
                    // 绘制黄色圆圈标识
                    g2d.setColor(Color.YELLOW);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(
                        (int) (displayX - radius),
                        (int) (displayY - radius),
                        (int) (radius * 2),
                        (int) (radius * 2)
                    );
                    
                    // 绘制十字准星
                    g2d.setColor(Color.RED);
                    g2d.setStroke(new BasicStroke(1));
                    int crossSize = Math.max(10, (int) (10 * zoomLevel));
                    g2d.drawLine((int) displayX - crossSize, (int) displayY, (int) displayX + crossSize, (int) displayY);
                    g2d.drawLine((int) displayX, (int) displayY - crossSize, (int) displayX, (int) displayY + crossSize);
                    
                    // 绘制标签
                    g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, Math.max(12, (int) (12 * zoomLevel))));
                    g2d.setColor(Color.GREEN);
                    String label = String.format("天体 #%d", selectedObjectIndex + 1);
                    FontMetrics fm = g2d.getFontMetrics();
                    g2d.drawString(label, (int) displayX + (int) radius + 5, (int) displayY - 5);
                    
                    // 绘制天体信息背景框
                    String infoText = String.format("X:%.1f Y:%.1f 亮:%.1f", obj.x, obj.y, obj.brightness);
                    int infoWidth = fm.stringWidth(infoText) + 10;
                    int infoHeight = fm.getHeight() + 4;
                    g2d.setColor(new Color(0, 0, 0, 180));
                    g2d.fillRect((int) displayX + (int) radius + 5, (int) displayY + 8, infoWidth, infoHeight);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(infoText, (int) displayX + (int) radius + 10, (int) displayY + fm.getAscent() + 8);
                }
            }
        }
    }
    
    // Getters and Setters
    
    public void setCurrentImage(BufferedImage currentImage) {
        this.currentImage = currentImage;
        updateCameraLabel();
    }
    
    public void setDetectedObjects(List<OpenCVImageProcessor.DetectedObject> detectedObjects) {
        this.detectedObjects = detectedObjects;
        updateCameraLabel();
    }
    
    public void setSelectedObjectIndex(int selectedObjectIndex) {
        this.selectedObjectIndex = selectedObjectIndex;
        updateCameraLabel();
    }
    
    public void resetZoom() {
        zoomLevel = 1.0;
        viewOffset = new Point(0, 0);
        updateCameraLabel();
    }
    
    public double getZoomLevel() {
        return zoomLevel;
    }
    
    public Point getViewOffset() {
        return viewOffset;
    }
}
