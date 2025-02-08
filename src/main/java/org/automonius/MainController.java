package org.automonius;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController extends Application {

    private final TreeTableViewComponent testPlanTreeTableViewComponent;
    private final TreeTableViewComponent reusableComponentTreeTableViewComponent;
    private final TableViewComponent tableViewComponent;
    private final VBox mainContainer;

    public MainController(boolean loadProject) {
        tableViewComponent = new TableViewComponent(loadProject);
        mainContainer = new VBox();

        testPlanTreeTableViewComponent = new TreeTableViewComponent(loadProject, tableViewComponent, mainContainer);
        reusableComponentTreeTableViewComponent = new TreeTableViewComponent(loadProject, tableViewComponent, mainContainer);

        // Add TableView1 to the main container initially
        TableView<ObservableList<String>> initialTableView = tableViewComponent.createNewTableView("TableView1");
        mainContainer.getChildren().add(tableViewComponent.createCommonTableViewLayout(initialTableView, "TableView1"));
    }

    public VBox getMainContainer() {
        return mainContainer;
    }

    public VBox createObjectRepositoryView() {
        return testPlanTreeTableViewComponent.createTreeTableView(false);
    }

    public VBox createReusableComponentTableView() {
        return reusableComponentTreeTableViewComponent.createTreeTableView(true);
    }

    public VBox createTestPlanTableView() {
        return testPlanTreeTableViewComponent.createTreeTableView(true);
    }

    public VBox createTableView2() {
        return tableViewComponent.createTableView2();
    }

    public VBox createPropertiesView() {
        return tableViewComponent.createPropertiesView();
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setCenter(mainContainer);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Automonius");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
