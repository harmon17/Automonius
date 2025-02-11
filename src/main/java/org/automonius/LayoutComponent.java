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

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hover Button Example");

        // Create the main layout
        Parent mainLayout = createMainLayout(false);

        // Create a new scene with the main layout
        Scene scene = new Scene(mainLayout, 800, 600);

        // Set the initial scene
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public Parent createMainLayout(boolean loadProject) {
        BorderPane mainLayout = createLayout(loadProject);

        // Create the buttons with icons
        Button button1 = createButtonWithIcon("/IMAGE/bank.png");
        Button button2 = createButtonWithIcon("/IMAGE/presentation.png");
        Button button3 = createButtonWithIcon("/IMAGE/settings.png");

        // Initially hide the buttons
        button1.setVisible(false);
        button2.setVisible(false);
        button3.setVisible(false);

        // Create the button box and set its alignment
        VBox buttonBox = new VBox(10, button1, button2, button3);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.5);");

        // Create a thin trigger zone for hover at the edge of the left screen
        Pane triggerZone = new Pane();
        triggerZone.setPrefWidth(3);
        triggerZone.setStyle("-fx-background-color: transparent;");

        // Event listener to make buttons visible
        triggerZone.setOnMouseEntered(e -> {
            button1.setVisible(true);
            button2.setVisible(true);
            button3.setVisible(true);
        });

        buttonBox.setOnMouseEntered(e -> {
            button1.setVisible(true);
            button2.setVisible(true);
            button3.setVisible(true);
        });

        // Event listener to hide buttons when the mouse exits the button area
        buttonBox.setOnMouseExited(e -> {
            button1.setVisible(false);
            button2.setVisible(false);
            button3.setVisible(false);
        });

        // Create a StackPane to overlay the hover buttons on the main layout
        StackPane overlayPane = new StackPane();
        overlayPane.getChildren().addAll(triggerZone, buttonBox);
        StackPane.setAlignment(triggerZone, Pos.CENTER_LEFT);
        StackPane.setAlignment(buttonBox, Pos.CENTER_LEFT);
        StackPane.setMargin(buttonBox, new Insets(0, 0, 0, -45)); // Adjust the margin to significantly overlap buttons with the main layout

        // Add the overlayPane to the main layout
        mainLayout.setLeft(overlayPane);

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

    // Method to create a button with an icon
    private Button createButtonWithIcon(String iconPath) {
        Button button = new Button();
        Image icon = new Image(getClass().getResourceAsStream(iconPath));
        ImageView imageView = new ImageView(icon);
        imageView.setFitHeight(60); // Make the buttons bigger
        imageView.setFitWidth(60);
        button.setGraphic(imageView);
        return button;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
