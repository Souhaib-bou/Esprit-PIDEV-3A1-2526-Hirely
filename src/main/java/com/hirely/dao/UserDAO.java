package com.hirely.dao;

import com.hirely.config.DatabaseConnection;
import com.hirely.models.Role;
import com.hirely.models.User;

import java.sql.*;

/**
 * Data Access Object for User operations
 */
public class UserDAO {

    /**
     * Authenticate user with email and password
     * @param email User email
     * @param plainPassword Plain text password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticateUser(String email, String plainPassword) {
        String query = "SELECT u.user_id, u.email, u.password_hash, u.role_id, u.is_active, " +
                      "u.is_verified, r.role_name, " +
                      "CASE " +
                      "  WHEN r.role_name = 'RECRUITER' THEN rp.recruiter_id " +
                      "  WHEN r.role_name = 'INTERVIEWEE' THEN ip.interviewee_id " +
                      "  ELSE 0 " +
                      "END AS profile_id, " +
                      "CASE " +
                      "  WHEN r.role_name = 'RECRUITER' THEN rp.first_name " +
                      "  WHEN r.role_name = 'INTERVIEWEE' THEN ip.first_name " +
                      "  ELSE 'Admin' " +
                      "END AS first_name, " +
                      "CASE " +
                      "  WHEN r.role_name = 'RECRUITER' THEN rp.last_name " +
                      "  WHEN r.role_name = 'INTERVIEWEE' THEN ip.last_name " +
                      "  ELSE 'User' " +
                      "END AS last_name " +
                      "FROM users u " +
                      "JOIN roles r ON u.role_id = r.role_id " +
                      "LEFT JOIN recruiter_profiles rp ON u.user_id = rp.user_id " +
                      "LEFT JOIN interviewee_profiles ip ON u.user_id = ip.user_id " +
                      "WHERE u.email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Check if account is active
                if (!rs.getBoolean("is_active")) {
                    System.out.println("❌ Account is deactivated");
                    return null;
                }

                // Verify password (plain text comparison)
                String storedPassword = rs.getString("password_hash");
                if (!plainPassword.equals(storedPassword)) {
                    System.out.println("❌ Invalid password");
                    return null;
                }

                // Create user object
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(storedPassword);
                user.setRoleId(rs.getInt("role_id"));
                user.setActive(rs.getBoolean("is_active"));
                user.setVerified(rs.getBoolean("is_verified"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setProfileId(rs.getInt("profile_id"));

                // Update last login
                updateLastLogin(user.getUserId());

                System.out.println("✅ Authentication successful for: " + email);
                return user;
            }

            System.out.println("❌ User not found: " + email);
            return null;

        } catch (SQLException e) {
            System.err.println("❌ Error during authentication:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get user by ID
     * @param userId User ID
     * @return User object or null if not found
     */
    public User getUserById(int userId) {
        String query = "SELECT u.user_id, u.email, u.password_hash, u.role_id, u.is_active, " +
                      "u.is_verified, r.role_name, " +
                      "CASE " +
                      "  WHEN r.role_name = 'RECRUITER' THEN rp.recruiter_id " +
                      "  WHEN r.role_name = 'INTERVIEWEE' THEN ip.interviewee_id " +
                      "  ELSE 0 " +
                      "END AS profile_id, " +
                      "CASE " +
                      "  WHEN r.role_name = 'RECRUITER' THEN rp.first_name " +
                      "  WHEN r.role_name = 'INTERVIEWEE' THEN ip.first_name " +
                      "  ELSE 'Admin' " +
                      "END AS first_name, " +
                      "CASE " +
                      "  WHEN r.role_name = 'RECRUITER' THEN rp.last_name " +
                      "  WHEN r.role_name = 'INTERVIEWEE' THEN ip.last_name " +
                      "  ELSE 'User' " +
                      "END AS last_name " +
                      "FROM users u " +
                      "JOIN roles r ON u.role_id = r.role_id " +
                      "LEFT JOIN recruiter_profiles rp ON u.user_id = rp.user_id " +
                      "LEFT JOIN interviewee_profiles ip ON u.user_id = ip.user_id " +
                      "WHERE u.user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setRoleId(rs.getInt("role_id"));
                user.setActive(rs.getBoolean("is_active"));
                user.setVerified(rs.getBoolean("is_verified"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setProfileId(rs.getInt("profile_id"));
                return user;
            }

            return null;

        } catch (SQLException e) {
            System.err.println("❌ Error getting user by ID:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update last login timestamp
     * @param userId User ID
     * @return true if successful, false otherwise
     */
    public boolean updateLastLogin(int userId) {
        String query = "UPDATE users SET last_login = NOW() WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating last login:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if email exists
     * @param email Email to check
     * @return true if exists, false otherwise
     */
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;

        } catch (SQLException e) {
            System.err.println("❌ Error checking email existence:");
            e.printStackTrace();
            return false;
        }
    }
}
