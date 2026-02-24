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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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

    // ---- Header ----
    @FXML private Text userNameLabel;
    @FXML private Text userEmailLabel;
    @FXML private Button logoutButton;

    // ---- Sidebar ----
    @FXML private Button dashboardBtn;
    @FXML private Button interviewsBtn;
    @FXML private Button applicationsBtn;

    // ---- Center stack ----
    @FXML private StackPane centerStack;
    @FXML private ScrollPane dashboardView;
    @FXML private ScrollPane interviewsView;

    // ---- Dashboard stats ----
    @FXML private Text totalApplicationsLabel;
    @FXML private Text upcomingInterviewsLabel;
    @FXML private Text pendingResponsesLabel;

    // ---- Dashboard pending table ----
    @FXML private TableView<Interview> pendingInterviewsTable;
    @FXML private TableColumn<Interview, String> pendingDateColumn;
    @FXML private TableColumn<Interview, String> pendingJobColumn;
    @FXML private TableColumn<Interview, String> pendingTypeColumn;
    @FXML private TableColumn<Interview, String> pendingRecruiterColumn;
    @FXML private TableColumn<Interview, Void> pendingActionsColumn;

    // ---- Dashboard confirmed table ----
    @FXML private TableView<Interview> confirmedInterviewsTable;
    @FXML private TableColumn<Interview, String> confirmedDateColumn;
    @FXML private TableColumn<Interview, String> confirmedJobColumn;
    @FXML private TableColumn<Interview, String> confirmedTypeColumn;
    @FXML private TableColumn<Interview, String> confirmedLocationColumn;
    @FXML private TableColumn<Interview, String> confirmedStatusColumn;

    // ---- My Interviews tab stats ----
    @FXML private Text allInterviewsCountLabel;
    @FXML private Text ivPendingCountLabel;
    @FXML private Text ivConfirmedCountLabel;
    @FXML private Text ivCompletedCountLabel;
    @FXML private Text ivCancelledCountLabel;
    @FXML private Text ivTableSubtitle;

    // ---- My Interviews filter ----
    @FXML private ComboBox<String> statusFilterCombo;

    // ---- My Interviews full table ----
    @FXML private TableView<Interview> allInterviewsTable;
    @FXML private TableColumn<Interview, String> ivDateColumn;
    @FXML private TableColumn<Interview, String> ivPositionColumn;
    @FXML private TableColumn<Interview, String> ivTypeColumn;
    @FXML private TableColumn<Interview, Integer> ivRoundColumn;
    @FXML private TableColumn<Interview, String> ivLocationColumn;
    @FXML private TableColumn<Interview, String> ivStatusColumn;
    @FXML private TableColumn<Interview, Void>   ivActionsColumn;

    // ---- State ----
    private InterviewDAO interviewDAO;
    private NotificationService notificationService;
    private User currentUser;
    private int intervieweeId;
    private ObservableList<Interview> allInterviewsList = FXCollections.observableArrayList();

    // ========== INIT ==========

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) { showError("No user logged in!"); return; }

        intervieweeId = currentUser.getProfileId();
        interviewDAO  = new InterviewDAO();
        notificationService = new NotificationService();

        setupUserInfo();
        setupDashboardTables();
        setupInterviewsTab();
        loadDashboardData();

        System.out.println("✅ Candidate Dashboard loaded for: " + currentUser.getFullName());
    }

    // ========== USER INFO ==========

    private void setupUserInfo() {
        userNameLabel.setText(currentUser.getFullName());
        userEmailLabel.setText(currentUser.getEmail());
    }

    // ========== NAVIGATION ==========

    @FXML
    private void showDashboard() {
        dashboardView.setVisible(true);  dashboardView.setManaged(true);
        interviewsView.setVisible(false); interviewsView.setManaged(false);

        dashboardBtn.setStyle("-fx-background-color: #FFE5D9; -fx-text-fill: #FF8C42; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-alignment: CENTER_LEFT;");
        interviewsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2D2D2D; -fx-font-size: 14px; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-alignment: CENTER_LEFT;");
        applicationsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2D2D2D; -fx-font-size: 14px; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-alignment: CENTER_LEFT;");
    }

    @FXML
    private void showMyInterviews() {
        dashboardView.setVisible(false); dashboardView.setManaged(false);
        interviewsView.setVisible(true);  interviewsView.setManaged(true);

        interviewsBtn.setStyle("-fx-background-color: #FFE5D9; -fx-text-fill: #FF8C42; -fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-alignment: CENTER_LEFT;");
        dashboardBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2D2D2D; -fx-font-size: 14px; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-alignment: CENTER_LEFT;");
        applicationsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2D2D2D; -fx-font-size: 14px; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-alignment: CENTER_LEFT;");

        loadInterviewsTab();
    }

    @FXML
    private void showApplications() {
        showInfo("Applications view - handled by other team!");
    }

    // ========== DASHBOARD TABLES ==========

    private void setupDashboardTables() {
        pendingDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));
        pendingJobColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        pendingTypeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));
        pendingRecruiterColumn.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty("Recruiter"));

        pendingActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn  = new Button("✅ Accept");
            private final Button declineBtn = new Button("❌ Decline");
            private final HBox box = new HBox(8, acceptBtn, declineBtn);
            {
                box.setAlignment(Pos.CENTER);
                acceptBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5px 10px; -fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 4px;");
                declineBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5px 10px; -fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 4px;");
                acceptBtn.setOnAction(e  -> handleAcceptInterview(getTableView().getItems().get(getIndex())));
                declineBtn.setOnAction(e -> handleDeclineInterview(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        confirmedDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));
        confirmedJobColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        confirmedTypeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));
        confirmedLocationColumn.setCellValueFactory(cd -> {
            Interview iv = cd.getValue();
            String val = (iv.getMeetingLink() != null && !iv.getMeetingLink().isEmpty())
                ? "🔗 " + iv.getMeetingLink() : (iv.getLocation() != null ? iv.getLocation() : "—");
            return new javafx.beans.property.SimpleStringProperty(val);
        });
        confirmedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        confirmedStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setTextFill(Color.web("#16a34a"));
                setStyle("-fx-font-weight: 600;");
            }
        });
    }

    private void loadDashboardData() {
        new Thread(() -> {
            int totalApps            = getTotalApplicationsCount();
            List<Interview> pending  = getInterviewsByStatus("SCHEDULED");
            List<Interview> confirmed = getInterviewsByStatuses(List.of("CONFIRMED","RESCHEDULED"));

            Platform.runLater(() -> {
                totalApplicationsLabel.setText(String.valueOf(totalApps));
                upcomingInterviewsLabel.setText(String.valueOf(confirmed.size()));
                pendingResponsesLabel.setText(String.valueOf(pending.size()));
                pendingInterviewsTable.setItems(FXCollections.observableArrayList(pending));
                confirmedInterviewsTable.setItems(FXCollections.observableArrayList(confirmed));
            });
        }).start();
    }

    // ========== MY INTERVIEWS TAB ==========

    private void setupInterviewsTab() {
        // Populate filter combo
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "All Statuses", "SCHEDULED", "CONFIRMED", "RESCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW"
        ));
        statusFilterCombo.getSelectionModel().selectFirst();

        // Wire columns
        ivDateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));
        ivPositionColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        ivTypeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));
        ivRoundColumn.setCellValueFactory(new PropertyValueFactory<>("interviewRound"));
        ivLocationColumn.setCellValueFactory(cd -> {
            Interview iv = cd.getValue();
            String val = (iv.getMeetingLink() != null && !iv.getMeetingLink().isEmpty())
                ? "🔗 " + iv.getMeetingLink() : (iv.getLocation() != null ? iv.getLocation() : "—");
            return new javafx.beans.property.SimpleStringProperty(val);
        });

        // Status column with colour badge
        ivStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        ivStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                switch (s) {
                    case "SCHEDULED"   -> { setTextFill(Color.web("#f59e0b")); setStyle("-fx-font-weight:600;"); }
                    case "CONFIRMED"   -> { setTextFill(Color.web("#16a34a")); setStyle("-fx-font-weight:600;"); }
                    case "COMPLETED"   -> { setTextFill(Color.web("#6366f1")); setStyle("-fx-font-weight:600;"); }
                    case "CANCELLED"   -> { setTextFill(Color.web("#dc2626")); setStyle("-fx-font-weight:600;"); }
                    case "RESCHEDULED" -> { setTextFill(Color.web("#0ea5e9")); setStyle("-fx-font-weight:600;"); }
                    case "NO_SHOW"     -> { setTextFill(Color.web("#9ca3af")); setStyle("-fx-font-weight:600;"); }
                    default            -> { setTextFill(Color.BLACK); setStyle(""); }
                }
            }
        });

        // Actions column – Accept/Decline only for SCHEDULED
        ivActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn  = new Button("✅");
            private final Button declineBtn = new Button("❌");
            private final HBox box = new HBox(6, acceptBtn, declineBtn);
            {
                box.setAlignment(Pos.CENTER);
                String btnStyle = "-fx-font-size: 13px; -fx-padding: 4px 8px; -fx-background-radius: 4px;";
                acceptBtn.setStyle(btnStyle + "-fx-background-color: #16a34a; -fx-text-fill: white;");
                declineBtn.setStyle(btnStyle + "-fx-background-color: #dc2626; -fx-text-fill: white;");
                acceptBtn.setTooltip(new Tooltip("Accept this interview"));
                declineBtn.setTooltip(new Tooltip("Decline this interview"));
                acceptBtn.setOnAction(e  -> handleAcceptInterview(getTableView().getItems().get(getIndex())));
                declineBtn.setOnAction(e -> handleDeclineInterview(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Interview iv = getTableView().getItems().get(getIndex());
                boolean isPending = "SCHEDULED".equals(iv.getStatus());
                acceptBtn.setDisable(!isPending);
                declineBtn.setDisable(!isPending);
                acceptBtn.setOpacity(isPending ? 1.0 : 0.35);
                declineBtn.setOpacity(isPending ? 1.0 : 0.35);
                setGraphic(box);
            }
        });
    }

    private void loadInterviewsTab() {
        new Thread(() -> {
            List<Interview> all = getAllInterviews();

            long pending   = all.stream().filter(i -> "SCHEDULED".equals(i.getStatus())).count();
            long confirmed = all.stream().filter(i -> "CONFIRMED".equals(i.getStatus()) || "RESCHEDULED".equals(i.getStatus())).count();
            long completed = all.stream().filter(i -> "COMPLETED".equals(i.getStatus())).count();
            long cancelled = all.stream().filter(i -> "CANCELLED".equals(i.getStatus()) || "NO_SHOW".equals(i.getStatus())).count();

            Platform.runLater(() -> {
                allInterviewsList.setAll(all);
                allInterviewsTable.setItems(allInterviewsList);

                allInterviewsCountLabel.setText(String.valueOf(all.size()));
                ivPendingCountLabel.setText(String.valueOf(pending));
                ivConfirmedCountLabel.setText(String.valueOf(confirmed));
                ivCompletedCountLabel.setText(String.valueOf(completed));
                ivCancelledCountLabel.setText(String.valueOf(cancelled));
                ivTableSubtitle.setText(all.size() + " interview(s) found");

                // Reset filter
                statusFilterCombo.getSelectionModel().selectFirst();
            });
        }).start();
    }

    @FXML
    private void handleStatusFilter() {
        String selected = statusFilterCombo.getValue();
        if (selected == null || selected.equals("All Statuses")) {
            allInterviewsTable.setItems(allInterviewsList);
            ivTableSubtitle.setText(allInterviewsList.size() + " interview(s) found");
        } else {
            ObservableList<Interview> filtered = allInterviewsList.filtered(
                i -> selected.equals(i.getStatus())
            );
            allInterviewsTable.setItems(filtered);
            ivTableSubtitle.setText(filtered.size() + " interview(s) with status: " + selected);
        }
    }

    @FXML
    private void refreshInterviewsTab() {
        loadInterviewsTab();
    }

    // ========== DATA ACCESS ==========

    private int getTotalApplicationsCount() {
        String sql = "SELECT COUNT(*) FROM applications WHERE interviewee_id = ?";
        try (Connection c = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, intervieweeId);
            ResultSet r = s.executeQuery();
            if (r.next()) return r.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Single-status query used by dashboard */
    private List<Interview> getInterviewsByStatus(String status) {
        return fetchInterviews("WHERE i.interviewee_id = ? AND i.status = ? AND i.scheduled_date >= CURDATE()",
            intervieweeId, status);
    }

    /** Multi-status query used by dashboard confirmed section */
    private List<Interview> getInterviewsByStatuses(List<String> statuses) {
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toArray(String[]::new));
        String sql = BASE_SQL + " WHERE i.interviewee_id = ? AND i.status IN (" + placeholders + ") " +
                     "AND i.scheduled_date >= CURDATE() ORDER BY i.scheduled_date, i.scheduled_time";
        List<Interview> list = new ArrayList<>();
        try (Connection c = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, intervieweeId);
            for (int i = 0; i < statuses.size(); i++) s.setString(i + 2, statuses.get(i));
            ResultSet r = s.executeQuery();
            while (r.next()) list.add(mapRow(r));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** All interviews (past + future) for the My Interviews tab */
    private List<Interview> getAllInterviews() {
        return fetchInterviews("WHERE i.interviewee_id = ?", intervieweeId);
    }

    // ---- SQL helpers ----

    private static final String BASE_SQL =
        "SELECT i.interview_id, i.scheduled_date, i.scheduled_time, i.duration_minutes, " +
        "i.status, i.meeting_link, i.location, i.interview_round, " +
        "it.type_name AS interview_type, jo.title AS job_title " +
        "FROM interviews i " +
        "JOIN interview_types it ON i.interview_type_id = it.interview_type_id " +
        "JOIN applications app ON i.application_id = app.application_id " +
        "JOIN job_offers jo ON app.job_offer_id = jo.job_offer_id ";

    /** Generic fetch: appends whereClause + ORDER BY then binds params */
    private List<Interview> fetchInterviews(String whereClause, Object... params) {
        String sql = BASE_SQL + whereClause + " ORDER BY i.scheduled_date DESC, i.scheduled_time DESC";
        List<Interview> list = new ArrayList<>();
        try (Connection c = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer) s.setInt(i + 1, (Integer) params[i]);
                else s.setString(i + 1, (String) params[i]);
            }
            ResultSet r = s.executeQuery();
            while (r.next()) list.add(mapRow(r));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Interview mapRow(ResultSet r) throws SQLException {
        Interview iv = new Interview();
        iv.setInterviewId(r.getInt("interview_id"));
        iv.setScheduledDate(r.getDate("scheduled_date"));
        iv.setScheduledTime(r.getTime("scheduled_time"));
        iv.setDurationMinutes(r.getInt("duration_minutes"));
        iv.setStatus(r.getString("status"));
        iv.setMeetingLink(r.getString("meeting_link"));
        iv.setLocation(r.getString("location"));
        iv.setInterviewTypeName(r.getString("interview_type"));
        iv.setJobTitle(r.getString("job_title"));
        try { iv.setInterviewRound(r.getInt("interview_round")); } catch (Exception ignored) {}
        return iv;
    }

    // ========== ACCEPT / DECLINE ==========

    private void handleAcceptInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Accept Interview");
        alert.setHeaderText("Confirm Interview Acceptance");
        alert.setContentText(
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: "     + interview.getScheduledDate() + "\n" +
            "Time: "     + interview.getScheduledTime());
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                if (updateInterviewStatus(interview.getInterviewId(), "CONFIRMED")) {
                    trySendNotification(interview, true);
                    showSuccess("Interview accepted! Notification sent to recruiter.");
                    loadDashboardData();
                    if (interviewsView.isVisible()) loadInterviewsTab();
                } else {
                    showError("Failed to accept interview. Please try again.");
                }
            }
        });
    }

    private void handleDeclineInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Decline Interview");
        alert.setHeaderText("Are you sure you want to decline this interview?");
        alert.setContentText(
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: "     + interview.getScheduledDate() + "\n" +
            "Time: "     + interview.getScheduledTime());
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                if (updateInterviewStatus(interview.getInterviewId(), "CANCELLED")) {
                    trySendNotification(interview, false);
                    showInfo("Interview declined. Notification sent to recruiter.");
                    loadDashboardData();
                    if (interviewsView.isVisible()) loadInterviewsTab();
                } else {
                    showError("Failed to decline interview. Please try again.");
                }
            }
        });
    }

    private void trySendNotification(Interview interview, boolean accepted) {
        try {
            int recruiterUserId = getRecruiterUserId(interview.getInterviewId());
            if (accepted) {
                notificationService.notifyInterviewConfirmed(
                    interview, recruiterUserId, currentUser.getFullName(), interview.getJobTitle());
            } else {
                notificationService.notifyInterviewDeclined(
                    interview, recruiterUserId, currentUser.getFullName(), interview.getJobTitle());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send notification: " + e.getMessage());
        }
    }

    private int getRecruiterUserId(int interviewId) {
        String sql = "SELECT rp.user_id FROM interviews i " +
                     "JOIN recruiter_profiles rp ON i.recruiter_id = rp.recruiter_id " +
                     "WHERE i.interview_id = ?";
        try (Connection c = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, interviewId);
            ResultSet r = s.executeQuery();
            if (r.next()) return r.getInt("user_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private boolean updateInterviewStatus(int interviewId, String status) {
        String sql = "UPDATE interviews SET status = ? WHERE interview_id = ?";
        try (Connection c = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, status);
            s.setInt(2, interviewId);
            return s.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ========== LOGOUT ==========

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
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ========== HELPERS ==========

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setContentText(msg); a.showAndWait();
    }
    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success"); a.setContentText(msg); a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information"); a.setContentText(msg); a.showAndWait();
    }
}
