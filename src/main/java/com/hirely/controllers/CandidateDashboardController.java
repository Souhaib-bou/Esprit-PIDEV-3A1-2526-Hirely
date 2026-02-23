package com.hirely.controllers;

import com.hirely.dao.InterviewDAO;
import com.hirely.models.Interview;
import com.hirely.models.User;
import com.hirely.utils.SessionManager;
import com.hirely.services.NotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Candidate Dashboard
 */
public class CandidateDashboardController {

    @FXML private Text userNameLabel;
    @FXML private Text userEmailLabel;
    @FXML private Button logoutButton;
    @FXML private Button dashboardBtn;
    @FXML private Button interviewsBtn;
    @FXML private Button applicationsBtn;
    @FXML private Text totalApplicationsLabel;
    @FXML private Text upcomingInterviewsLabel;
    @FXML private Text pendingResponsesLabel;

    // Pending interviews table
    @FXML private TableView<Interview> pendingInterviewsTable;
    @FXML private TableColumn<Interview, String> pendingDateColumn;
    @FXML private TableColumn<Interview, String> pendingJobColumn;
    @FXML private TableColumn<Interview, String> pendingTypeColumn;
    @FXML private TableColumn<Interview, String> pendingRecruiterColumn;
    @FXML private TableColumn<Interview, Void> pendingActionsColumn;

    // Confirmed interviews table
    @FXML private TableView<Interview> confirmedInterviewsTable;
    @FXML private TableColumn<Interview, String> confirmedDateColumn;
    @FXML private TableColumn<Interview, String> confirmedJobColumn;
    @FXML private TableColumn<Interview, String> confirmedTypeColumn;
    @FXML private TableColumn<Interview, String> confirmedLocationColumn;
    @FXML private TableColumn<Interview, String> confirmedStatusColumn;

    private InterviewDAO interviewDAO;
    private NotificationService notificationService;
    private User currentUser;
    private int intervieweeId;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in!");
            return;
        }

        intervieweeId = currentUser.getProfileId();
        interviewDAO = new InterviewDAO();
        notificationService = new NotificationService();

        setupUserInfo();
        setupTables();
        loadDashboardData();

        System.out.println("✅ Candidate Dashboard loaded for: " + currentUser.getFullName());
    }

    /**
     * Setup user information in header
     */
    private void setupUserInfo() {
        userNameLabel.setText(currentUser.getFullName());
        userEmailLabel.setText(currentUser.getEmail());
    }

    /**
     * Setup table columns
     */
    private void setupTables() {
        // Pending interviews table
        pendingDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));
        pendingJobColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        pendingTypeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));
        pendingRecruiterColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty("Recruiter"));

        // Actions column for pending interviews
        pendingActionsColumn.setCellFactory(column -> new TableCell<Interview, Void>() {
            private final Button acceptBtn = new Button("✅ Accept");
            private final Button declineBtn = new Button("❌ Decline");
            private final HBox container = new HBox(8, acceptBtn, declineBtn);

            {
                container.setAlignment(Pos.CENTER);
                acceptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 4px;");
                declineBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6px 12px; -fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 4px;");

                acceptBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleAcceptInterview(interview);
                });

                declineBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleDeclineInterview(interview);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        // Confirmed interviews table
        confirmedDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));
        confirmedJobColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        confirmedTypeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));
        confirmedLocationColumn.setCellValueFactory(cellData -> {
            Interview interview = cellData.getValue();
            String location = interview.getMeetingLink() != null ?
                "🔗 " + interview.getMeetingLink() : interview.getLocation();
            return new javafx.beans.property.SimpleStringProperty(location);
        });

        confirmedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        confirmedStatusColumn.setCellFactory(column -> new TableCell<Interview, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equals("CONFIRMED")) {
                        setTextFill(Color.web("#16a34a"));
                        setStyle("-fx-font-weight: 600;");
                    }
                }
            }
        });
    }

    /**
     * Load dashboard data
     */
    private void loadDashboardData() {
        new Thread(() -> {
            // Get statistics
            int totalApplications = getTotalApplicationsCount();
            List<Interview> pendingInterviews = getPendingInterviews();
            List<Interview> confirmedInterviews = getConfirmedInterviews();

            Platform.runLater(() -> {
                totalApplicationsLabel.setText(String.valueOf(totalApplications));
                upcomingInterviewsLabel.setText(String.valueOf(confirmedInterviews.size()));
                pendingResponsesLabel.setText(String.valueOf(pendingInterviews.size()));

                pendingInterviewsTable.setItems(FXCollections.observableArrayList(pendingInterviews));
                confirmedInterviewsTable.setItems(FXCollections.observableArrayList(confirmedInterviews));

                System.out.println("✅ Dashboard loaded: " + pendingInterviews.size() + " pending, " + confirmedInterviews.size() + " confirmed");
            });
        }).start();
    }

    /**
     * Get total applications count for candidate
     */
    private int getTotalApplicationsCount() {
        String query = "SELECT COUNT(*) FROM applications WHERE interviewee_id = ?";

        try (Connection conn = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, intervieweeId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get pending interviews (SCHEDULED, awaiting response)
     */
    private List<Interview> getPendingInterviews() {
        List<Interview> interviews = new ArrayList<>();

        String query = "SELECT i.interview_id, i.scheduled_date, i.scheduled_time, " +
                      "i.duration_minutes, i.status, i.meeting_link, i.location, " +
                      "it.type_name AS interview_type, jo.title AS job_title " +
                      "FROM interviews i " +
                      "JOIN interview_types it ON i.interview_type_id = it.interview_type_id " +
                      "JOIN applications app ON i.application_id = app.application_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "WHERE i.interviewee_id = ? AND i.status = 'SCHEDULED' " +
                      "AND i.scheduled_date >= CURDATE() " +
                      "ORDER BY i.scheduled_date, i.scheduled_time";

        try (Connection conn = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, intervieweeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Interview interview = new Interview();
                interview.setInterviewId(rs.getInt("interview_id"));
                interview.setScheduledDate(rs.getDate("scheduled_date"));
                interview.setScheduledTime(rs.getTime("scheduled_time"));
                interview.setDurationMinutes(rs.getInt("duration_minutes"));
                interview.setStatus(rs.getString("status"));
                interview.setMeetingLink(rs.getString("meeting_link"));
                interview.setLocation(rs.getString("location"));
                interview.setInterviewTypeName(rs.getString("interview_type"));
                interview.setJobTitle(rs.getString("job_title"));
                interviews.add(interview);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return interviews;
    }

    /**
     * Get confirmed interviews
     */
    private List<Interview> getConfirmedInterviews() {
        List<Interview> interviews = new ArrayList<>();

        String query = "SELECT i.interview_id, i.scheduled_date, i.scheduled_time, " +
                      "i.duration_minutes, i.status, i.meeting_link, i.location, " +
                      "it.type_name AS interview_type, jo.title AS job_title " +
                      "FROM interviews i " +
                      "JOIN interview_types it ON i.interview_type_id = it.interview_type_id " +
                      "JOIN applications app ON i.application_id = app.application_id " +
                      "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id " +
                      "WHERE i.interviewee_id = ? AND i.status IN ('CONFIRMED', 'RESCHEDULED') " +
                      "AND i.scheduled_date >= CURDATE() " +
                      "ORDER BY i.scheduled_date, i.scheduled_time";

        try (Connection conn = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, intervieweeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Interview interview = new Interview();
                interview.setInterviewId(rs.getInt("interview_id"));
                interview.setScheduledDate(rs.getDate("scheduled_date"));
                interview.setScheduledTime(rs.getTime("scheduled_time"));
                interview.setDurationMinutes(rs.getInt("duration_minutes"));
                interview.setStatus(rs.getString("status"));
                interview.setMeetingLink(rs.getString("meeting_link"));
                interview.setLocation(rs.getString("location"));
                interview.setInterviewTypeName(rs.getString("interview_type"));
                interview.setJobTitle(rs.getString("job_title"));
                interviews.add(interview);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return interviews;
    }

    /**
     * Handle accept interview
     */
    private void handleAcceptInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Accept Interview");
        alert.setHeaderText("Confirm Interview Acceptance");
        alert.setContentText(
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: " + interview.getScheduledDate() + "\n" +
            "Time: " + interview.getScheduledTime()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Update interview status
                boolean success = updateInterviewStatus(interview.getInterviewId(), "CONFIRMED");

                if (success) {
                    // Send notification to recruiter
                    try {
                        int recruiterUserId = getRecruiterUserId(interview.getInterviewId());
                        notificationService.notifyInterviewConfirmed(
                            interview,
                            recruiterUserId,
                            currentUser.getFullName(),
                            interview.getJobTitle()
                        );
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to send notification: " + e.getMessage());
                    }

                    showSuccess("Interview accepted! Notification sent to recruiter.");
                    loadDashboardData();
                } else {
                    showError("Failed to accept interview. Please try again.");
                }
            }
        });
    }

    /**
     * Handle decline interview
     */
    private void handleDeclineInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Decline Interview");
        alert.setHeaderText("Are you sure you want to decline this interview?");
        alert.setContentText(
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: " + interview.getScheduledDate() + "\n" +
            "Time: " + interview.getScheduledTime()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = updateInterviewStatus(interview.getInterviewId(), "CANCELLED");

                if (success) {
                    // Send notification to recruiter
                    try {
                        int recruiterUserId = getRecruiterUserId(interview.getInterviewId());
                        notificationService.notifyInterviewDeclined(
                            interview,
                            recruiterUserId,
                            currentUser.getFullName(),
                            interview.getJobTitle()
                        );
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to send notification: " + e.getMessage());
                    }

                    showInfo("Interview declined. Notification sent to recruiter.");
                    loadDashboardData();
                } else {
                    showError("Failed to decline interview. Please try again.");
                }
            }
        });
    }

    /**
     * Get recruiter user ID from interview
     */
    private int getRecruiterUserId(int interviewId) {
        String query = "SELECT rp.user_id FROM interviews i " +
                      "JOIN recruiter_profiles rp ON i.recruiter_id = rp.recruiter_id " +
                      "WHERE i.interview_id = ?";

        try (Connection conn = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, interviewId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Update interview status
     */
    private boolean updateInterviewStatus(int interviewId, String status) {
        String query = "UPDATE interviews SET status = ? WHERE interview_id = ?";

        try (Connection conn = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, status);
            stmt.setInt(2, interviewId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout() {
        SessionManager.logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showDashboard() {
        // Already on dashboard
    }

    @FXML
    private void showMyInterviews() {
        showInfo("My Interviews view coming soon!");
    }

    @FXML
    private void showApplications() {
        showInfo("Applications view - handled by other team!");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
