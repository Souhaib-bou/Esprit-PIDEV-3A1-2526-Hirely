package com.hirely.controllers;

import com.hirely.dao.InterviewDAO;
import com.hirely.models.*;
import com.hirely.utils.SessionManager;
import com.hirely.services.NotificationService;
import java.sql.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller for Schedule Interview dialog
 */
public class ScheduleInterviewController {

    @FXML private ComboBox<Application> applicationComboBox;
    @FXML private ComboBox<InterviewType> interviewTypeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> hourComboBox;
    @FXML private ComboBox<String> minuteComboBox;
    @FXML private ComboBox<Integer> durationComboBox;
    @FXML private RadioButton virtualRadio;
    @FXML private RadioButton inPersonRadio;
    @FXML private VBox meetingLinkBox;
    @FXML private TextField meetingLinkField;
    @FXML private VBox locationBox;
    @FXML private TextField locationField;
    @FXML private ComboBox<Integer> roundComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button scheduleButton;

    private InterviewDAO interviewDAO;
    private NotificationService notificationService;
    private User currentUser;
    private int recruiterId;
    private boolean interviewScheduled = false;
    private boolean isEditMode = false;
    private Interview editingInterview = null;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        recruiterId = currentUser.getProfileId();
        interviewDAO = new InterviewDAO();
        notificationService = new NotificationService();

        setupRadioButtons();
        setupTimeFields();
        setupDurationField();
        setupRoundField();
        loadApplications();
        loadInterviewTypes();

        System.out.println("✅ Schedule Interview dialog initialized");
    }

    /**
     * Set edit mode and populate fields with existing interview data
     */
    public void setEditMode(Interview interview) {
        this.isEditMode = true;
        this.editingInterview = interview;

        // Change button text
        scheduleButton.setText("Update Interview");

        // Disable application selection in edit mode
        applicationComboBox.setDisable(true);

        // Pre-populate fields
        Platform.runLater(() -> {
            // Find and select interview type
            for (InterviewType type : interviewTypeComboBox.getItems()) {
                if (type.getInterviewTypeId() == interview.getInterviewTypeId()) {
                    interviewTypeComboBox.setValue(type);
                    break;
                }
            }

            // Set date
            if (interview.getScheduledDate() != null) {
                datePicker.setValue(interview.getScheduledDate().toLocalDate());
            }

            // Set time
            if (interview.getScheduledTime() != null) {
                LocalTime time = interview.getScheduledTime().toLocalTime();
                hourComboBox.setValue(String.format("%02d", time.getHour()));
                minuteComboBox.setValue(String.format("%02d", time.getMinute()));
            }

            // Set duration
            durationComboBox.setValue(interview.getDurationMinutes());

            // Set format (virtual vs in-person)
            if (interview.getMeetingLink() != null && !interview.getMeetingLink().isEmpty()) {
                virtualRadio.setSelected(true);
                meetingLinkField.setText(interview.getMeetingLink());
            } else if (interview.getLocation() != null && !interview.getLocation().isEmpty()) {
                inPersonRadio.setSelected(true);
                locationField.setText(interview.getLocation());
            }

            // Set round
            roundComboBox.setValue(interview.getInterviewRound());

            // Set notes
            if (interview.getNotes() != null) {
                notesArea.setText(interview.getNotes());
            }

            System.out.println("✅ Edit mode activated for interview ID: " + interview.getInterviewId());
        });
    }

    /**
     * Setup radio buttons for format selection
     */
    private void setupRadioButtons() {
        ToggleGroup formatGroup = new ToggleGroup();
        virtualRadio.setToggleGroup(formatGroup);
        inPersonRadio.setToggleGroup(formatGroup);

        // Show/hide fields based on selection
        virtualRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            meetingLinkBox.setVisible(newVal);
            meetingLinkBox.setManaged(newVal);
            locationBox.setVisible(!newVal);
            locationBox.setManaged(!newVal);
        });
    }

    /**
     * Setup time dropdown fields
     */
    private void setupTimeFields() {
        // Hours (00-23)
        for (int i = 0; i < 24; i++) {
            hourComboBox.getItems().add(String.format("%02d", i));
        }
        hourComboBox.setValue("09"); // Default 9 AM

        // Minutes (00, 15, 30, 45)
        minuteComboBox.getItems().addAll("00", "15", "30", "45");
        minuteComboBox.setValue("00");
    }

    /**
     * Setup duration field
     */
    private void setupDurationField() {
        durationComboBox.getItems().addAll(15, 30, 45, 60, 90, 120);
        durationComboBox.setValue(60); // Default 60 minutes
    }

    /**
     * Setup interview round field
     */
    private void setupRoundField() {
        roundComboBox.getItems().addAll(1, 2, 3, 4, 5);
        roundComboBox.setValue(1); // Default first round
    }

    /**
     * Load shortlisted applications
     */
    private void loadApplications() {
        List<Application> applications = interviewDAO.getShortlistedApplications();
        applicationComboBox.getItems().setAll(applications);

        if (applications.isEmpty()) {
            applicationComboBox.setPromptText("No candidates available for interview");
            applicationComboBox.setDisable(true);
        }
    }

    /**
     * Load interview types
     */
    private void loadInterviewTypes() {
        List<InterviewType> types = interviewDAO.getInterviewTypes();
        interviewTypeComboBox.getItems().setAll(types);

        // Auto-select duration when type is selected
        interviewTypeComboBox.setOnAction(e -> {
            InterviewType selected = interviewTypeComboBox.getValue();
            if (selected != null) {
                durationComboBox.setValue(selected.getTypicalDurationMinutes());
            }
        });
    }

    /**
     * Handle schedule button
     */
    @FXML
    private void handleSchedule() {
        hideError();

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Create or use existing interview object
        Interview interview = isEditMode ? editingInterview : new Interview();

        // Set IDs (only for new interviews)
        if (!isEditMode) {
            Application selectedApp = applicationComboBox.getValue();
            interview.setApplicationId(selectedApp.getApplicationId());
            interview.setRecruiterId(recruiterId);
            interview.setIntervieweeId(selectedApp.getIntervieweeId());
        }

        // Update interview type
        interview.setInterviewTypeId(interviewTypeComboBox.getValue().getInterviewTypeId());

        // Set date and time
        LocalDate date = datePicker.getValue();
        interview.setScheduledDate(Date.valueOf(date));

        int hour = Integer.parseInt(hourComboBox.getValue());
        int minute = Integer.parseInt(minuteComboBox.getValue());
        interview.setScheduledTime(Time.valueOf(LocalTime.of(hour, minute)));

        // Set duration
        interview.setDurationMinutes(durationComboBox.getValue());

        // Set location or meeting link
        if (virtualRadio.isSelected()) {
            interview.setMeetingLink(meetingLinkField.getText().trim());
            interview.setLocation(null);
        } else {
            interview.setLocation(locationField.getText().trim());
            interview.setMeetingLink(null);
        }

        // Set round and notes
        interview.setInterviewRound(roundComboBox.getValue());
        interview.setNotes(notesArea.getText().trim());

        // Disable button during save
        scheduleButton.setDisable(true);
        String originalButtonText = scheduleButton.getText();
        scheduleButton.setText(isEditMode ? "Updating..." : "Scheduling...");

        // Save to database
        new Thread(() -> {
            boolean success;

            if (isEditMode) {
                // Update existing interview
                success = interviewDAO.updateInterview(interview);
            } else {
                // Create new interview
                Application selectedApp = applicationComboBox.getValue();
                success = interviewDAO.scheduleInterview(interview);

                if (success) {
                    // Send notification to candidate (only for new interviews)
                    try {
                        int candidateUserId = getCandidateUserId(selectedApp.getIntervieweeId());

                        notificationService.notifyInterviewScheduled(
                            interview,
                            candidateUserId,
                            selectedApp.getCandidateName(),
                            selectedApp.getJobTitle()
                        );
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to send notification: " + e.getMessage());
                    }
                }
            }

            javafx.application.Platform.runLater(() -> {
                scheduleButton.setDisable(false);
                scheduleButton.setText(originalButtonText);

                if (success) {
                    interviewScheduled = true;
                    String successMsg = isEditMode ?
                        "Interview updated successfully!" :
                        "Interview scheduled successfully! Notification sent to candidate.";
                    showSuccess(successMsg);

                    // Close dialog after 1 second
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            javafx.application.Platform.runLater(this::closeDialog);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    String errorMsg = isEditMode ?
                        "Failed to update interview. Please try again." :
                        "Failed to schedule interview. Please try again.";
                    showError(errorMsg);
                }
            });
        }).start();
    }

    /**
     * Validate all inputs
     */
    private boolean validateInputs() {
        // Check candidate selected
        if (applicationComboBox.getValue() == null) {
            showError("Please select a candidate.");
            return false;
        }

        // Check interview type selected
        if (interviewTypeComboBox.getValue() == null) {
            showError("Please select an interview type.");
            return false;
        }

        // Check date selected
        if (datePicker.getValue() == null) {
            showError("Please select a date.");
            return false;
        }

        // Check date is not in the past
        if (datePicker.getValue().isBefore(LocalDate.now())) {
            showError("Interview date cannot be in the past.");
            return false;
        }

        // Check time selected
        if (hourComboBox.getValue() == null || minuteComboBox.getValue() == null) {
            showError("Please select a time.");
            return false;
        }

        // Check format-specific fields
        if (virtualRadio.isSelected()) {
            String link = meetingLinkField.getText().trim();
            if (link.isEmpty()) {
                showError("Please enter a meeting link for virtual interviews.");
                return false;
            }
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                showError("Meeting link must start with http:// or https://");
                return false;
            }
        } else {
            String location = locationField.getText().trim();
            if (location.isEmpty()) {
                showError("Please enter a location for in-person interviews.");
                return false;
            }
        }

        return true;
    }

    /**
     * Handle cancel button
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setTextFill(javafx.scene.paint.Color.web("#dc2626"));
        errorLabel.setVisible(true);
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        errorLabel.setText("✅ " + message);
        errorLabel.setTextFill(javafx.scene.paint.Color.web("#16a34a"));
        errorLabel.setVisible(true);
    }

    /**
     * Hide error message
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }

    /**
     * Get candidate user ID from interviewee ID
     */
    private int getCandidateUserId(int intervieweeId) {
        String query = "SELECT user_id FROM interviewee_profiles WHERE interviewee_id = ?";

        try (Connection conn = com.hirely.config.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, intervieweeId);
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
     * Check if interview was scheduled
     */
    public boolean isInterviewScheduled() {
        return interviewScheduled;
    }
}
