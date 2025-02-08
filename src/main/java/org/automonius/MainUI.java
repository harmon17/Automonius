package org.automonius;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        start(primaryStage, false);
    }

    public void start(Stage primaryStage, boolean loadProject) {
        System.out.println("Starting Main UI...");
        primaryStage.setTitle("Main UI");

        // Create Layout for Main Area
        LayoutComponent layoutComponent = new LayoutComponent();
        BorderPane layout = layoutComponent.createLayout(loadProject);

        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
