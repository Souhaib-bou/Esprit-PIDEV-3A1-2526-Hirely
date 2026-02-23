package com.hirely.models;

import java.sql.Timestamp;

/**
 * Recruiter profile model
 */
public class RecruiterProfile {
    private int recruiterId;
    private int userId;
    private String firstName;
    private String lastName;
    private String companyName;
    private String position;
    private String phoneNumber;
    private String department;
    private String bio;
    private String profilePictureUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public RecruiterProfile() {
    }

    public RecruiterProfile(int recruiterId, String firstName, String lastName) {
        this.recruiterId = recruiterId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
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

    @Override
    public String toString() {
        return "RecruiterProfile{" +
                "recruiterId=" + recruiterId +
                ", fullName='" + getFullName() + '\'' +
                ", company='" + companyName + '\'' +
                ", position='" + position + '\'' +
                '}';
    }
}