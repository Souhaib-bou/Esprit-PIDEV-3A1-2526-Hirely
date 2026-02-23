package com.hirely.controllers;

import com.hirely.dao.InterviewDAO;
import com.hirely.models.Interview;
import com.hirely.models.InterviewType;
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
import javafx.scene.Scene;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Interview List View
 */
public class InterviewListController {

    @FXML private Button scheduleNewBtn;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> dateRangeComboBox;
    @FXML private Button applyFiltersBtn;
    @FXML private Button clearFiltersBtn;
    @FXML private Text resultsCountLabel;
    @FXML private Button refreshBtn;
    @FXML private TableView<Interview> interviewsTable;
    @FXML private TableColumn<Interview, Integer> idColumn;
    @FXML private TableColumn<Interview, String> dateColumn;
    @FXML private TableColumn<Interview, String> timeColumn;
    @FXML private TableColumn<Interview, String> candidateColumn;
    @FXML private TableColumn<Interview, String> jobColumn;
    @FXML private TableColumn<Interview, String> typeColumn;
    @FXML private TableColumn<Interview, String> statusColumn;
    @FXML private TableColumn<Interview, Integer> roundColumn;
    @FXML private TableColumn<Interview, Void> actionsColumn;
    @FXML private Text totalLabel;
    @FXML private Text scheduledLabel;
    @FXML private Text confirmedLabel;
    @FXML private Text completedLabel;
    @FXML private Text cancelledLabel;

    private InterviewDAO interviewDAO;
    private User currentUser;
    private int recruiterId;
    private ObservableList<Interview> allInterviews;
    private ObservableList<Interview> filteredInterviews;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        recruiterId = currentUser.getProfileId();
        interviewDAO = new InterviewDAO();

        allInterviews = FXCollections.observableArrayList();
        filteredInterviews = FXCollections.observableArrayList();

        setupFilters();
        setupTable();
        loadInterviews();

        System.out.println("✅ Interview List loaded");
    }

    /**
     * Setup filter dropdowns
     */
    private void setupFilters() {
        // Status filter
        statusFilterComboBox.getItems().addAll(
            "All Statuses", "SCHEDULED", "CONFIRMED", "COMPLETED", "CANCELLED", "RESCHEDULED"
        );
        statusFilterComboBox.setValue("All Statuses");

        // Type filter
        typeFilterComboBox.getItems().add("All Types");
        List<InterviewType> types = interviewDAO.getInterviewTypes();
        types.forEach(type -> typeFilterComboBox.getItems().add(type.getTypeName()));
        typeFilterComboBox.setValue("All Types");

        // Date range filter
        dateRangeComboBox.getItems().addAll(
            "All Time", "Today", "This Week", "This Month", "Next 7 Days", "Next 30 Days", "Past"
        );
        dateRangeComboBox.setValue("All Time");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("interviewId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("scheduledDate"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("scheduledTime"));
        candidateColumn.setCellValueFactory(new PropertyValueFactory<>("candidateName"));
        jobColumn.setCellValueFactory(new PropertyValueFactory<>("jobTitle"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("interviewTypeName"));
        roundColumn.setCellValueFactory(new PropertyValueFactory<>("interviewRound"));

        // Status column with colors
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
                    }
                }
            }
        });

        // Actions column with buttons
        actionsColumn.setCellFactory(column -> new TableCell<Interview, Void>() {
            private final Button viewBtn = new Button("👁️ View");
            private final Button editBtn = new Button("✏️ Edit");
            private final Button completeBtn = new Button("✔️ Complete");
            private final Button evaluateBtn = new Button("⭐ Evaluate");
            private final Button cancelBtn = new Button("❌ Cancel");
            private final Button deleteBtn = new Button("🗑️ Delete");
            private final HBox container = new HBox(6);

            {
                container.setAlignment(Pos.CENTER);
                viewBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 4px;");
                editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 4px;");
                completeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 4px;");
                evaluateBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 4px;");
                cancelBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 4px;");
                deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #991b1b; -fx-text-fill: white; -fx-background-radius: 4px;");

                viewBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleViewInterview(interview);
                });

                editBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleEditInterview(interview);
                });

                completeBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleCompleteInterview(interview);
                });

                evaluateBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleEvaluateInterview(interview);
                });

                cancelBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleCancelInterview(interview);
                });

                deleteBtn.setOnAction(e -> {
                    Interview interview = getTableView().getItems().get(getIndex());
                    handleDeleteInterview(interview);
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
                    container.getChildren().add(viewBtn);

                    // Show Edit button only for SCHEDULED and CONFIRMED interviews
                    if (interview.getStatus().equals("SCHEDULED") ||
                        interview.getStatus().equals("CONFIRMED")) {
                        container.getChildren().add(editBtn);
                    }

                    // Show Complete button for SCHEDULED and CONFIRMED interviews
                    if (interview.getStatus().equals("SCHEDULED") ||
                        interview.getStatus().equals("CONFIRMED")) {
                        container.getChildren().add(completeBtn);
                    }

                    // Show Evaluate button only for CONFIRMED/COMPLETED interviews
                    if (interview.getStatus().equals("COMPLETED") ||
                        interview.getStatus().equals("CONFIRMED")) {
                        container.getChildren().add(evaluateBtn);
                    }

                    // Show Cancel button only if not already cancelled/completed
                    if (!interview.getStatus().equals("CANCELLED") &&
                        !interview.getStatus().equals("COMPLETED")) {
                        container.getChildren().add(cancelBtn);
                    }

                    // Show Delete button for CANCELLED interviews
                    if (interview.getStatus().equals("CANCELLED")) {
                        container.getChildren().add(deleteBtn);
                    }

                    setGraphic(container);
                }
            }
        });
    }

    /**
     * Load all interviews
     */
    private void loadInterviews() {
        new Thread(() -> {
            List<Interview> interviews = interviewDAO.getAllInterviews(recruiterId);

            Platform.runLater(() -> {
                allInterviews.setAll(interviews);
                filteredInterviews.setAll(interviews);
                interviewsTable.setItems(filteredInterviews);
                updateStatistics();
                updateResultsCount();
                System.out.println("✅ Loaded " + interviews.size() + " interviews");
            });
        }).start();
    }

    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        int total = filteredInterviews.size();
        int scheduled = (int) filteredInterviews.stream().filter(i -> i.getStatus().equals("SCHEDULED")).count();
        int confirmed = (int) filteredInterviews.stream().filter(i -> i.getStatus().equals("CONFIRMED")).count();
        int completed = (int) filteredInterviews.stream().filter(i -> i.getStatus().equals("COMPLETED")).count();
        int cancelled = (int) filteredInterviews.stream().filter(i -> i.getStatus().equals("CANCELLED")).count();

        totalLabel.setText("Total: " + total);
        scheduledLabel.setText("Scheduled: " + scheduled);
        confirmedLabel.setText("Confirmed: " + confirmed);
        completedLabel.setText("Completed: " + completed);
        cancelledLabel.setText("Cancelled: " + cancelled);
    }

    /**
     * Update results count label
     */
    private void updateResultsCount() {
        resultsCountLabel.setText(filteredInterviews.size() + " Interviews");
    }

    /**
     * Handle apply filters
     */
    @FXML
    private void handleApplyFilters() {
        String statusFilter = statusFilterComboBox.getValue();
        String typeFilter = typeFilterComboBox.getValue();

        filteredInterviews.setAll(
            allInterviews.stream()
                .filter(i -> statusFilter.equals("All Statuses") || i.getStatus().equals(statusFilter))
                .filter(i -> typeFilter.equals("All Types") || i.getInterviewTypeName().equals(typeFilter))
                .collect(Collectors.toList())
        );

        updateStatistics();
        updateResultsCount();
        System.out.println("🔍 Filters applied: " + filteredInterviews.size() + " results");
    }

    /**
     * Handle clear filters
     */
    @FXML
    private void handleClearFilters() {
        statusFilterComboBox.setValue("All Statuses");
        typeFilterComboBox.setValue("All Types");
        dateRangeComboBox.setValue("All Time");
        filteredInterviews.setAll(allInterviews);
        updateStatistics();
        updateResultsCount();
        System.out.println("🔄 Filters cleared");
    }

    /**
     * Filter to show only evaluatable interviews (CONFIRMED and COMPLETED)
     * Called when accessing from Evaluations menu
     */
    public void showOnlyEvaluatable() {
        // Wait for data to load, then apply filter
        Platform.runLater(() -> {
            filteredInterviews.setAll(
                allInterviews.stream()
                    .filter(i -> i.getStatus().equals("CONFIRMED") || i.getStatus().equals("COMPLETED"))
                    .collect(Collectors.toList())
            );

            // Update UI to show what's filtered
            statusFilterComboBox.setValue("CONFIRMED");
            updateStatistics();
            updateResultsCount();
            System.out.println("🔍 Showing only evaluatable interviews: " + filteredInterviews.size() + " results");
        });
    }

    /**
     * Handle refresh
     */
    @FXML
    private void handleRefresh() {
        loadInterviews();
    }

    /**
     * Handle schedule new interview
     */
    @FXML
    private void handleScheduleNew() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/schedule-interview.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Schedule Interview");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            ScheduleInterviewController controller = loader.getController();
            if (controller.isInterviewScheduled()) {
                loadInterviews();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle view interview
     */
    private void handleViewInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Interview Details");
        alert.setHeaderText("Interview #" + interview.getInterviewId());
        alert.setContentText(
            "Candidate: " + interview.getCandidateName() + "\n" +
            "Position: " + interview.getJobTitle() + "\n" +
            "Type: " + interview.getInterviewTypeName() + "\n" +
            "Date: " + interview.getScheduledDate() + "\n" +
            "Time: " + interview.getScheduledTime() + "\n" +
            "Duration: " + interview.getDurationMinutes() + " min\n" +
            "Status: " + interview.getStatus() + "\n" +
            "Round: " + interview.getInterviewRound() + "\n" +
            "Location: " + (interview.getMeetingLink() != null ? "Virtual" : interview.getLocation())
        );
        alert.showAndWait();
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
                loadInterviews();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error opening evaluation form: " + e.getMessage());
        }
    }

    /**
     * Handle edit interview
     */
    private void handleEditInterview(Interview interview) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/schedule-interview.fxml"));
            Parent root = loader.load();

            ScheduleInterviewController controller = loader.getController();
            controller.setEditMode(interview); // Load existing interview data

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Interview");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (controller.isInterviewScheduled()) {
                loadInterviews();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Error opening edit form: " + e.getMessage());
        }
    }

    /**
     * Handle complete interview
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
                    loadInterviews();
                } else {
                    showError("Failed to mark interview as completed. Please try again.");
                }
            }
        });
    }

    /**
     * Handle cancel interview
     */
    private void handleCancelInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Interview");
        alert.setHeaderText("Are you sure you want to cancel this interview?");
        alert.setContentText(
            "Candidate: " + interview.getCandidateName() + "\n" +
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: " + interview.getScheduledDate() + " at " + interview.getScheduledTime()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Call DAO to cancel interview
                showInfo("Interview cancelled successfully!");
                loadInterviews();
            }
        });
    }

    /**
     * Handle delete interview (permanent removal)
     */
    private void handleDeleteInterview(Interview interview) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Delete Interview");
        alert.setHeaderText("⚠️ Permanently Delete Interview?");
        alert.setContentText(
            "This will PERMANENTLY remove this interview from the database.\n" +
            "This action CANNOT be undone!\n\n" +
            "Candidate: " + interview.getCandidateName() + "\n" +
            "Position: " + interview.getJobTitle() + "\n" +
            "Date: " + interview.getScheduledDate() + " at " + interview.getScheduledTime() + "\n\n" +
            "Are you absolutely sure?"
        );

        ButtonType deleteButton = new ButtonType("Delete Permanently", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == deleteButton) {
                boolean success = interviewDAO.deleteInterview(interview.getInterviewId());
                if (success) {
                    showInfo("Interview deleted permanently!");
                    loadInterviews();
                } else {
                    showError("Failed to delete interview. Please try again.");
                }
            }
        });
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
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
