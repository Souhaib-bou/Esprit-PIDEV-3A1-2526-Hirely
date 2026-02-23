package com.hirely.controllers;

import com.hirely.dao.EvaluationDAO;
import com.hirely.models.InterviewEvaluation;
import com.hirely.models.User;
import com.hirely.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Controller for Evaluations Table View
 */
public class EvaluationsTableController {

    @FXML private Text resultsCountLabel;
    @FXML private Button refreshBtn;
    @FXML private TableView<InterviewEvaluation> evaluationsTable;
    @FXML private TableColumn<InterviewEvaluation, String> candidateColumn;
    @FXML private TableColumn<InterviewEvaluation, String> jobColumn;
    @FXML private TableColumn<InterviewEvaluation, String> dateColumn;
    @FXML private TableColumn<InterviewEvaluation, String> typeColumn;
    @FXML private TableColumn<InterviewEvaluation, Integer> roundColumn;
    @FXML private TableColumn<InterviewEvaluation, Double> ratingColumn;
    @FXML private TableColumn<InterviewEvaluation, String> recommendationColumn;
    @FXML private TableColumn<InterviewEvaluation, String> hireDecisionColumn;
    @FXML private TableColumn<InterviewEvaluation, String> evaluatedDateColumn;
    @FXML private TableColumn<InterviewEvaluation, Void> actionsColumn;
    @FXML private Text totalLabel;
    @FXML private Text recommendedLabel;
    @FXML private Text rejectedLabel;
    @FXML private Text onHoldLabel;

    private EvaluationDAO evaluationDAO;
    private User currentUser;
    private int recruiterId;
    private ObservableList<InterviewEvaluation> evaluations;

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

        recruiterId = currentUser.getProfileId();
        evaluationDAO = new EvaluationDAO();

        evaluations = FXCollections.observableArrayList();

        setupTable();
        loadEvaluations();

        System.out.println("✅ Evaluations Table loaded");
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        // Candidate column
        candidateColumn.setCellValueFactory(cellData -> {
            InterviewEvaluation eval = cellData.getValue();
            if (eval.getInterview() != null) {
                return new javafx.beans.property.SimpleStringProperty(eval.getInterview().getCandidateName());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Job column
        jobColumn.setCellValueFactory(cellData -> {
            InterviewEvaluation eval = cellData.getValue();
            if (eval.getInterview() != null) {
                return new javafx.beans.property.SimpleStringProperty(eval.getInterview().getJobTitle());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Date column
        dateColumn.setCellValueFactory(cellData -> {
            InterviewEvaluation eval = cellData.getValue();
            if (eval.getInterview() != null && eval.getInterview().getScheduledDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(eval.getInterview().getScheduledDate().toString());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Type column
        typeColumn.setCellValueFactory(cellData -> {
            InterviewEvaluation eval = cellData.getValue();
            if (eval.getInterview() != null) {
                return new javafx.beans.property.SimpleStringProperty(eval.getInterview().getInterviewTypeName());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Round column
        roundColumn.setCellValueFactory(cellData -> {
            InterviewEvaluation eval = cellData.getValue();
            if (eval.getInterview() != null) {
                return new javafx.beans.property.SimpleObjectProperty<>(eval.getInterview().getInterviewRound());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(0);
        });

        // Rating column with color coding
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("overallRating"));
        ratingColumn.setCellFactory(column -> new TableCell<InterviewEvaluation, Double>() {
            @Override
            protected void updateItem(Double rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.1f/10", rating));

                    // Color code based on rating
                    if (rating >= 8) {
                        setTextFill(Color.web("#16a34a"));
                        setStyle("-fx-font-weight: 600;");
                    } else if (rating >= 6) {
                        setTextFill(Color.web("#2563eb"));
                        setStyle("-fx-font-weight: 600;");
                    } else if (rating >= 4) {
                        setTextFill(Color.web("#eab308"));
                        setStyle("-fx-font-weight: 600;");
                    } else {
                        setTextFill(Color.web("#dc2626"));
                        setStyle("-fx-font-weight: 600;");
                    }
                }
            }
        });

        // Recommendation column with color coding
        recommendationColumn.setCellValueFactory(new PropertyValueFactory<>("recommendation"));
        recommendationColumn.setCellFactory(column -> new TableCell<InterviewEvaluation, String>() {
            @Override
            protected void updateItem(String recommendation, boolean empty) {
                super.updateItem(recommendation, empty);
                if (empty || recommendation == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(recommendation);

                    switch (recommendation) {
                        case "RECOMMEND":
                            setTextFill(Color.web("#16a34a"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        case "REJECT":
                            setTextFill(Color.web("#dc2626"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        case "HOLD":
                            setTextFill(Color.web("#eab308"));
                            setStyle("-fx-font-weight: 600;");
                            break;
                        default:
                            setTextFill(Color.web("#6b7280"));
                    }
                }
            }
        });

        // Hire Decision column
        hireDecisionColumn.setCellValueFactory(new PropertyValueFactory<>("hireDecision"));
        hireDecisionColumn.setCellFactory(column -> new TableCell<InterviewEvaluation, String>() {
            @Override
            protected void updateItem(String decision, boolean empty) {
                super.updateItem(decision, empty);
                if (empty || decision == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(decision);
                    setTextFill(Color.web("#2D2D2D"));
                    setStyle("-fx-font-weight: 600;");
                }
            }
        });

        // Evaluated Date column
        evaluatedDateColumn.setCellValueFactory(cellData -> {
            InterviewEvaluation eval = cellData.getValue();
            if (eval.getEvaluatedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                return new javafx.beans.property.SimpleStringProperty(sdf.format(eval.getEvaluatedAt()));
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Actions column with buttons
        actionsColumn.setCellFactory(column -> new TableCell<InterviewEvaluation, Void>() {
            private final Button viewBtn = new Button("👁️ View");
            private final Button editBtn = new Button("✏️ Edit");
            private final Button deleteBtn = new Button("🗑️ Delete");
            private final HBox container = new HBox(6);

            {
                container.setAlignment(Pos.CENTER);
                viewBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 4px;");
                editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 4px;");
                deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4px 8px; -fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 4px;");

                viewBtn.setOnAction(e -> {
                    InterviewEvaluation evaluation = getTableView().getItems().get(getIndex());
                    handleViewEvaluation(evaluation);
                });

                editBtn.setOnAction(e -> {
                    InterviewEvaluation evaluation = getTableView().getItems().get(getIndex());
                    handleEditEvaluation(evaluation);
                });

                deleteBtn.setOnAction(e -> {
                    InterviewEvaluation evaluation = getTableView().getItems().get(getIndex());
                    handleDeleteEvaluation(evaluation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    container.getChildren().clear();
                    container.getChildren().add(viewBtn);
                    container.getChildren().add(editBtn);
                    container.getChildren().add(deleteBtn);
                    setGraphic(container);
                }
            }
        });
    }

    /**
     * Load all evaluations
     */
    private void loadEvaluations() {
        new Thread(() -> {
            List<InterviewEvaluation> evaluationsList = evaluationDAO.getAllEvaluationsForRecruiter(recruiterId);

            Platform.runLater(() -> {
                evaluations.setAll(evaluationsList);
                evaluationsTable.setItems(evaluations);
                updateStatistics();
                updateResultsCount();
                System.out.println("✅ Loaded " + evaluationsList.size() + " evaluations");
            });
        }).start();
    }

    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        int total = evaluations.size();
        int recommended = (int) evaluations.stream()
            .filter(e -> "RECOMMEND".equals(e.getRecommendation()))
            .count();
        int rejected = (int) evaluations.stream()
            .filter(e -> "REJECT".equals(e.getRecommendation()))
            .count();
        int onHold = (int) evaluations.stream()
            .filter(e -> "HOLD".equals(e.getRecommendation()))
            .count();

        totalLabel.setText("Total: " + total);
        recommendedLabel.setText("Recommended: " + recommended);
        rejectedLabel.setText("Rejected: " + rejected);
        onHoldLabel.setText("On Hold: " + onHold);
    }

    /**
     * Update results count label
     */
    private void updateResultsCount() {
        resultsCountLabel.setText(evaluations.size() + " Evaluations");
    }

    /**
     * Handle refresh
     */
    @FXML
    private void handleRefresh() {
        loadEvaluations();
    }

    /**
     * Handle view evaluation
     */
    private void handleViewEvaluation(InterviewEvaluation evaluation) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Evaluation Details");
        alert.setHeaderText("Evaluation for " + (evaluation.getInterview() != null ? evaluation.getInterview().getCandidateName() : "N/A"));

        StringBuilder content = new StringBuilder();
        if (evaluation.getInterview() != null) {
            content.append("Candidate: ").append(evaluation.getInterview().getCandidateName()).append("\n");
            content.append("Position: ").append(evaluation.getInterview().getJobTitle()).append("\n");
            content.append("Interview Type: ").append(evaluation.getInterview().getInterviewTypeName()).append("\n");
        }
        content.append("\nOverall Rating: ").append(String.format("%.1f/10", evaluation.getOverallRating()));
        content.append("\nRecommendation: ").append(evaluation.getRecommendation());
        content.append("\nHire Decision: ").append(evaluation.getHireDecision());
        content.append("\n\nStrengths:\n").append(evaluation.getStrengths());
        content.append("\n\nWeaknesses:\n").append(evaluation.getWeaknesses());
        content.append("\n\nGeneral Comments:\n").append(evaluation.getGeneralComments());

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Handle edit evaluation - opens the evaluation form with data
     */
    private void handleEditEvaluation(InterviewEvaluation evaluation) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/evaluation-form.fxml"));
            javafx.scene.Parent root = loader.load();

            EvaluationFormController controller = loader.getController();
            controller.loadEvaluationForEdit(evaluation);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Edit Evaluation");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (controller.isEvaluationSubmitted()) {
                loadEvaluations();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error opening evaluation form: " + e.getMessage());
        }
    }

    /**
     * Handle delete evaluation
     */
    private void handleDeleteEvaluation(InterviewEvaluation evaluation) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Delete Evaluation");
        alert.setHeaderText("⚠️ Delete This Evaluation?");

        StringBuilder content = new StringBuilder();
        if (evaluation.getInterview() != null) {
            content.append("Candidate: ").append(evaluation.getInterview().getCandidateName()).append("\n");
            content.append("Position: ").append(evaluation.getInterview().getJobTitle()).append("\n");
        }
        content.append("\nThis will permanently delete this evaluation.\n");
        content.append("The interview will revert to CONFIRMED status.\n");
        content.append("Are you sure?");

        alert.setContentText(content.toString());

        javafx.scene.control.ButtonType deleteButton = new javafx.scene.control.ButtonType("Delete", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancelButton = new javafx.scene.control.ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == deleteButton) {
                boolean success = evaluationDAO.deleteEvaluation(
                    evaluation.getEvaluationId(),
                    evaluation.getInterviewId()
                );

                if (success) {
                    showInfo("✅ Evaluation deleted successfully!");
                    loadEvaluations();
                } else {
                    showError("Failed to delete evaluation. Please try again.");
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



