package com.hirely.controllers;

import com.hirely.dao.EvaluationDAO;
import com.hirely.models.*;
import com.hirely.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Evaluation Form
 */
public class EvaluationFormController {

    @FXML private Text interviewInfoLabel;
    @FXML private VBox criteriaContainer;
    @FXML private Text overallRatingLabel;
    @FXML private RadioButton stronglyRecommendRadio;
    @FXML private RadioButton recommendRadio;
    @FXML private RadioButton neutralRadio;
    @FXML private RadioButton notRecommendRadio;
    @FXML private TextArea strengthsArea;
    @FXML private TextArea weaknessesArea;
    @FXML private TextArea commentsArea;
    @FXML private RadioButton hireRadio;
    @FXML private RadioButton noHireRadio;
    @FXML private RadioButton maybeRadio;
    @FXML private RadioButton pendingRadio;
    @FXML private TextArea nextStepsArea;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button submitButton;

    private EvaluationDAO evaluationDAO;
    private User currentUser;
    private int recruiterId;
    private Interview interview;
    private List<EvaluationCriteria> criteria;
    private Map<Integer, Slider> criteriaSliders;
    private Map<Integer, TextArea> criteriaComments;
    private boolean evaluationSubmitted = false;
    private boolean isEditMode = false;
    private int editingEvaluationId = -1;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        recruiterId = currentUser.getProfileId();
        evaluationDAO = new EvaluationDAO();

        criteriaSliders = new HashMap<>();
        criteriaComments = new HashMap<>();

        setupRadioButtons();
    }

    /**
     * Set the interview to evaluate
     */
    public void setInterview(Interview interview) {
        this.interview = interview;
        interviewInfoLabel.setText(
            "Evaluating: " + interview.getCandidateName() + " - " + interview.getJobTitle() +
            " (" + interview.getScheduledDate() + ")"
        );
        loadCriteria();
    }

    /**
     * Load existing evaluation for editing
     */
    public void loadEvaluationForEdit(InterviewEvaluation evaluation) {
        this.interview = evaluation.getInterview();
        this.isEditMode = true;
        this.editingEvaluationId = evaluation.getEvaluationId();

        interviewInfoLabel.setText(
            "Editing: " + evaluation.getInterview().getCandidateName() + " - " + evaluation.getInterview().getJobTitle() +
            " (" + evaluation.getInterview().getScheduledDate() + ")"
        );

        // Change button text
        submitButton.setText("Update Evaluation");

        loadCriteria();

        // Load evaluation data
        overallRatingLabel.setText(String.format("%.2f", evaluation.getOverallRating()));

        // Set recommendation
        switch (evaluation.getRecommendation()) {
            case "STRONGLY_RECOMMEND":
                stronglyRecommendRadio.setSelected(true);
                break;
            case "RECOMMEND":
                recommendRadio.setSelected(true);
                break;
            case "NEUTRAL":
                neutralRadio.setSelected(true);
                break;
            case "NOT_RECOMMEND":
                notRecommendRadio.setSelected(true);
                break;
        }

        // Set text areas
        strengthsArea.setText(evaluation.getStrengths() != null ? evaluation.getStrengths() : "");
        weaknessesArea.setText(evaluation.getWeaknesses() != null ? evaluation.getWeaknesses() : "");
        commentsArea.setText(evaluation.getGeneralComments() != null ? evaluation.getGeneralComments() : "");
        nextStepsArea.setText(evaluation.getNextSteps() != null ? evaluation.getNextSteps() : "");

        // Set hire decision
        switch (evaluation.getHireDecision()) {
            case "HIRE":
                hireRadio.setSelected(true);
                break;
            case "NO_HIRE":
                noHireRadio.setSelected(true);
                break;
            case "MAYBE":
                maybeRadio.setSelected(true);
                break;
            case "PENDING":
                pendingRadio.setSelected(true);
                break;
        }

        // Load individual scores
        if (evaluation.getScores() != null && !evaluation.getScores().isEmpty()) {
            for (EvaluationScore score : evaluation.getScores()) {
                Slider slider = criteriaSliders.get(score.getCriteriaId());
                TextArea commentArea = criteriaComments.get(score.getCriteriaId());

                if (slider != null) {
                    slider.setValue(score.getScore());
                }
                if (commentArea != null && score.getComments() != null) {
                    commentArea.setText(score.getComments());
                }
            }
        }
    }

    /**
     * Setup radio button groups
     */
    private void setupRadioButtons() {
        // Recommendation group
        ToggleGroup recommendationGroup = new ToggleGroup();
        stronglyRecommendRadio.setToggleGroup(recommendationGroup);
        recommendRadio.setToggleGroup(recommendationGroup);
        neutralRadio.setToggleGroup(recommendationGroup);
        notRecommendRadio.setToggleGroup(recommendationGroup);

        // Hire decision group
        ToggleGroup hireGroup = new ToggleGroup();
        hireRadio.setToggleGroup(hireGroup);
        noHireRadio.setToggleGroup(hireGroup);
        maybeRadio.setToggleGroup(hireGroup);
        pendingRadio.setToggleGroup(hireGroup);
    }

    /**
     * Load evaluation criteria and create rating controls
     */
    private void loadCriteria() {
        criteria = evaluationDAO.getActiveCriteria();

        for (EvaluationCriteria criterion : criteria) {
            VBox criterionBox = createCriterionControl(criterion);
            criteriaContainer.getChildren().add(criterionBox);
        }

        System.out.println("✅ Loaded " + criteria.size() + " evaluation criteria");
    }

    /**
     * Create rating control for a criterion
     */
    private VBox createCriterionControl(EvaluationCriteria criterion) {
        VBox container = new VBox(12);
        container.setStyle("-fx-padding: 16px; -fx-background-color: #F9FAFB; -fx-background-radius: 8px;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Text nameLabel = new Text(criterion.getCriteriaName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-fill: #2D2D2D;");

        Text categoryLabel = new Text(criterion.getCategory());
        categoryLabel.setStyle("-fx-font-size: 12px; -fx-fill: #6b7280; -fx-font-style: italic;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text weightLabel = new Text("Weight: " + String.format("%.2f", criterion.getWeight()));
        weightLabel.setStyle("-fx-font-size: 12px; -fx-fill: #9ca3af;");

        header.getChildren().addAll(nameLabel, categoryLabel, spacer, weightLabel);

        // Description
        Text description = new Text(criterion.getDescription());
        description.setStyle("-fx-font-size: 13px; -fx-fill: #6b7280;");
        description.setWrappingWidth(600);

        // Rating slider
        HBox ratingBox = new HBox(16);
        ratingBox.setAlignment(Pos.CENTER_LEFT);

        Text ratingLabelText = new Text("Rating:");
        ratingLabelText.setStyle("-fx-font-size: 14px; -fx-fill: #2D2D2D;");

        Slider slider = new Slider(0, 5, 0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setPrefWidth(300);

        Text scoreValue = new Text("0");
        scoreValue.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-fill: #FF8C42;");

        // Update score display when slider changes
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scoreValue.setText(String.format("%.0f", newVal.doubleValue()));
            calculateOverallRating();
        });

        ratingBox.getChildren().addAll(ratingLabelText, slider, scoreValue, new Text("/ 5"));

        // Comments
        Text commentsLabel = new Text("Comments (Optional):");
        commentsLabel.setStyle("-fx-font-size: 13px; -fx-fill: #6b7280;");

        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Additional notes for this criterion...");
        commentsArea.setPrefHeight(60);
        commentsArea.setWrapText(true);
        commentsArea.setStyle("-fx-font-size: 12px;");

        // Store references
        criteriaSliders.put(criterion.getCriteriaId(), slider);
        criteriaComments.put(criterion.getCriteriaId(), commentsArea);

        container.getChildren().addAll(header, description, ratingBox, commentsLabel, commentsArea);
        return container;
    }

    /**
     * Calculate overall rating (weighted average)
     */
    private void calculateOverallRating() {
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        for (EvaluationCriteria criterion : criteria) {
            Slider slider = criteriaSliders.get(criterion.getCriteriaId());
            if (slider != null) {
                double score = slider.getValue();
                double weight = criterion.getWeight();
                totalWeightedScore += score * weight;
                totalWeight += weight;
            }
        }

        double overallRating = totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;
        overallRatingLabel.setText(String.format("%.2f", overallRating));
    }

    /**
     * Handle submit button
     */
    @FXML
    private void handleSubmit() {
        hideError();

        // Validate
        if (!validateForm()) {
            return;
        }

        // Create evaluation object
        InterviewEvaluation evaluation = new InterviewEvaluation();

        if (isEditMode) {
            evaluation.setEvaluationId(editingEvaluationId);
        }

        evaluation.setInterviewId(interview.getInterviewId());
        evaluation.setRecruiterId(recruiterId);

        // Overall rating
        double overallRating = Double.parseDouble(overallRatingLabel.getText());
        evaluation.setOverallRating(overallRating);

        // Recommendation
        String recommendation = getSelectedRecommendation();
        evaluation.setRecommendation(recommendation);

        // Text fields
        evaluation.setStrengths(strengthsArea.getText().trim());
        evaluation.setWeaknesses(weaknessesArea.getText().trim());
        evaluation.setGeneralComments(commentsArea.getText().trim());

        // Hire decision
        String hireDecision = getSelectedHireDecision();
        evaluation.setHireDecision(hireDecision);
        evaluation.setNextSteps(nextStepsArea.getText().trim());

        evaluation.setDraft(false);

        // Add individual scores
        for (EvaluationCriteria criterion : criteria) {
            Slider slider = criteriaSliders.get(criterion.getCriteriaId());
            TextArea commentsArea = criteriaComments.get(criterion.getCriteriaId());

            int score = (int) slider.getValue();
            String comments = commentsArea.getText().trim();

            EvaluationScore evalScore = new EvaluationScore(criterion.getCriteriaId(), score, comments);
            evaluation.addScore(evalScore);
        }

        // Disable button during save
        submitButton.setDisable(true);
        String originalButtonText = submitButton.getText();
        submitButton.setText(isEditMode ? "Updating..." : "Submitting...");

        // Submit to database
        new Thread(() -> {
            boolean success;
            if (isEditMode) {
                success = evaluationDAO.updateEvaluation(evaluation);
            } else {
                success = evaluationDAO.submitEvaluation(evaluation);
            }

            javafx.application.Platform.runLater(() -> {
                submitButton.setDisable(false);
                submitButton.setText(originalButtonText);

                if (success) {
                    evaluationSubmitted = true;
                    showSuccess(isEditMode ? "Evaluation updated successfully!" : "Evaluation submitted successfully!");

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
                    showError(isEditMode ? "Failed to update evaluation. Please try again." : "Failed to submit evaluation. Please try again.");
                }
            });
        }).start();
    }

    /**
     * Validate form
     */
    private boolean validateForm() {
        // Check if at least one criterion is rated
        boolean hasRatings = criteriaSliders.values().stream()
            .anyMatch(slider -> slider.getValue() > 0);

        if (!hasRatings) {
            showError("Please rate at least one criterion.");
            return false;
        }

        // Check recommendation selected
        if (getSelectedRecommendation() == null) {
            showError("Please select a recommendation.");
            return false;
        }

        // Check hire decision selected
        if (getSelectedHireDecision() == null) {
            showError("Please select a hiring decision.");
            return false;
        }

        return true;
    }

    /**
     * Get selected recommendation
     */
    private String getSelectedRecommendation() {
        if (stronglyRecommendRadio.isSelected()) return "STRONGLY_RECOMMEND";
        if (recommendRadio.isSelected()) return "RECOMMEND";
        if (neutralRadio.isSelected()) return "NEUTRAL";
        if (notRecommendRadio.isSelected()) return "NOT_RECOMMEND";
        return null;
    }

    /**
     * Get selected hire decision
     */
    private String getSelectedHireDecision() {
        if (hireRadio.isSelected()) return "HIRE";
        if (noHireRadio.isSelected()) return "NO_HIRE";
        if (maybeRadio.isSelected()) return "MAYBE";
        if (pendingRadio.isSelected()) return "PENDING";
        return null;
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
     * Check if evaluation was submitted
     */
    public boolean isEvaluationSubmitted() {
        return evaluationSubmitted;
    }
}
