package com.hirely.dao;

import com.hirely.config.DatabaseConnection;
import com.hirely.models.EvaluationCriteria;
import com.hirely.models.EvaluationScore;
import com.hirely.models.InterviewEvaluation;
import com.hirely.models.Interview;
import com.hirely.services.NotificationService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Evaluation operations
 */
public class EvaluationDAO {

    /**
     * Get all active evaluation criteria
     */
    public List<EvaluationCriteria> getActiveCriteria() {
        List<EvaluationCriteria> criteria = new ArrayList<>();

        String query = "SELECT criteria_id, criteria_name, description, max_score, " +
                      "weight, category, is_active, display_order " +
                      "FROM evaluation_criteria " +
                      "WHERE is_active = TRUE " +
                      "ORDER BY display_order";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                EvaluationCriteria criterion = new EvaluationCriteria();
                criterion.setCriteriaId(rs.getInt("criteria_id"));
                criterion.setCriteriaName(rs.getString("criteria_name"));
                criterion.setDescription(rs.getString("description"));
                criterion.setMaxScore(rs.getInt("max_score"));
                criterion.setWeight(rs.getDouble("weight"));
                criterion.setCategory(rs.getString("category"));
                criterion.setActive(rs.getBoolean("is_active"));
                criterion.setDisplayOrder(rs.getInt("display_order"));
                criteria.add(criterion);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting evaluation criteria:");
            e.printStackTrace();
        }

        return criteria;
    }

    /**
     * Submit interview evaluation
     */
    public boolean submitEvaluation(InterviewEvaluation evaluation) {
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Insert evaluation
            String evalQuery = "INSERT INTO interview_evaluations " +
                              "(interview_id, recruiter_id, overall_rating, recommendation, " +
                              "strengths, weaknesses, general_comments, hire_decision, " +
                              "next_steps, is_draft) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement evalStmt = conn.prepareStatement(evalQuery, Statement.RETURN_GENERATED_KEYS);
            evalStmt.setInt(1, evaluation.getInterviewId());
            evalStmt.setInt(2, evaluation.getRecruiterId());
            evalStmt.setDouble(3, evaluation.getOverallRating());
            evalStmt.setString(4, evaluation.getRecommendation());
            evalStmt.setString(5, evaluation.getStrengths());
            evalStmt.setString(6, evaluation.getWeaknesses());
            evalStmt.setString(7, evaluation.getGeneralComments());
            evalStmt.setString(8, evaluation.getHireDecision());
            evalStmt.setString(9, evaluation.getNextSteps());
            evalStmt.setBoolean(10, evaluation.isDraft());

            int rowsAffected = evalStmt.executeUpdate();

            if (rowsAffected == 0) {
                conn.rollback();
                return false;
            }

            // Get generated evaluation ID
            ResultSet generatedKeys = evalStmt.getGeneratedKeys();
            int evaluationId = 0;
            if (generatedKeys.next()) {
                evaluationId = generatedKeys.getInt(1);
            }

            // Insert individual scores
            String scoreQuery = "INSERT INTO evaluation_scores " +
                               "(evaluation_id, criteria_id, score, comments) " +
                               "VALUES (?, ?, ?, ?)";

            PreparedStatement scoreStmt = conn.prepareStatement(scoreQuery);

            for (EvaluationScore score : evaluation.getScores()) {
                scoreStmt.setInt(1, evaluationId);
                scoreStmt.setInt(2, score.getCriteriaId());
                scoreStmt.setInt(3, score.getScore());
                scoreStmt.setString(4, score.getComments());
                scoreStmt.addBatch();
            }

            scoreStmt.executeBatch();

            // Update interview status to COMPLETED
            String updateInterviewQuery = "UPDATE interviews SET status = 'COMPLETED' WHERE interview_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateInterviewQuery);
            updateStmt.setInt(1, evaluation.getInterviewId());
            updateStmt.executeUpdate();

            // Commit transaction
            conn.commit();
            System.out.println("✅ Evaluation submitted successfully!");

            // Send notification to candidate
            try {
                sendEvaluationNotification(evaluation.getInterviewId());
            } catch (Exception e) {
                System.err.println("⚠️ Failed to send evaluation notification: " + e.getMessage());
            }

            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error submitting evaluation:");
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get evaluation for an interview
     */
    public InterviewEvaluation getEvaluationByInterviewId(int interviewId) {
        String query = "SELECT e.evaluation_id, e.interview_id, e.recruiter_id, " +
                      "e.overall_rating, e.recommendation, e.strengths, e.weaknesses, " +
                      "e.general_comments, e.hire_decision, e.next_steps, e.is_draft, " +
                      "e.evaluated_at, e.updated_at " +
                      "FROM interview_evaluations e " +
                      "WHERE e.interview_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, interviewId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                InterviewEvaluation evaluation = new InterviewEvaluation();
                evaluation.setEvaluationId(rs.getInt("evaluation_id"));
                evaluation.setInterviewId(rs.getInt("interview_id"));
                evaluation.setRecruiterId(rs.getInt("recruiter_id"));
                evaluation.setOverallRating(rs.getDouble("overall_rating"));
                evaluation.setRecommendation(rs.getString("recommendation"));
                evaluation.setStrengths(rs.getString("strengths"));
                evaluation.setWeaknesses(rs.getString("weaknesses"));
                evaluation.setGeneralComments(rs.getString("general_comments"));
                evaluation.setHireDecision(rs.getString("hire_decision"));
                evaluation.setNextSteps(rs.getString("next_steps"));
                evaluation.setDraft(rs.getBoolean("is_draft"));
                evaluation.setEvaluatedAt(rs.getTimestamp("evaluated_at"));
                evaluation.setUpdatedAt(rs.getTimestamp("updated_at"));

                // Load scores
                evaluation.setScores(getScoresForEvaluation(evaluation.getEvaluationId()));

                return evaluation;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting evaluation:");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get scores for an evaluation
     */
    private List<EvaluationScore> getScoresForEvaluation(int evaluationId) {
        List<EvaluationScore> scores = new ArrayList<>();

        String query = "SELECT s.score_id, s.evaluation_id, s.criteria_id, s.score, s.comments, " +
                      "c.criteria_name " +
                      "FROM evaluation_scores s " +
                      "JOIN evaluation_criteria c ON s.criteria_id = c.criteria_id " +
                      "WHERE s.evaluation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, evaluationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                EvaluationScore score = new EvaluationScore();
                score.setScoreId(rs.getInt("score_id"));
                score.setEvaluationId(rs.getInt("evaluation_id"));
                score.setCriteriaId(rs.getInt("criteria_id"));
                score.setScore(rs.getInt("score"));
                score.setComments(rs.getString("comments"));
                score.setCriteriaName(rs.getString("criteria_name"));
                scores.add(score);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting evaluation scores:");
            e.printStackTrace();
        }

        return scores;
    }

    /**
     * Send evaluation notification to candidate
     */
    private void sendEvaluationNotification(int interviewId) {
        String query = "SELECT i.interviewee_id, ip.user_id, jo.title AS job_title " +
                      "FROM interviews i " +
                      "JOIN interviewee_profiles ip ON i.interviewee_id = ip.interviewee_id " +
                      "JOIN applications app ON i.application_id = app.application_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "WHERE i.interview_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, interviewId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int candidateUserId = rs.getInt("user_id");
                String jobTitle = rs.getString("job_title");

                Interview interview = new Interview();
                interview.setInterviewId(interviewId);

                NotificationService notificationService = new NotificationService();
                notificationService.notifyEvaluationCompleted(interview, candidateUserId, jobTitle);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if interview has been evaluated
     */
    public boolean hasEvaluation(int interviewId) {
        String query = "SELECT COUNT(*) FROM interview_evaluations WHERE interview_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, interviewId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error checking evaluation:");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get all evaluations for a recruiter with interview details
     */
    public List<InterviewEvaluation> getAllEvaluationsForRecruiter(int recruiterId) {
        List<InterviewEvaluation> evaluations = new ArrayList<>();

        String query = "SELECT e.evaluation_id, e.interview_id, e.recruiter_id, " +
                      "e.overall_rating, e.recommendation, e.strengths, e.weaknesses, " +
                      "e.general_comments, e.hire_decision, e.next_steps, e.is_draft, " +
                      "e.evaluated_at, e.updated_at, " +
                      "CONCAT(ip.first_name, ' ', ip.last_name) AS candidate_name, " +
                      "jo.title AS job_title, " +
                      "i.scheduled_date, i.scheduled_time, it.type_name AS interview_type, " +
                      "i.interview_round " +
                      "FROM interview_evaluations e " +
                      "JOIN interviews i ON e.interview_id = i.interview_id " +
                      "JOIN interviewee_profiles ip ON i.interviewee_id = ip.interviewee_id " +
                      "JOIN applications app ON i.application_id = app.application_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "JOIN interview_types it ON i.interview_type_id = it.interview_type_id " +
                      "WHERE e.recruiter_id = ? " +
                      "ORDER BY e.evaluated_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, recruiterId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                InterviewEvaluation evaluation = new InterviewEvaluation();
                evaluation.setEvaluationId(rs.getInt("evaluation_id"));
                evaluation.setInterviewId(rs.getInt("interview_id"));
                evaluation.setRecruiterId(rs.getInt("recruiter_id"));
                evaluation.setOverallRating(rs.getDouble("overall_rating"));
                evaluation.setRecommendation(rs.getString("recommendation"));
                evaluation.setStrengths(rs.getString("strengths"));
                evaluation.setWeaknesses(rs.getString("weaknesses"));
                evaluation.setGeneralComments(rs.getString("general_comments"));
                evaluation.setHireDecision(rs.getString("hire_decision"));
                evaluation.setNextSteps(rs.getString("next_steps"));
                evaluation.setDraft(rs.getBoolean("is_draft"));
                evaluation.setEvaluatedAt(rs.getTimestamp("evaluated_at"));
                evaluation.setUpdatedAt(rs.getTimestamp("updated_at"));

                // Create and set interview details
                Interview interview = new Interview();
                interview.setInterviewId(rs.getInt("interview_id"));
                interview.setCandidateName(rs.getString("candidate_name"));
                interview.setJobTitle(rs.getString("job_title"));
                interview.setScheduledDate(rs.getDate("scheduled_date"));
                interview.setScheduledTime(rs.getTime("scheduled_time"));
                interview.setInterviewTypeName(rs.getString("interview_type"));
                interview.setInterviewRound(rs.getInt("interview_round"));

                evaluation.setInterview(interview);

                // Load scores
                evaluation.setScores(getScoresForEvaluation(evaluation.getEvaluationId()));

                evaluations.add(evaluation);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting all evaluations:");
            e.printStackTrace();
        }

        return evaluations;
    }

    /**
     * Delete an evaluation permanently
     */
    public boolean deleteEvaluation(int evaluationId, int interviewId) {
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Delete evaluation scores first (foreign key)
            String deleteScoresQuery = "DELETE FROM evaluation_scores WHERE evaluation_id = ?";
            PreparedStatement deleteScoresStmt = conn.prepareStatement(deleteScoresQuery);
            deleteScoresStmt.setInt(1, evaluationId);
            deleteScoresStmt.executeUpdate();

            // Delete evaluation
            String deleteEvalQuery = "DELETE FROM interview_evaluations WHERE evaluation_id = ?";
            PreparedStatement deleteEvalStmt = conn.prepareStatement(deleteEvalQuery);
            deleteEvalStmt.setInt(1, evaluationId);
            int rowsAffected = deleteEvalStmt.executeUpdate();

            if (rowsAffected > 0) {
                // Reset interview status back to CONFIRMED (so it can be evaluated again or completed)
                String updateInterviewQuery = "UPDATE interviews SET status = 'CONFIRMED' WHERE interview_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateInterviewQuery);
                updateStmt.setInt(1, interviewId);
                updateStmt.executeUpdate();

                conn.commit();
                System.out.println("✅ Evaluation deleted successfully!");
                return true;
            } else {
                conn.rollback();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error deleting evaluation:");
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update an existing evaluation
     */
    public boolean updateEvaluation(InterviewEvaluation evaluation) {
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Update evaluation
            String evalQuery = "UPDATE interview_evaluations SET " +
                              "overall_rating = ?, recommendation = ?, " +
                              "strengths = ?, weaknesses = ?, general_comments = ?, " +
                              "hire_decision = ?, next_steps = ?, is_draft = ? " +
                              "WHERE evaluation_id = ?";

            PreparedStatement evalStmt = conn.prepareStatement(evalQuery);
            evalStmt.setDouble(1, evaluation.getOverallRating());
            evalStmt.setString(2, evaluation.getRecommendation());
            evalStmt.setString(3, evaluation.getStrengths());
            evalStmt.setString(4, evaluation.getWeaknesses());
            evalStmt.setString(5, evaluation.getGeneralComments());
            evalStmt.setString(6, evaluation.getHireDecision());
            evalStmt.setString(7, evaluation.getNextSteps());
            evalStmt.setBoolean(8, evaluation.isDraft());
            evalStmt.setInt(9, evaluation.getEvaluationId());

            int rowsAffected = evalStmt.executeUpdate();

            if (rowsAffected == 0) {
                conn.rollback();
                return false;
            }

            // Delete old scores
            String deleteScoresQuery = "DELETE FROM evaluation_scores WHERE evaluation_id = ?";
            PreparedStatement deleteScoresStmt = conn.prepareStatement(deleteScoresQuery);
            deleteScoresStmt.setInt(1, evaluation.getEvaluationId());
            deleteScoresStmt.executeUpdate();

            // Insert new scores
            String scoreQuery = "INSERT INTO evaluation_scores " +
                               "(evaluation_id, criteria_id, score, comments) " +
                               "VALUES (?, ?, ?, ?)";

            PreparedStatement scoreStmt = conn.prepareStatement(scoreQuery);

            for (EvaluationScore score : evaluation.getScores()) {
                scoreStmt.setInt(1, evaluation.getEvaluationId());
                scoreStmt.setInt(2, score.getCriteriaId());
                scoreStmt.setInt(3, score.getScore());
                scoreStmt.setString(4, score.getComments());
                scoreStmt.addBatch();
            }

            scoreStmt.executeBatch();

            conn.commit();
            System.out.println("✅ Evaluation updated successfully!");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error updating evaluation:");
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
