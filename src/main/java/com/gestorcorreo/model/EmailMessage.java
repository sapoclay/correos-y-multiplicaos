package com.gestorcorreo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un mensaje de correo electrónico
 */
public class EmailMessage {
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private boolean isHtml;
    private LocalDateTime sentDate;
    private LocalDateTime receivedDate;
    private List<Attachment> attachments;
    private String messageId;
    private boolean isRead;
    private List<String> tags; // Lista de nombres de etiquetas

    public EmailMessage() {
        this.to = new ArrayList<>();
        this.cc = new ArrayList<>();
        this.bcc = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.isHtml = false;
        this.isRead = false;
    }

    public EmailMessage(String from, String to, String subject, String body) {
        this();
        this.from = from;
        this.to.add(to);
        this.subject = subject;
        this.body = body;
    }

    // Getters y Setters
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public void addTo(String email) {
        this.to.add(email);
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public void addCc(String email) {
        this.cc.add(email);
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }

    public void addBcc(String email) {
        this.bcc.add(email);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setHtml(boolean html) {
        isHtml = html;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tagName) {
        if (!this.tags.contains(tagName)) {
            this.tags.add(tagName);
        }
    }

    public void removeTag(String tagName) {
        this.tags.remove(tagName);
    }

    public boolean hasTag(String tagName) {
        return this.tags.contains(tagName);
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
                "from='" + from + '\'' +
                ", to=" + to +
                ", subject='" + subject + '\'' +
                ", sentDate=" + sentDate +
                ", isRead=" + isRead +
                ", attachments=" + attachments.size() +
                '}';
    }
}
