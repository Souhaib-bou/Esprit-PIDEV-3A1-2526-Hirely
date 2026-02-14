package Controllers;

import Models.JobOffer;
import Services.JobOfferService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;

public class JobOfferController {

    // ================= FORM FIELDS =================
    @FXML private TextField titleField;
    @FXML private TextField userIdField; // can be hidden or    auto-filled
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> contractTypeBox;
    @FXML private TextField salaryField;
    @FXML private TextField locationField;
    @FXML private TextField experienceField;
    @FXML private DatePicker publicationDatePicker;
    @FXML private ComboBox<String> statusBox;

    // ================= TABLE =================
    @FXML private TableView<JobOffer> tableView;
    @FXML private TableColumn<JobOffer, Integer> colId;
    @FXML private TableColumn<JobOffer, String> colTitle;
    @FXML private TableColumn<JobOffer, String> colContract;
    @FXML private TableColumn<JobOffer, Double> colSalary;
    @FXML private TableColumn<JobOffer, Integer> colExperience;
    @FXML private TableColumn<JobOffer, String> colLocation;
    @FXML private TableColumn<JobOffer, String> colStatus;
    @FXML private TableColumn<JobOffer, Integer> colUserId; // new column to show creator
    @FXML private TableColumn<JobOffer, Void> colAction;

    private JobOfferService service = new JobOfferService();
    private ObservableList<JobOffer> data = FXCollections.observableArrayList();

    private JobOffer selectedJob = null; // currently editing job

    // Replace with actual logged-in user ID from your session/auth system
    private int currentUserId = 1;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        // Fill ComboBoxes
        contractTypeBox.getItems().addAll("CDI","CDD","Internship","Freelance");
        statusBox.getItems().addAll("Open","Closed");

        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("jobOfferId"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colContract.setCellValueFactory(new PropertyValueFactory<>("contractType"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colExperience.setCellValueFactory(new PropertyValueFactory<>("experienceRequired"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("user_id")); // new column

        // Add Edit buttons to table
        addEditButtonToTable();

        // Load data
        loadData();
    }

    // ================= ADD =================
    @FXML
    public void addJobOffer() {
        try {

            JobOffer j = new JobOffer(
                    titleField.getText(),
                    descriptionField.getText(),
                    contractTypeBox.getValue(),
                    Double.parseDouble(salaryField.getText()),
                    locationField.getText(),
                    Integer.parseInt(experienceField.getText()),
                    Date.valueOf(publicationDatePicker.getValue()),
                    statusBox.getValue(),
                    currentUserId // assign logged-in user
            );

            service.add(j);
            loadData();
            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE =================
    @FXML
    public void updateJobOffer() {
        try {

            if (selectedJob == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Update Warning");
                alert.setHeaderText(null);
                alert.setContentText("Please click the Edit button on a row first!");
                alert.showAndWait();
                return;
            }

            // Only allow update if the current user is the creator
            if (selectedJob.getUser_id() != currentUserId) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Permission Denied");
                alert.setHeaderText(null);
                alert.setContentText("You can only edit your own job offers.");
                alert.showAndWait();
                return;
            }

            // update the selected object
            selectedJob.setTitle(titleField.getText());
            selectedJob.setDescription(descriptionField.getText());
            selectedJob.setContractType(contractTypeBox.getValue());
            selectedJob.setSalary(Double.parseDouble(salaryField.getText()));
            selectedJob.setLocation(locationField.getText());
            selectedJob.setExperienceRequired(Integer.parseInt(experienceField.getText()));
            selectedJob.setPublicationDate(Date.valueOf(publicationDatePicker.getValue()));
            selectedJob.setStatus(statusBox.getValue());

            service.update(selectedJob);
            loadData();
            clearFields();
            selectedJob = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= FILL FORM =================
    private void fillForm(JobOffer job) {
        if (job == null) return;

        selectedJob = job;

        titleField.setText(job.getTitle());
        descriptionField.setText(job.getDescription());
        contractTypeBox.setValue(job.getContractType());
        salaryField.setText(String.valueOf(job.getSalary()));
        experienceField.setText(String.valueOf(job.getExperienceRequired()));
        locationField.setText(job.getLocation());
        statusBox.setValue(job.getStatus());
        userIdField.setText(String.valueOf(job.getUser_id())); // optional, hidden
        if (job.getPublicationDate() != null)
            publicationDatePicker.setValue(job.getPublicationDate().toLocalDate());
    }

    // ================= CLEAR FORM =================
    private void clearFields() {
        titleField.clear();
        descriptionField.clear();
        salaryField.clear();
        locationField.clear();
        experienceField.clear();
        publicationDatePicker.setValue(null);
        contractTypeBox.setValue(null);
        statusBox.setValue(null);
        userIdField.setText(String.valueOf(currentUserId));
    }

    // ================= LOAD DATA =================
    private void loadData() {
        try {
            data.setAll(service.getAll());
            tableView.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= EDIT/DELETE BUTTONS =================
    private void addEditButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-background-radius:6;");
                editBtn.setOnAction(event -> {
                    JobOffer job = getTableView().getItems().get(getIndex());
                    fillForm(job);
                });

                deleteBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:6;");
                deleteBtn.setOnAction(event -> {
                    JobOffer job = getTableView().getItems().get(getIndex());
                    if (job.getUser_id() != currentUserId) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Permission Denied");
                        alert.setHeaderText(null);
                        alert.setContentText("You can only delete your own job offers.");
                        alert.showAndWait();
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Confirmation");
                    alert.setHeaderText(null);
                    alert.setContentText("Are you sure you want to delete job: " + job.getTitle() + "?");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            try {
                                service.delete(job.getJobOfferId());
                                loadData();
                                if (selectedJob != null && selectedJob.getJobOfferId() == job.getJobOfferId()) {
                                    clearFields();
                                    selectedJob = null;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ================= NAVIGATION =================
    public void goToApplications(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/application.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Applications");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}