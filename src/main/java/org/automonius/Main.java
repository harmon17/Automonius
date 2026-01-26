package org.automonius;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
        AnchorPane root = loader.load();

        // Get controller and inject stage
        MainController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        // Create scene
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/fxml/css/styles.css")).toExternalForm()
        );

        // Set stage
        primaryStage.setTitle("Test Explorer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 🔥 Defer project setup until after stage is visible
        Platform.runLater(controller::setupInitialProject);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
