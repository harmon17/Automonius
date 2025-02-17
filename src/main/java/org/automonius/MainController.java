package org.automonius;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.annotations.ObjectType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class MainController extends Application {

    private final TreeTableViewComponent testPlanTreeTableViewComponent;
    private final TreeTableViewComponent reusableComponentTreeTableViewComponent;
    private final TableViewComponent tableViewComponent;
    private final VBox mainContainer;
    private TableView<ActionData> tableView1;
    private final TableManager tableManager;

    public MainController(boolean loadProject, TableManager tableManager) {
        this.tableManager = tableManager;

        tableViewComponent = new TableViewComponent(loadProject, tableManager);

        mainContainer = new VBox();

        testPlanTreeTableViewComponent = new TreeTableViewComponent(loadProject, tableManager, mainContainer);
        reusableComponentTreeTableViewComponent = new TreeTableViewComponent(loadProject, tableManager, mainContainer);

        // Comment out the initialization of TableView1
        // initializeTableView1();
    }

    private void initializeTableView1() {
        mainContainer.getChildren().clear();  // Clear previous content
        VBox tableView1Box = tableManager.createTableView1Layout();
        tableView1 = findTableView(tableView1Box);

        if (tableView1 == null) {
            throw new IndexOutOfBoundsException("TableView not found in VBox");
        }

        // Add TableView1 to mainContainer
        mainContainer.getChildren().add(tableView1Box);
    }

    private TableView<ActionData> findTableView(VBox vbox) {
        for (Node node : vbox.getChildren()) {
            if (node instanceof TableView<?>) {
                TableView<?> tableView = (TableView<?>) node;
                if (tableView.getItems().isEmpty() || tableView.getItems().get(0) instanceof ActionData) {
                    return (TableView<ActionData>) tableView;
                }
            }
        }
        return null;
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
