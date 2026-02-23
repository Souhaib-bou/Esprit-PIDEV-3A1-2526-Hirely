package com.hirely.models;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Interview model
 */
public class Interview {
    private int interviewId;
    private int applicationId;
    private int recruiterId;
    private int intervieweeId;
    private int interviewTypeId;
    private Date scheduledDate;
    private Time scheduledTime;
    private int durationMinutes;
    private String location;
    private String meetingLink;
    private String status;
    private int interviewRound;
    private String notes;
    private String cancellationReason;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Additional fields from joins
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String interviewTypeName;

    // Constructors
    public Interview() {
    }

    // Getters and Setters
    public int getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(int interviewId) {
        this.interviewId = interviewId;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public int getIntervieweeId() {
        return intervieweeId;
    }

    public void setIntervieweeId(int intervieweeId) {
        this.intervieweeId = intervieweeId;
    }

    public int getInterviewTypeId() {
        return interviewTypeId;
    }

    public void setInterviewTypeId(int interviewTypeId) {
        this.interviewTypeId = interviewTypeId;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Time getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Time scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getInterviewRound() {
        return interviewRound;
    }

    public void setInterviewRound(int interviewRound) {
        this.interviewRound = interviewRound;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Additional getters/setters
    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getInterviewTypeName() {
        return interviewTypeName;
    }

    public void setInterviewTypeName(String interviewTypeName) {
        this.interviewTypeName = interviewTypeName;
    }

    public String getFormattedDateTime() {
        return scheduledDate + " at " + scheduledTime;
    }

    @Override
    public String toString() {
        return "Interview{" +
                "interviewId=" + interviewId +
                ", candidate='" + candidateName + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                ", date=" + scheduledDate +
                ", status='" + status + '\'' +
                '}';
    }
}