package com.ocrsearch.model;

import java.time.LocalDateTime;

public class Document {
    private int id;
    private String filename;
    private String text;
    private LocalDateTime uploadTime;
    private double confidence;

    // Constructors
    public Document() {}

    public Document(String filename, String text, double confidence) {
        this.filename = filename;
        this.text = text;
        this.confidence = confidence;
        this.uploadTime = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    @Override
    public String toString() {
        return String.format("ID: %d | Filename: %s | Confidence: %.2f%% | Uploaded: %s",
                id, filename, confidence, uploadTime.toString().substring(0, 19));
    }
}