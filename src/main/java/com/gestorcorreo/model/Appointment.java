package com.gestorcorreo.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de cita para el calendario.
 */
public class Appointment {
    private String id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private String location;
    private boolean allDay;
    private LocalDateTime createdAt;

    public Appointment() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public Appointment(String title, String description, LocalDateTime start, LocalDateTime end, String location, boolean allDay) {
        this();
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.location = location;
        this.allDay = allDay;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean overlaps(Appointment other) {
        if (this.allDay || other.allDay) return false; // Simple: ignorar solapado para eventos all-day
        if (this.start == null || this.end == null || other.start == null || other.end == null) return false;
        return !this.end.isBefore(other.start) && !other.end.isBefore(this.start);
    }
}
