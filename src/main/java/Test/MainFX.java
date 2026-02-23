package Test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class - uses Hirely Login Screen
 */
public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Load the Hirely login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        // Create scene
        Scene scene = new Scene(root, 900, 600);

        // Load CSS styling
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        // Configure stage
        stage.setTitle("Hirely - Interview & Evaluation Module");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();

        // Show stage
        stage.show();

        System.out.println("===========================================");
        System.out.println("  HIRELY - Application Started");
        System.out.println("===========================================");
        System.out.println("Login screen loaded successfully!");
        System.out.println("\nTest Credentials:");
        System.out.println("  Recruiter: recruiter@company.com / password123");
        System.out.println("  Candidate: jane.candidate@email.com / password123");
        System.out.println("===========================================\n");
    }

    public static void main(String[] args) {
        launch();
    }
}
