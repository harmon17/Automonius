package org.automonius;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LayoutComponent extends Application {

    private Scene mainScene;
    private Scene executionScene;
    private Stage primaryStage;
    private TreeTableViewComponent treeTableViewComponent;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Button Navigation Example");

        // Create the main layout
        Parent mainLayout = createMainLayout(false);
        mainScene = new Scene(mainLayout, 800, 600);

        // Create the ExecutionPane layout
        executionScene = new Scene(new ExecutionPane(this::switchToMainLayout), 800, 600);

        // Set the initial scene to the main layout
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public Parent createMainLayout(boolean loadProject) {
        BorderPane mainLayout = createLayout(loadProject);

        // Initialize the TableViewComponent
        TableViewComponent tableViewComponent = new TableViewComponent(loadProject);

        // Initialize the TreeTableViewComponent with the initialized TableViewComponent
        treeTableViewComponent = new TreeTableViewComponent(loadProject, tableViewComponent, new VBox());

        // Create the additional buttons with images
        Button button1 = createButtonWithImage("IMAGE/bank.png", 60, 60);
        Button button2 = createButtonWithImage("IMAGE/presentation.png", 60, 60);
        Button button3 = createButtonWithImage("IMAGE/settings.png", 60, 60);

        // Event listener to switch to the ExecutionPane when the middle button is clicked
        button2.setOnAction(e -> switchToExecutionPane());

        // Event listener to switch to the main layout when the top button is clicked
        button1.setOnAction(e -> switchToMainLayout());

        // Layout for the additional buttons
        VBox buttonLayout = new VBox(10, button1, button2, button3);
        buttonLayout.setAlignment(Pos.CENTER_LEFT);
        buttonLayout.setPadding(new Insets(10));

        // Add the buttons layout to the left of the main layout
        mainLayout.setLeft(buttonLayout);

        return mainLayout;
    }

    public BorderPane createLayout(boolean loadProject) {
        BorderPane mainLayout = new BorderPane();

        MenuBarComponent menuBarComponent = new MenuBarComponent();
        mainLayout.setTop(menuBarComponent.createMenuBar());

        MainController mainController = new MainController(loadProject);
        VBox mainContainer = mainController.getMainContainer();
        VBox objectRepositoryView = mainController.createObjectRepositoryView();
        VBox reusableComponentView = mainController.createReusableComponentTableView();
        VBox testPlanView = mainController.createTestPlanTableView();
        VBox tableView2Box = mainController.createTableView2();
        VBox propertiesBox = mainController.createPropertiesView();

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(60);
        ColumnConstraints cc3 = new ColumnConstraints();
        cc3.setPercentWidth(20);

        GridPane topArea = new GridPane();
        topArea.getColumnConstraints().addAll(cc1, cc2, cc3);
        topArea.add(testPlanView, 0, 0);
        topArea.add(mainContainer, 1, 0);
        topArea.add(objectRepositoryView, 2, 0);

        GridPane bottomArea = new GridPane();
        bottomArea.getColumnConstraints().addAll(cc1, cc2, cc3);
        bottomArea.add(reusableComponentView, 0, 0);
        bottomArea.add(tableView2Box, 1, 0);
        bottomArea.add(propertiesBox, 2, 0);

        RowConstraints rc1 = new RowConstraints();
        rc1.setPercentHeight(50);
        RowConstraints rc2 = new RowConstraints();
        rc2.setPercentHeight(50);

        GridPane mainArea = new GridPane();
        mainArea.getRowConstraints().addAll(rc1, rc2);
        mainArea.add(topArea, 0, 0);
        mainArea.add(bottomArea, 0, 1);

        mainLayout.setCenter(mainArea);

        return mainLayout;
    }

    private Button createButtonWithImage(String imagePath, double width, double height) {
        Button button = new Button();
        try {
            // Log the image path
            System.out.println("Loading image from: " + getClass().getClassLoader().getResource(imagePath));

            // Use ClassLoader to load the image resource
            Image icon = new Image(getClass().getClassLoader().getResourceAsStream(imagePath));
            ImageView imageView = new ImageView(icon);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            button.setGraphic(imageView);
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
        }
        return button;
    }

    private void switchToExecutionPane() {
        if (primaryStage != null) {
            primaryStage.setScene(executionScene);
        } else {
            System.out.println("Error: primaryStage is null");
        }
    }

    private void switchToMainLayout() {
        if (primaryStage != null) {
            primaryStage.setScene(mainScene);
        } else {
            System.out.println("Error: primaryStage is null");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
