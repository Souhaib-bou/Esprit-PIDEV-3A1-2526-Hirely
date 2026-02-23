package esprit.farouk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class for Hirely Interview & Evaluation Module
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            // Create scene
            Scene scene = new Scene(root, 900, 600);

            // Load CSS
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            // Configure stage
            primaryStage.setTitle("Hirely - Interview & Evaluation Module");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();

            // Show stage
            primaryStage.show();

            System.out.println("===========================================");
            System.out.println("  HIRELY - Application Started");
            System.out.println("===========================================");
            System.out.println("Login screen loaded successfully!");
            System.out.println("\nTest Credentials:");
            System.out.println("  Recruiter: recruiter@company.com / password123");
            System.out.println("  Candidate: jane.candidate@email.com / password123");
            System.out.println("===========================================\n");

        } catch (Exception e) {
            System.err.println("❌ Error loading login screen:");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println("\n===========================================");
        System.out.println("  HIRELY - Application Closed");
        System.out.println("===========================================");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
