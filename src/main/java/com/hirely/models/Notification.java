package com.hirely.models;

import java.sql.Timestamp;

/**
 * Notification model
 */
public class Notification {
    private int notificationId;
    private int userId;
    private int interviewId;
    private String notificationType;
    private String title;
    private String message;
    private boolean isRead;
    private Timestamp createdAt;
    private Timestamp readAt;

    // Constructors
    public Notification() {
    }

    public Notification(int userId, String notificationType, String title, String message) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.isRead = false;
    }

    // Getters and Setters
    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(int interviewId) {
        this.interviewId = interviewId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getReadAt() {
        return readAt;
    }

    public void setReadAt(Timestamp readAt) {
        this.readAt = readAt;
    }

    public String getTimeAgo() {
        if (createdAt == null) return "";

        long diff = System.currentTimeMillis() - createdAt.getTime();
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days < 7) return days + "d ago";
        return createdAt.toString().substring(0, 10);
    }

    public String getIconEmoji() {
        if (notificationType == null) return "📬";

        switch (notificationType) {
            case "INTERVIEW_SCHEDULED":
            case "INTERVIEW_INVITATION":
                return "📅";
            case "INTERVIEW_CONFIRMED":
                return "✅";
            case "INTERVIEW_CANCELLED":
                return "❌";
            case "INTERVIEW_RESCHEDULED":
                return "🔄";
            case "EVALUATION_COMPLETED":
                return "⭐";
            case "INTERVIEW_REMINDER":
                return "⏰";
            default:
                return "📬";
        }
    }
}
