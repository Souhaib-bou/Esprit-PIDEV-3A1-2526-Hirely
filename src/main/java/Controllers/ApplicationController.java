package Controllers;

import Models.Application;
import Services.ApplicationService;
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
import java.sql.SQLException;
import java.time.LocalDate;

public class ApplicationController {

    // ================= FORM FIELDS =================
    @FXML private DatePicker datePicker;
    @FXML private TextArea coverLetterField;
    @FXML private TextField resumeField;
    @FXML private TextField userIdField;       // maps to user_id
    @FXML private TextField jobIdField;        // maps to jobOfferId

    // ================= TABLE =================
    @FXML private TableView<Application> tableView;
    @FXML private TableColumn<Application, Integer> colId;
    @FXML private TableColumn<Application, Integer> colUserId;
    @FXML private TableColumn<Application, Integer> colJobId;
    @FXML private TableColumn<Application, LocalDate> colDate;
    @FXML private TableColumn<Application, String> colCoverLetter;
    @FXML private TableColumn<Application, String> colResume;
    @FXML private TableColumn<Application, String> colStatus;
    @FXML private TableColumn<Application, Void> colAction;

    private ApplicationService service = new ApplicationService();
    private ObservableList<Application> data = FXCollections.observableArrayList();
    private Application selectedApplication = null;

    // Mock user role (replace with actual session)
    private String currentUserRole = "recruiter"; // or "candidate"

    // ================= INITIALIZE =================
    @FXML
    public void initialize() throws SQLException {

        colId.setCellValueFactory(new PropertyValueFactory<>("applicationId"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("user_id"));
        colJobId.setCellValueFactory(new PropertyValueFactory<>("jobOfferId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("applicationDate"));
        colCoverLetter.setCellValueFactory(new PropertyValueFactory<>("coverLetter"));
        colResume.setCellValueFactory(new PropertyValueFactory<>("resumePath"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("currentStatus"));

        addEditDeleteButtonToTable();
        loadData();
    }

    // ================= ADD APPLICATION =================
    @FXML
    public void addApplication() {
        try {
            Application app = new Application(
                    datePicker.getValue(),
                    coverLetterField.getText(),
                    resumeField.getText(),
                    Integer.parseInt(userIdField.getText()),
                    Integer.parseInt(jobIdField.getText())
            );

            service.add(app);
            loadData();
            clearFields();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE APPLICATION =================
    @FXML
    public void updateApplication() {
        try {
            if (!"recruiter".equalsIgnoreCase(currentUserRole)) {
                System.out.println("Only recruiters can update applications.");
                return;
            }

            if (selectedApplication == null) {
                System.out.println("Select an application to update.");
                return;
            }

            selectedApplication.setApplicationDate(datePicker.getValue());
            selectedApplication.setCoverLetter(coverLetterField.getText());
            selectedApplication.setResumePath(resumeField.getText());
            selectedApplication.setUser_id(Integer.parseInt(userIdField.getText()));
            selectedApplication.setJobOfferId(Integer.parseInt(jobIdField.getText()));

            service.update(selectedApplication); // Make sure this exists in ApplicationService
            loadData();
            clearFields();
            selectedApplication = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DELETE APPLICATION =================
    private void deleteApplication(Application app) {
        try {
            service.delete(app.getApplicationId());
            loadData();
            if (selectedApplication == app) selectedApplication = null;
            clearFields();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= FILL FORM =================
    private void fillForm(Application app) {
        selectedApplication = app;

        datePicker.setValue(app.getApplicationDate());
        coverLetterField.setText(app.getCoverLetter());
        resumeField.setText(app.getResumePath());
        userIdField.setText(String.valueOf(app.getuser_id()));
        jobIdField.setText(String.valueOf(app.getJobOfferId()));
    }

    // ================= TABLE ACTION BUTTONS =================
    private void addEditDeleteButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox pane = new HBox(8, editBtn, delBtn);

            {
                editBtn.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-background-radius:6;");
                delBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:6;");

                editBtn.setOnAction(event -> {
                    Application app = getTableView().getItems().get(getIndex());
                    fillForm(app);
                });

                delBtn.setOnAction(event -> {
                    Application app = getTableView().getItems().get(getIndex());
                    deleteApplication(app);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
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

    // ================= CLEAR FORM =================
    private void clearFields() {
        datePicker.setValue(null);
        coverLetterField.clear();
        resumeField.clear();
        userIdField.clear();
        jobIdField.clear();
    }

    // ================= NAVIGATION =================
    public void goBackToJobOffers(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/job_offer.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Job Offers");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}