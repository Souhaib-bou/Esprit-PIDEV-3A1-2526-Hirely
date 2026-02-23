package com.hirely.controllers;

import com.hirely.dao.UserDAO;
import com.hirely.models.User;
import com.hirely.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the login screen
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private UserDAO userDAO;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        userDAO = new UserDAO();

        // Add Enter key listener to password field
        passwordField.setOnAction(event -> handleLogin());

        // Clear error on field change
        emailField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> hideError());
    }

    /**
     * Handle login button click
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }

        // Disable button during authentication
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        // Perform authentication in background thread
        new Thread(() -> {
            User user = userDAO.authenticateUser(email, password);

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("Sign In");

                if (user != null) {
                    handleSuccessfulLogin(user);
                } else {
                    showError("Invalid email or password. Please try again.");
                }
            });
        }).start();
    }

    /**
     * Handle successful login
     */
    private void handleSuccessfulLogin(User user) {
        // Store user in session
        SessionManager.setCurrentUser(user);

        // Show success message
        System.out.println("✅ Login successful! Welcome, " + user.getFullName());

        // Route to appropriate dashboard based on role
        try {
            String fxmlFile = null;

            switch (user.getRoleName()) {
                case "RECRUITER":
                    fxmlFile = "/fxml/recruiter-dashboard.fxml";
                    System.out.println("→ Routing to Recruiter Dashboard...");
                    loadDashboard(fxmlFile, "Hirely - Recruiter Dashboard", 1200, 700);
                    break;

                case "INTERVIEWEE":
                    fxmlFile = "/fxml/candidate-dashboard.fxml";
                    System.out.println("→ Routing to Candidate Dashboard...");
                    loadDashboard(fxmlFile, "Hirely - Candidate Portal", 1200, 700);
                    break;

                case "ADMIN":
                    showInfo("Admin dashboard coming soon!");
                    break;

                default:
                    showError("Unknown role: " + user.getRoleName());
            }

        } catch (Exception e) {
            System.err.println("❌ Error loading dashboard:");
            e.printStackTrace();
            showError("Error loading dashboard. Please try again.");
        }
    }

    /**
     * Load dashboard screen
     */
    private void loadDashboard(String fxmlFile, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();

            System.out.println("✅ Dashboard loaded successfully!");

        } catch (IOException e) {
            System.err.println("❌ Error loading dashboard:");
            e.printStackTrace();
            showError("Error loading dashboard: " + e.getMessage());
        }
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setVisible(true);
    }

    /**
     * Show info message
     */
    private void showInfo(String message) {
        errorLabel.setText("ℹ️ " + message);
        errorLabel.setTextFill(javafx.scene.paint.Color.web("#2563eb"));
        errorLabel.setVisible(true);
    }

    /**
     * Hide error message
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setTextFill(javafx.scene.paint.Color.web("#dc2626"));
    }

    /**
     * Clear the login form
     */
    private void clearForm() {
        emailField.clear();
        passwordField.clear();
        hideError();
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
