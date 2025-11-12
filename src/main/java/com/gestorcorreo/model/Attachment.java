package com.gestorcorreo.model;

import java.io.File;

/**
 * Representa un archivo adjunto en un correo electrónico
 */
public class Attachment {
    private String fileName;
    private String filePath;
    private String mimeType;
    private long fileSize;
    private byte[] content;
    private boolean inline; // si es contenido embebido (cid)
    private String contentId; // Content-ID para imágenes inline

    public Attachment() {
    }

    public Attachment(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public Attachment(File file) {
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
    }

    // Getters y Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.fileSize = content != null ? content.length : 0;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + formatFileSize(fileSize) +
                ", mimeType='" + mimeType + '\'' +
                ", inline=" + inline +
                '}';
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}
