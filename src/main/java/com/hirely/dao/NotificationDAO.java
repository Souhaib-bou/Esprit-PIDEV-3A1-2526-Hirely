package com.hirely.dao;

import com.hirely.config.DatabaseConnection;
import com.hirely.models.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Notification operations
 */
public class NotificationDAO {

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(int userId, int limit) {
        List<Notification> notifications = new ArrayList<>();

        String query = "SELECT notification_id, user_id, interview_id, notification_type, " +
                      "title, message, is_read, created_at, read_at " +
                      "FROM notifications " +
                      "WHERE user_id = ? " +
                      "ORDER BY created_at DESC " +
                      "LIMIT ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Notification notification = new Notification();
                notification.setNotificationId(rs.getInt("notification_id"));
                notification.setUserId(rs.getInt("user_id"));
                notification.setInterviewId(rs.getInt("interview_id"));
                notification.setNotificationType(rs.getString("notification_type"));
                notification.setTitle(rs.getString("title"));
                notification.setMessage(rs.getString("message"));
                notification.setRead(rs.getBoolean("is_read"));
                notification.setCreatedAt(rs.getTimestamp("created_at"));
                notification.setReadAt(rs.getTimestamp("read_at"));
                notifications.add(notification);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting notifications:");
            e.printStackTrace();
        }

        return notifications;
    }

    /**
     * Get unread notification count
     */
    public int getUnreadCount(int userId) {
        String query = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting unread count:");
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Mark notification as read
     */
    public boolean markAsRead(int notificationId) {
        String query = "UPDATE notifications SET is_read = TRUE, read_at = NOW() " +
                      "WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, notificationId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking notification as read:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    public boolean markAllAsRead(int userId) {
        String query = "UPDATE notifications SET is_read = TRUE, read_at = NOW() " +
                      "WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error marking all as read:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create a notification
     */
    public boolean createNotification(Notification notification) {
        String query = "INSERT INTO notifications " +
                      "(user_id, interview_id, notification_type, title, message, is_read) " +
                      "VALUES (?, ?, ?, ?, ?, FALSE)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, notification.getUserId());

            if (notification.getInterviewId() > 0) {
                stmt.setInt(2, notification.getInterviewId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, notification.getNotificationType());
            stmt.setString(4, notification.getTitle());
            stmt.setString(5, notification.getMessage());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Notification created: " + notification.getTitle());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error creating notification:");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete old read notifications (cleanup)
     */
    public int deleteOldNotifications(int userId, int daysOld) {
        String query = "DELETE FROM notifications " +
                      "WHERE user_id = ? AND is_read = TRUE " +
                      "AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, daysOld);

            return stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Error deleting old notifications:");
            e.printStackTrace();
            return 0;
        }
    }
}
