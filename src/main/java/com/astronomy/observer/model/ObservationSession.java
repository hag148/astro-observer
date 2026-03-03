package com.astronomy.observer.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 观测会话
 */
public class ObservationSession {
    private String id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String targetName;
    private String preset;
    private int imageCount;
    private String location;
    private String notes;

    public ObservationSession() {
        this.id = "OBS_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.startTime = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public void incrementImageCount() {
        this.imageCount++;
    }

    public void endSession() {
        this.endTime = LocalDateTime.now();
    }

    public long getDurationSeconds() {
        if (endTime == null || startTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }

    @Override
    public String toString() {
        return String.format("Observation[%s, target=%s, preset=%s, images=%d, duration=%ds]",
            id, targetName, preset, imageCount, getDurationSeconds());
    }
}
