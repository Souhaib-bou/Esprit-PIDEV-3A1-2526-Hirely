package Test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(
                FXMLLoader.load(getClass().getResource("/fxml/job_offer.fxml"))
        );
        stage.setScene(scene);
        stage.setTitle("HR Recruitment System");
        stage.show();
        stage.setWidth(1350);
        stage.setHeight(850);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.centerOnScreen();

    }

    public static void main(String[] args) {
        launch();
    }
}
