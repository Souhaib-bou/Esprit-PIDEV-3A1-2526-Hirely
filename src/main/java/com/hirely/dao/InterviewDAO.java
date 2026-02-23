package com.hirely.dao;

import com.hirely.config.DatabaseConnection;
import com.hirely.models.Interview;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Interview operations
 */
public class InterviewDAO {

    /**
     * Get total interview count for a recruiter
     */
    public int getInterviewCount(int recruiterId) {
        String query = "SELECT COUNT(*) FROM interviews WHERE recruiter_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, recruiterId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting interview count:");
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get count of interviews scheduled for today
     */
    public int getTodayInterviewCount(int recruiterId) {
        String query = "SELECT COUNT(*) FROM interviews " +
                      "WHERE recruiter_id = ? AND scheduled_date = CURDATE() " +
                      "AND status IN ('SCHEDULED', 'CONFIRMED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, recruiterId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting today's interview count:");
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get count of pending evaluations (completed interviews without evaluation)
     */
    public int getPendingEvaluationsCount(int recruiterId) {
        String query = "SELECT COUNT(*) FROM interviews i " +
                      "LEFT JOIN interview_evaluations e ON i.interview_id = e.interview_id " +
                      "WHERE i.recruiter_id = ? AND i.status = 'COMPLETED' " +
                      "AND e.evaluation_id IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, recruiterId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting pending evaluations count:");
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get upcoming interviews for a recruiter (next N days)
     */
    public List<Interview> getUpcomingInterviews(int recruiterId, int days) {
        List<Interview> interviews = new ArrayList<>();

        String query = "SELECT " +
                      "i.interview_id, i.scheduled_date, i.scheduled_time, i.duration_minutes, " +
                      "i.status, i.location, i.meeting_link, i.interview_round, " +
                      "it.type_name AS interview_type, " +
                      "CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name, " +
                      "u.email AS candidate_email, " +
                      "jo.title AS job_title " +
                      "FROM interviews i " +
                      "JOIN interview_types it ON i.interview_type_id = it.interview_type_id " +
                      "JOIN interviewee_profiles ip ON i.interviewee_id = ip.interviewee_id " +
                      "JOIN users u ON ip.user_id = u.user_id " +
                      "JOIN applications app ON i.application_id = app.application_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "WHERE i.recruiter_id = ? " +
                      "AND i.scheduled_date >= CURDATE() " +
                      "AND i.scheduled_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                      "AND i.status IN ('SCHEDULED', 'CONFIRMED', 'RESCHEDULED') " +
                      "ORDER BY i.scheduled_date, i.scheduled_time";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, recruiterId);
            stmt.setInt(2, days);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Interview interview = new Interview();
                interview.setInterviewId(rs.getInt("interview_id"));
                interview.setScheduledDate(rs.getDate("scheduled_date"));
                interview.setScheduledTime(rs.getTime("scheduled_time"));
                interview.setDurationMinutes(rs.getInt("duration_minutes"));
                interview.setStatus(rs.getString("status"));
                interview.setLocation(rs.getString("location"));
                interview.setMeetingLink(rs.getString("meeting_link"));
                interview.setInterviewRound(rs.getInt("interview_round"));
                interview.setInterviewTypeName(rs.getString("interview_type"));
                interview.setCandidateName(rs.getString("candidate_name"));
                interview.setCandidateEmail(rs.getString("candidate_email"));
                interview.setJobTitle(rs.getString("job_title"));

                interviews.add(interview);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting upcoming interviews:");
            e.printStackTrace();
        }

        return interviews;
    }

    /**
     * Get all interviews for a recruiter (including past and completed)
     */
    public List<Interview> getAllInterviews(int recruiterId) {
        List<Interview> interviews = new ArrayList<>();

        String query = "SELECT " +
                      "i.interview_id, i.scheduled_date, i.scheduled_time, i.duration_minutes, " +
                      "i.status, i.location, i.meeting_link, i.interview_round, " +
                      "it.type_name AS interview_type, " +
                      "CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name, " +
                      "u.email AS candidate_email, " +
                      "jo.title AS job_title " +
                      "FROM interviews i " +
                      "JOIN interview_types it ON i.interview_type_id = it.interview_type_id " +
                      "JOIN interviewee_profiles ip ON i.interviewee_id = ip.interviewee_id " +
                      "JOIN users u ON ip.user_id = u.user_id " +
                      "JOIN applications app ON i.application_id = app.application_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "WHERE i.recruiter_id = ? " +
                      "ORDER BY i.scheduled_date DESC, i.scheduled_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, recruiterId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Interview interview = new Interview();
                interview.setInterviewId(rs.getInt("interview_id"));
                interview.setScheduledDate(rs.getDate("scheduled_date"));
                interview.setScheduledTime(rs.getTime("scheduled_time"));
                interview.setDurationMinutes(rs.getInt("duration_minutes"));
                interview.setStatus(rs.getString("status"));
                interview.setLocation(rs.getString("location"));
                interview.setMeetingLink(rs.getString("meeting_link"));
                interview.setInterviewRound(rs.getInt("interview_round"));
                interview.setInterviewTypeName(rs.getString("interview_type"));
                interview.setCandidateName(rs.getString("candidate_name"));
                interview.setCandidateEmail(rs.getString("candidate_email"));
                interview.setJobTitle(rs.getString("job_title"));

                interviews.add(interview);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting all interviews:");
            e.printStackTrace();
        }

        return interviews;
    }

    /**
     * Get all interview types
     */
    public List<com.hirely.models.InterviewType> getInterviewTypes() {
        List<com.hirely.models.InterviewType> types = new ArrayList<>();

        String query = "SELECT interview_type_id, type_name, description, typical_duration_minutes, is_active " +
                      "FROM interview_types WHERE is_active = TRUE ORDER BY type_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                com.hirely.models.InterviewType type = new com.hirely.models.InterviewType();
                type.setInterviewTypeId(rs.getInt("interview_type_id"));
                type.setTypeName(rs.getString("type_name"));
                type.setDescription(rs.getString("description"));
                type.setTypicalDurationMinutes(rs.getInt("typical_duration_minutes"));
                type.setActive(rs.getBoolean("is_active"));
                types.add(type);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting interview types:");
            e.printStackTrace();
        }

        return types;
    }

    /**
     * Get shortlisted applications (ready for interview)
     */
    public List<com.hirely.models.Application> getShortlistedApplications() {
        List<com.hirely.models.Application> applications = new ArrayList<>();

        String query = "SELECT " +
                      "app.application_id, app.job_offer_id, app.interviewee_id, " +
                      "app.status, app.applied_date, " +
                      "CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name, " +
                      "u.email AS candidate_email, " +
                      "jo.title AS job_title, " +
                      "ip.skills AS candidate_skills " +
                      "FROM applications app " +
                      "JOIN interviewee_profiles ip ON app.interviewee_id = ip.interviewee_id " +
                      "JOIN users u ON ip.user_id = u.user_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "WHERE app.status IN ('SHORTLISTED', 'UNDER_REVIEW') " +
                      "ORDER BY app.applied_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                com.hirely.models.Application app = new com.hirely.models.Application();
                app.setApplicationId(rs.getInt("application_id"));
                app.setJobOfferId(rs.getInt("job_offer_id"));
                app.setIntervieweeId(rs.getInt("interviewee_id"));
                app.setStatus(rs.getString("status"));
                app.setAppliedDate(rs.getDate("applied_date"));
                app.setCandidateName(rs.getString("candidate_name"));
                app.setCandidateEmail(rs.getString("candidate_email"));
                app.setJobTitle(rs.getString("job_title"));
                app.setCandidateSkills(rs.getString("candidate_skills"));
                applications.add(app);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting shortlisted applications:");
            e.printStackTrace();
        }

        return applications;
    }

    /**
     * Schedule a new interview
     */
    public boolean scheduleInterview(Interview interview) {
        String query = "INSERT INTO interviews " +
                      "(application_id, recruiter_id, interviewee_id, interview_type_id, " +
                      "scheduled_date, scheduled_time, duration_minutes, location, meeting_link, " +
                      "status, interview_round, notes) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED', ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, interview.getApplicationId());
            stmt.setInt(2, interview.getRecruiterId());
            stmt.setInt(3, interview.getIntervieweeId());
            stmt.setInt(4, interview.getInterviewTypeId());
            stmt.setDate(5, interview.getScheduledDate());
            stmt.setTime(6, interview.getScheduledTime());
            stmt.setInt(7, interview.getDurationMinutes());
            stmt.setString(8, interview.getLocation());
            stmt.setString(9, interview.getMeetingLink());
            stmt.setInt(10, interview.getInterviewRound());
            stmt.setString(11, interview.getNotes());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Update application status
                updateApplicationStatus(interview.getApplicationId(), "INTERVIEW_SCHEDULED");
                System.out.println("✅ Interview scheduled successfully!");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error scheduling interview:");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update an existing interview
     */
    public boolean updateInterview(Interview interview) {
        String query = "UPDATE interviews SET " +
                      "interview_type_id = ?, scheduled_date = ?, scheduled_time = ?, " +
                      "duration_minutes = ?, location = ?, meeting_link = ?, " +
                      "interview_round = ?, notes = ? " +
                      "WHERE interview_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, interview.getInterviewTypeId());
            stmt.setDate(2, interview.getScheduledDate());
            stmt.setTime(3, interview.getScheduledTime());
            stmt.setInt(4, interview.getDurationMinutes());
            stmt.setString(5, interview.getLocation());
            stmt.setString(6, interview.getMeetingLink());
            stmt.setInt(7, interview.getInterviewRound());
            stmt.setString(8, interview.getNotes());
            stmt.setInt(9, interview.getInterviewId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Interview updated successfully!");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error updating interview:");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete an interview permanently
     */
    public boolean deleteInterview(int interviewId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // First, delete related evaluation scores if any
            String deleteScoresQuery = "DELETE FROM evaluation_scores " +
                                      "WHERE evaluation_id IN " +
                                      "(SELECT evaluation_id FROM interview_evaluations WHERE interview_id = ?)";
            PreparedStatement deleteScoresStmt = conn.prepareStatement(deleteScoresQuery);
            deleteScoresStmt.setInt(1, interviewId);
            deleteScoresStmt.executeUpdate();

            // Delete evaluation if any
            String deleteEvalQuery = "DELETE FROM interview_evaluations WHERE interview_id = ?";
            PreparedStatement deleteEvalStmt = conn.prepareStatement(deleteEvalQuery);
            deleteEvalStmt.setInt(1, interviewId);
            deleteEvalStmt.executeUpdate();

            // Delete interview
            String deleteInterviewQuery = "DELETE FROM interviews WHERE interview_id = ?";
            PreparedStatement deleteInterviewStmt = conn.prepareStatement(deleteInterviewQuery);
            deleteInterviewStmt.setInt(1, interviewId);
            int rowsAffected = deleteInterviewStmt.executeUpdate();

            if (rowsAffected > 0) {
                conn.commit();
                System.out.println("✅ Interview deleted permanently!");
                return true;
            } else {
                conn.rollback();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error deleting interview:");
            e.printStackTrace();

            // Rollback on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;

        } finally {
            // Reset auto-commit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update application status
     */
    private boolean updateApplicationStatus(int applicationId, String status) {
        String query = "UPDATE applications SET status = ? WHERE application_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, status);
            stmt.setInt(2, applicationId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating application status:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update interview status
     * @param interviewId Interview ID
     * @param status New status (e.g., "COMPLETED", "CANCELLED", "CONFIRMED")
     * @return true if successful, false otherwise
     */
    public boolean updateInterviewStatus(int interviewId, String status) {
        String query = "UPDATE interviews SET status = ? WHERE interview_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, status);
            stmt.setInt(2, interviewId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Interview status updated to: " + status);
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.err.println("❌ Error updating interview status:");
            e.printStackTrace();
            return false;
        }
    }
}