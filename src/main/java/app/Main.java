package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.DebugLog;
import util.PerspectiveClient;
import util.Session;

import java.util.concurrent.CompletableFuture;

/**
 * JavaFX entry point for the Hirely forum desktop app.
 * Loads the main user forum view and applies shared window styling.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Boot into the user forum screen.
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UserForumView.fxml"));
        Scene scene = new Scene(loader.load(), 1400, 850);

        // Shared stylesheet used by all forum windows.
        scene.getStylesheets().add(
                getClass().getResource(Session.getThemeStylesheetPath()).toExternalForm());

        stage.setTitle("Hirely Forum");
        stage.setScene(scene);
        scene.getRoot().setStyle("-fx-font-size: 15px;");

        // Undecorated window; app bar buttons handle minimize/maximize/close.
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // Base responsive sizing for desktop usage.
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setResizable(true);
        stage.sizeToScene();
        stage.centerOnScreen();

        stage.show();

        runPerspectiveSelfTestAsync();
    }

    public static void main(String[] args) {
        launch();
    }

    private void runPerspectiveSelfTestAsync() {
        CompletableFuture.runAsync(() -> {
            DebugLog.info("PerspectiveSelfTest", "Starting Perspective self-test");
            try {
                PerspectiveClient.PerspectiveResult result = new PerspectiveClient().selfTest();
                DebugLog.info("PerspectiveSelfTest", "Perspective self-test success: toxicity="
                        + result.getToxicity() + ", latencyMs=" + result.getLatencyMs());
            } catch (Exception ex) {
                DebugLog.error("PerspectiveSelfTest", "Perspective self-test failed: " + safeMessage(ex), ex);
            }
        });
    }

    private String safeMessage(Throwable ex) {
        if (ex == null) {
            return "";
        }
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return msg.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
