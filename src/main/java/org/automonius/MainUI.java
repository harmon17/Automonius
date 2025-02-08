package org.automonius;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        start(primaryStage, false);
    }

    public void start(Stage primaryStage, boolean loadProject) {
        System.out.println("Starting Main UI...");
        primaryStage.setTitle("Main UI");

        // Create MainController
        MainController mainController = new MainController(loadProject);

        // Create Menu Bar
        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("Menu Here, that can add button");
        MenuItem addDirectoryItem = new MenuItem("Add Directory");
        MenuItem addTableViewItem = new MenuItem("Add TableView");
        menu.getItems().addAll(addDirectoryItem, addTableViewItem);
        menuBar.getMenus().add(menu);

        // Create Main Layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(menuBar);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        // Add components to grid
        gridPane.add(mainController.createTestPlanTableView(), 0, 0);
        gridPane.add(mainController.createTableView1(), 1, 0);
        gridPane.add(mainController.createObjectRepositoryView(), 2, 0);
        gridPane.add(mainController.createReusableComponentTableView(), 0, 1);
        gridPane.add(mainController.createTableView2(), 1, 1);
        gridPane.add(mainController.createPropertiesView(), 2, 1);

        mainLayout.setCenter(gridPane);

        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
