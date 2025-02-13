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
    private Scene secondaryScene;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Button Navigation Example");

        // Create the main layout
        Parent mainLayout = createMainLayout(false);
        mainScene = new Scene(mainLayout, 800, 600);

        // Create the secondary layout
        Parent secondaryLayout = createSecondaryLayout();
        secondaryScene = new Scene(secondaryLayout, 800, 600);

        // Set the initial scene
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public Parent createMainLayout(boolean loadProject) {
        BorderPane mainLayout = createLayout(loadProject);

        // Create the buttons with icons
        Button button1 = createButtonWithIcon("/IMAGE/bank.png");
        Button button2 = createButtonWithIcon("/IMAGE/presentation.png");
        Button button3 = createButtonWithIcon("/IMAGE/settings.png");

        // Event listener to switch to secondary layout when middle button is clicked
        button2.setOnAction(e -> switchToSecondaryLayout());

        // Create the button box and set its alignment
        VBox buttonBox = new VBox(10, button1, button2, button3);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10));

        // Add the button box directly to the main layout
        mainLayout.setLeft(buttonBox);

        return mainLayout;
    }

    public Parent createSecondaryLayout() {
        BorderPane secondaryLayout = new BorderPane();

        // Create a button to return to the main layout
        Button backButton = new Button("Back to Main Page");
        backButton.setOnAction(e -> switchToMainLayout());

        // Align the back button to the center
        StackPane centerPane = new StackPane();
        centerPane.getChildren().add(backButton);
        secondaryLayout.setCenter(centerPane);

        return secondaryLayout;
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

    private void switchToSecondaryLayout() {
        primaryStage.setScene(secondaryScene);
    }

    private void switchToMainLayout() {
        primaryStage.setScene(mainScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
