package org.automonius;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.annotations.ObjectType;

import java.util.Optional;
import java.util.stream.Collectors;

public class MainController extends Application {

    private final TreeTableViewComponent testPlanTreeTableViewComponent;
    private final TreeTableViewComponent reusableComponentTreeTableViewComponent;
    private final TableViewComponent tableViewComponent;
    private final VBox mainContainer;
    private TableView<ActionData> tableView1;
    private ComboBox<ObjectType> objectTypeComboBox;

    public MainController(boolean loadProject) {
        // Initialize the TableManager
        TableManager tableManager = new TableManager();

        // Initialize the TableViewComponent
        tableViewComponent = new TableViewComponent(loadProject);

        mainContainer = new VBox();

        // Initialize the TreeTableViewComponent with the initialized TableManager
        testPlanTreeTableViewComponent = new TreeTableViewComponent(loadProject, tableManager, mainContainer);
        reusableComponentTreeTableViewComponent = new TreeTableViewComponent(loadProject, tableManager, mainContainer);

        // Add TableView1 to the main container initially
        initializeTableView1();
    }

    private void initializeTableView1() {
        objectTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(ObjectType.values()));
        objectTypeComboBox.getSelectionModel().selectFirst();
        objectTypeComboBox.setOnAction(event -> updateTableView1());

        VBox tableView1Box = tableViewComponent.createTableView1();
        tableView1 = findTableView(tableView1Box);

        if (tableView1 == null) {
            throw new IndexOutOfBoundsException("TableView not found in VBox");
        }

        // Add ComboBox and TableView1 to mainContainer
        mainContainer.getChildren().addAll(objectTypeComboBox, tableView1Box);

        updateTableView1();
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

    private void updateTableView1() {
        ObjectType selectedObjectType = objectTypeComboBox.getSelectionModel().getSelectedItem();
        ObservableList<ActionData> filteredData = FXCollections.observableArrayList(
                tableViewComponent.getTableView1Data().stream()
                        .filter(action -> action.getObject().equals(selectedObjectType.toString()))
                        .collect(Collectors.toList())
        );
        tableView1.setItems(filteredData);
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
