package com.hirely.controllers;

import com.hirely.dao.InterviewDAO;
import com.hirely.models.Interview;
import com.hirely.models.User;
import com.hirely.utils.SessionManager;
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
import java.util.List;

/**
 * Controller for Recruiter Dashboard
 */
public class RecruiterDashboardController {

    // Header
    @FXML private Text userNameLabel;
    @FXML private Text userEmailLabel;
    @FXML private Button logoutButton;

    // Navigation
    @FXML private Button dashboardBtn;
    @FXML private Button interviewsBtn;
    @FXML private Button evaluationsBtn;
    @FXML private Button scheduleBtn;

    // Statistics
    @FXML private Text totalInterviewsLabel;
    @FXML private Text todayInterviewsLabel;
    @FXML private Text pendingEvaluationsLabel;

    // Table
    @FXML private TableView<Interview> interviewsTable;
    @FXML private TableColumn<Interview, String> dateColumn;
    @FXML private TableColumn<Interview, String> candidateColumn;
    @FXML private TableColumn<Interview, String> jobColumn;
    @FXML private TableColumn<Interview, String> typeColumn;
    @FXML private TableColumn<Interview, String> statusColumn;
    @FXML private TableColumn<Interview, String> locationColumn;
    @FXML private TableColumn<Interview, Void> actionsColumn;
    @FXML private Button refreshBtn;

    private InterviewDAO interviewDAO;
    private User currentUser;
    private int recruiterId;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        // Get current user from session
        currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
            showError("No user logged in!");
            return;
        }

        recruiterId = currentUser.getProfileId();

        // Initialize DAO
        interviewDAO = new InterviewDAO();

        // Setup UI
        setupUserInfo();
        setupTable();
        loadDashboardData();

        System.out.println("✅ Recruiter Dashboard loaded for: " + currentUser.getFullName());
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
    private void setupTable() {
        // Date & Time column
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));

        // Candidate column
        candidateColumn.setCellValueFactory(new PropertyValueFactory<>("candidateName"));

        // Job column
        jobColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));

        // Type column
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));

        // Status column with color coding
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new TableCell<Interview, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);

                    // Color code based on status
                    switch (status) {
                        case "CONFIRMED":
                            setTextFill(Color.web("#16a34a"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        case "SCHEDULED":
                            setTextFill(Color.web("#eab308"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        case "CANCELLED":
                            setTextFill(Color.web("#dc2626"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        case "COMPLETED":
                            setTextFill(Color.web("#2563eb"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        default:
                            setTextFill(Color.web("#6b7280"));
                            setStyle("");
                    }
                }
            }
        });

        // Location column
        locationColumn.setCellValueFactory(cellData -> {
            Interview interview = cellData.getValue();
            String location = interview.getMeetingLink() != null ?
                "🔗 Virtual" : (interview.getLocation() != null ? interview.getLocation() : "TBD");
            return new javafx.beans.property.SimpleStringProperty(location);
        });

        // Actions column with buttons
        actionsColumn.setCellFactory(column -> new TableCell<Interview, Void>() {
            private final Button completeBtn = new Button("✔️ Complete");
            private final Button evaluateBtn = new Button("⭐ Evaluate");
            private final HBox container = new HBox(6);

            {
                container.setAlignment(Pos.CENTER);
                completeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 4px;");
                evaluateBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 4px;");

                completeBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleCompleteInterview(interview);
                });

                evaluateBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleEvaluateInterview(interview);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Interview interview = getTableView().getItems().get(getIndex());

                    container.getChildren().clear();

                    // Show Complete button for SCHEDULED and CONFIRMED interviews
                    if (interview.getStatus().equals("SCHEDULED") ||
                        interview.getStatus().equals("CONFIRMED")) {
                        container.getChildren().add(completeBtn);
                    }

                    // Show Evaluate button for CONFIRMED and COMPLETED interviews
                    if (interview.getStatus().equals("COMPLETED") ||
                        interview.getStatus().equals("CONFIRMED")) {
                        container.getChildren().add(evaluateBtn);
                    }

                    setGraphic(container);
                }
            }
        });
    }

    /**
     * Load dashboard data (statistics and interviews)
     */
    private void loadDashboardData() {
        // Load in background thread
        new Thread(() -> {
            // Get statistics
            int totalInterviews = interviewDAO.getInterviewCount(recruiterId);
            int todayInterviews = interviewDAO.getTodayInterviewCount(recruiterId);
            int pendingEvals = interviewDAO.getPendingEvaluationsCount(recruiterId);

            // Get upcoming interviews (next 30 days)
            List<Interview> interviews = interviewDAO.getUpcomingInterviews(recruiterId, 30);

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                // Update statistics
                totalInterviewsLabel.setText(String.valueOf(totalInterviews));
                todayInterviewsLabel.setText(String.valueOf(todayInterviews));
                pendingEvaluationsLabel.setText(String.valueOf(pendingEvals));

                // Update table
                ObservableList<Interview> interviewList = FXCollections.observableArrayList(interviews);
                interviewsTable.setItems(interviewList);

                System.out.println("✅ Dashboard data loaded: " + interviews.size() + " upcoming interviews");
            });
        }).start();
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.logout();

                try {
                    // Load login screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    Parent root = loader.load();

                    Scene scene = new Scene(root, 900, 600);
                    scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

                    Stage stage = (Stage) logoutButton.getScene().getWindow();
                    stage.setScene(scene);
                    stage.centerOnScreen();

                    System.out.println("✅ Logged out successfully");

                } catch (IOException e) {
                    showError("Error loading login screen: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Show dashboard (current view)
     */
    @FXML
    private void showDashboard() {
        // Reload the dashboard FXML to refresh the view
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/recruiter-dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            // Get the main window
            javafx.scene.layout.BorderPane mainPane = (javafx.scene.layout.BorderPane) dashboardBtn.getScene().getRoot();

            // Replace the entire content with fresh dashboard
            Scene scene = dashboardBtn.getScene();
            scene.setRoot(dashboardRoot);
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            System.out.println("✅ Navigated back to Dashboard");

        } catch (IOException e) {
            System.err.println("❌ Error loading dashboard:");
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
        }
    }

    /**
     * Show interviews list
     */
    @FXML
    private void showInterviews() {
        try {
            // Load interview list view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/interview-list.fxml"));
            Parent interviewListRoot = loader.load();

            // Get the main content area (center of BorderPane)
            javafx.scene.layout.BorderPane mainPane = (javafx.scene.layout.BorderPane) dashboardBtn.getScene().getRoot();

            // Create a BorderPane wrapper to maintain header and sidebar
            javafx.scene.layout.BorderPane contentPane = new javafx.scene.layout.BorderPane();
            contentPane.setCenter(interviewListRoot);
            contentPane.setStyle("-fx-background-color: #F5F5F5;");

            // Replace center content
            mainPane.setCenter(contentPane);

            System.out.println("✅ Navigated to Interview List");

        } catch (IOException e) {
            System.err.println("❌ Error loading interview list:");
            e.printStackTrace();
            showError("Error loading interview list: " + e.getMessage());
        }
    }

    /**
     * Show evaluations list (navigates to Evaluations Table View)
     */
    @FXML
    private void showEvaluations() {
        try {
            // Load evaluations table view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/evaluations-table.fxml"));
            Parent evaluationsRoot = loader.load();

            // Get the main content area (center of BorderPane)
            javafx.scene.layout.BorderPane mainPane = (javafx.scene.layout.BorderPane) dashboardBtn.getScene().getRoot();

            // Create a BorderPane wrapper to maintain header and sidebar
            javafx.scene.layout.BorderPane contentPane = new javafx.scene.layout.BorderPane();
            contentPane.setCenter(evaluationsRoot);
            contentPane.setStyle("-fx-background-color: #F5F5F5;");

            // Replace center content
            mainPane.setCenter(contentPane);

            System.out.println("✅ Navigated to Evaluations Table");

        } catch (IOException e) {
            System.err.println("❌ Error loading evaluations table:");
            e.printStackTrace();
            showError("Error loading evaluations: " + e.getMessage());
        }
    }

    /**
     * Handle schedule interview
     */
    @FXML
    private void handleScheduleInterview() {
        try {
            // Load schedule interview dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/schedule-interview.fxml"));
            Parent root = loader.load();

            // Create new stage for dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Schedule Interview");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            // Refresh dashboard after dialog closes
            ScheduleInterviewController controller = loader.getController();
            if (controller.isInterviewScheduled()) {
                System.out.println("✅ Interview scheduled, refreshing dashboard...");
                handleRefresh();
            }

        } catch (IOException e) {
            System.err.println("❌ Error opening schedule interview dialog:");
            e.printStackTrace();
            showError("Error opening schedule dialog: " + e.getMessage());
        }
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refreshing dashboard...");
        loadDashboardData();
    }

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info alert
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handle mark interview as completed
     */
    private void handleCompleteInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mark as Completed");
        alert.setHeaderText("Mark this interview as completed?");
        alert.setContentText(
            "Candidate: " + interview.getCandidateName() + "\n" +
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: " + interview.getScheduledDate() + " at " + interview.getScheduledTime() + "\n\n" +
            "Once marked as completed, you can evaluate this interview."
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = interviewDAO.updateInterviewStatus(interview.getInterviewId(), "COMPLETED");
                if (success) {
                    showInfo("✅ Interview marked as completed!");
                    loadDashboardData();
                } else {
                    showError("Failed to mark interview as completed. Please try again.");
                }
            }
        });
    }

    /**
     * Handle evaluate interview
     */
    private void handleEvaluateInterview(Interview interview) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/evaluation-form.fxml"));
            Parent root = loader.load();

            EvaluationFormController controller = loader.getController();
            controller.setInterview(interview);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Evaluate Interview");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (controller.isEvaluationSubmitted()) {
                loadDashboardData();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error opening evaluation form: " + e.getMessage());
        }
    }
}