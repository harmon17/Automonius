package org.automonius;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control        .MenuBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.Commands.SampleCommands;
import org.utils.ActionDiscovery;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class LayoutComponent extends Application {

    private Scene mainScene;
    private Scene executionScene;
    private Stage primaryStage;
    private VBox buttonLayout;
    private boolean areButtonsVisible;
    private BorderPane mainLayout;
    private BorderPane executionLayout;
    private MenuBarComponent menuBarComponent;
    private HBox topContainer;
    private TranslateTransition showTransition;
    private TranslateTransition hideTransition;
    private TableManager tableManager;
    private List<Method> actions;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Button Navigation Example");

        // Discover actions and create a centralized TableManager
        actions = ActionDiscovery.discoverActions(SampleCommands.class);
        tableManager = new TableManager(actions);

        // Create the main layout
        Parent mainLayout = createMainLayout(false);
        mainScene = new Scene(mainLayout, 800, 600);

        // Create the ExecutionPane layout
        executionLayout = createExecutionLayout();
        executionScene = new Scene(executionLayout, 800, 600);

        // Set the initial scene to the main layout
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public Parent createMainLayout(boolean loadProject) {
        mainLayout = createLayout(loadProject);

        // Initialize the TableViewComponent
        TableViewComponent tableViewComponent = new TableViewComponent(loadProject, tableManager);

        // Initialize the TreeTableViewManager with the initialized TableManager
        TreeTableViewManager treeTableViewManager = new TreeTableViewManager(loadProject, tableManager, new VBox());

        // Create the additional buttons with images
        Button button1 = createButtonWithImage("IMAGE/bank.png", 60, 60);
        Button button2 = createButtonWithImage("IMAGE/presentation.png", 60, 60);
        Button button3 = createButtonWithImage("IMAGE/settings.png", 60, 60);

        // Event listener to switch to the ExecutionPane when the middle button is clicked
        button2.setOnAction(e -> switchToExecutionPane());

        // Event listener to switch to the main layout when the top button is clicked
        button1.setOnAction(e -> switchToMainLayout());

        // Layout for the additional buttons
        buttonLayout = new VBox(10, button1, button2, button3);
        buttonLayout.setAlignment(Pos.CENTER_LEFT);
        buttonLayout.setPadding(new Insets(10));
        buttonLayout.setStyle("-fx-background-color: #f0f0f0;");
        buttonLayout.setPrefWidth(80); // Set the preferred width to fit the buttons

        // Initially hide buttons
        areButtonsVisible = false;
        buttonLayout.setTranslateX(-buttonLayout.getPrefWidth());
        buttonLayout.setVisible(false);

        // Create slide in/out transitions
        showTransition = new TranslateTransition(Duration.millis(300), buttonLayout);
        hideTransition = new TranslateTransition(Duration.millis(300), buttonLayout);

        return mainLayout;
    }

    public BorderPane createLayout(boolean loadProject) {
        mainLayout = new BorderPane();

        menuBarComponent = new MenuBarComponent();
        MenuBar menuBar = menuBarComponent.createMenuBar(this);

        // Create the toggle button with an image
        Button toggleButton = createButtonWithImage("IMAGE/toggle.png", 20, 20);
        toggleButton.setOnAction(e -> toggleButtonsVisibility());

        // Add the toggle button and the menu bar to a horizontal box
        topContainer = new HBox(10, toggleButton, menuBar);
        topContainer.setAlignment(Pos.CENTER_LEFT);
        topContainer.setPadding(new Insets(10));

        mainLayout.setTop(topContainer);

        MainController mainController = new MainController(loadProject, tableManager);
        VBox mainContainer = mainController.getMainContainer();
        VBox objectRepositoryView = mainController.createObjectRepositoryView();
        VBox reusableComponentView = mainController.createReusableComponentTableView();
        VBox testPlanView = mainController.createTestPlanTableView();
        VBox tableView2Box = mainController.createTableView2();
//        VBox propertiesBox = mainController.createPropertiesView();

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
//        bottomArea.add(propertiesBox, 2, 0);

        RowConstraints rc1 = new RowConstraints();
        rc1.setPercentHeight(50);
        RowConstraints rc2 = new RowConstraints();
        rc2.setPercentHeight(50);

        GridPane mainArea = new GridPane();
        mainArea.getRowConstraints().addAll(rc1, rc2);
        mainArea.add(topArea, 0, 0);
        mainArea.add(bottomArea, 0, 1);

        mainLayout.setCenter(mainArea);

        // Initially hide buttons
        mainLayout.setLeft(null);

        return mainLayout;
    }

    public BorderPane createExecutionLayout() {
        executionLayout = new BorderPane();

        menuBarComponent = new MenuBarComponent();
        MenuBar menuBar = menuBarComponent.createMenuBar(this);

        // Add the toggle button and the menu bar to a horizontal box
        HBox executionTopContainer = new HBox(10, topContainer.getChildren().get(0), menuBar);
        executionTopContainer.setAlignment(Pos.CENTER_LEFT);
        executionTopContainer.setPadding(new Insets(10));

        executionLayout.setTop(executionTopContainer);

        VBox executionContent = new VBox();
        executionContent.setAlignment(Pos.CENTER);
        executionContent.setSpacing(10);
        executionContent.setPadding(new Insets(10));

        // Add components to executionContent
        executionContent.getChildren().add(new Label("Execution Content")); // Example content

        executionLayout.setCenter(executionContent);

        return executionLayout;
    }

    public void hideButtons() {
        hideTransition.setToX(-buttonLayout.getPrefWidth());
        hideTransition.setOnFinished(e -> {
            buttonLayout.setVisible(false);
            ((BorderPane) primaryStage.getScene().getRoot()).setLeft(null);
        });
        hideTransition.play();
        areButtonsVisible = false;
    }

    public void showButtons() {
        buttonLayout.setVisible(true);
        ((BorderPane) primaryStage.getScene().getRoot()).setLeft(buttonLayout);
        showTransition.setToX(0);
        showTransition.play();
        areButtonsVisible = true;
    }

    private Button createButtonWithImage(String imagePath, double width, double height) {
        Button button = new Button();
        try {
            // Log the image path
            System.out.println("Loading image from: " + getClass().getClassLoader().getResource(imagePath));

            // Use ClassLoader to load the image resource
            Image icon = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imagePath)));
            ImageView imageView = new ImageView(icon);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            button.setGraphic(imageView);
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
            button.setText("Error loading image");
        }
        return button;
    }

    private void toggleButtonsVisibility() {
        if (areButtonsVisible) {
            hideButtons();
        } else {
            showButtons();
        }
    }

    private void switchToExecutionPane() {
        if (primaryStage != null) {
            primaryStage.setScene(executionScene);
            synchronizeLayout();
        } else {
            System.out.println("Error: primaryStage is null");
        }
    }

    private void switchToMainLayout() {
        if (primaryStage != null) {
            primaryStage.setScene(mainScene);
            synchronizeLayout();
        } else {
            System.out.println("Error: primaryStage is null");
        }
    }

    private void synchronizeLayout() {
        BorderPane rootPane = (BorderPane) primaryStage.getScene().getRoot();
        if (rootPane != null) {
            if (areButtonsVisible) {
                rootPane.setLeft(buttonLayout);
                buttonLayout.setTranslateX(0);
                buttonLayout.setVisible(true);
            } else {
                rootPane.setLeft(null);
                buttonLayout.setTranslateX(-buttonLayout.getPrefWidth());
                buttonLayout.setVisible(false);
            }
            rootPane.setTop(topContainer);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
