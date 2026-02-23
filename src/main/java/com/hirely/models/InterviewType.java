package com.hirely.models;

/**
 * Interview Type model
 */
public class InterviewType {
    private int interviewTypeId;
    private String typeName;
    private String description;
    private int typicalDurationMinutes;
    private boolean isActive;

    // Constructors
    public InterviewType() {
    }

    public InterviewType(int interviewTypeId, String typeName, int typicalDurationMinutes) {
        this.interviewTypeId = interviewTypeId;
        this.typeName = typeName;
        this.typicalDurationMinutes = typicalDurationMinutes;
    }

    // Getters and Setters
    public int getInterviewTypeId() {
        return interviewTypeId;
    }

    public void setInterviewTypeId(int interviewTypeId) {
        this.interviewTypeId = interviewTypeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTypicalDurationMinutes() {
        return typicalDurationMinutes;
    }

    public void setTypicalDurationMinutes(int typicalDurationMinutes) {
        this.typicalDurationMinutes = typicalDurationMinutes;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
