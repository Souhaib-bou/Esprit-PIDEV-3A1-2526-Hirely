package com.hirely.utils;

import com.hirely.models.User;

/**
 * Session manager to store current logged-in user
 * Uses singleton pattern to maintain user session across the application
 */
public class SessionManager {

    private static User currentUser = null;

    /**
     * Private constructor to prevent instantiation
     */
    private SessionManager() {
    }

    /**
     * Set the current logged-in user
     * @param user The user to set as current
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            System.out.println("✅ Session started for: " + user.getEmail() + " (" + user.getRoleName() + ")");
        }
    }

    /**
     * Get the current logged-in user
     * @return Current user or null if no user is logged in
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if a user is currently logged in
     * @return true if user is logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Logout the current user
     */
    public static void logout() {
        if (currentUser != null) {
            System.out.println("✅ Session ended for: " + currentUser.getEmail());
        }
        currentUser = null;
    }

    /**
     * Get the current user's role name
     * @return Role name or null if no user logged in
     */
    public static String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRoleName() : null;
    }

    /**
     * Get the current user's ID
     * @return User ID or 0 if no user logged in
     */
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : 0;
    }

    /**
     * Get the current user's profile ID (recruiter_id or interviewee_id)
     * @return Profile ID or 0 if no user logged in
     */
    public static int getCurrentProfileId() {
        return currentUser != null ? currentUser.getProfileId() : 0;
    }

    /**
     * Check if current user is a recruiter
     */
    public static boolean isRecruiter() {
        return currentUser != null && "RECRUITER".equals(currentUser.getRoleName());
    }

    /**
     * Check if current user is an interviewee
     */
    public static boolean isInterviewee() {
        return currentUser != null && "INTERVIEWEE".equals(currentUser.getRoleName());
    }

    /**
     * Check if current user is an admin
     */
    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRoleName());
    }
}
