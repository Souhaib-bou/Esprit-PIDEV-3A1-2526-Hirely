package com.hirely.models;

import java.sql.Date;

/**
 * Application model (minimal - for linking to interviews)
 */
public class Application {
    private int applicationId;
    private int jobOfferId;
    private int intervieweeId;
    private String status;
    private Date appliedDate;

    // Additional fields from joins
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String candidateSkills;

    // Constructors
    public Application() {
    }

    // Getters and Setters
    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getJobOfferId() {
        return jobOfferId;
    }

    public void setJobOfferId(int jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public int getIntervieweeId() {
        return intervieweeId;
    }

    public void setIntervieweeId(int intervieweeId) {
        this.intervieweeId = intervieweeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(Date appliedDate) {
        this.appliedDate = appliedDate;
    }

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

    public String getCandidateSkills() {
        return candidateSkills;
    }

    public void setCandidateSkills(String candidateSkills) {
        this.candidateSkills = candidateSkills;
    }

    public String getDisplayText() {
        return candidateName + " - " + jobTitle + " (" + status + ")";
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
