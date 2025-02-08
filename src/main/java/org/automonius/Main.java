package org.automonius;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting Main Application...");
        primaryStage.setTitle("Project Manager");

        Button newProjectBtn = new Button("Create New Project");
        Button loadProjectBtn = new Button("Load Existing Project");

        newProjectBtn.setOnAction(e -> {
            System.out.println("Create New Project button clicked.");
            openMainUI(false);
        });
        loadProjectBtn.setOnAction(e -> {
            System.out.println("Load Existing Project button clicked.");
            openMainUI(true);
        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(newProjectBtn, loadProjectBtn);

        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openMainUI(boolean loadProject) {
        try {
            System.out.println("Opening Main UI...");
            new MainUI().start(new Stage(), loadProject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
