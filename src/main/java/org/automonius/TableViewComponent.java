package org.automonius;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.Commands.SampleCommands;
import org.annotations.Action;
import org.annotations.InputType;
import org.utils.ActionDiscovery;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class TableViewComponent {
    private final TableManager tableManager;
    private final VBox mainContainer;
    private final ObservableList<ActionData> tableView1Data = FXCollections.observableArrayList();
    private final List<Method> actions;
    private final List<String> objectList;
    private final List<String> methodList;

    public TableViewComponent(boolean loadProject) {
        tableManager = new TableManager();
        mainContainer = new VBox();

        // Discover actions and populate tableView1Data
        actions = ActionDiscovery.discoverActions(SampleCommands.class);
        objectList = actions.stream().map(action -> action.getAnnotation(Action.class).object().toString()).distinct().collect(Collectors.toList());
        methodList = actions.stream().map(Method::getName).distinct().collect(Collectors.toList());

        for (Method action : actions) {
            Action annotation = action.getAnnotation(Action.class);
            tableView1Data.add(new ActionData(annotation.object().toString(), annotation.desc(), action.getName(), annotation.input()));
        }
    }

    public ObservableList<ActionData> getTableView1Data() {
        return tableView1Data;
    }

    public List<String> getObjectList() {
        return objectList;
    }

    public List<String> getMethodList() {
        return methodList;
    }

    // New method to provide actions
    public List<Method> getActions() {
        return actions;
    }

    public VBox createCommonTableViewLayout(String tableName) {
        TableView<ActionData> tableView = tableManager.getTableViewMap().get(tableName);
        return tableManager.createCommonTableViewLayout(tableView, tableName);
    }

    public TableView<ActionData> createNewTableView(String name) {
        return tableManager.createNewTableView(name, actions);
    }

    public VBox createTableView1() {
        TableView<ActionData> tableView1 = tableManager.createNewTableView("TableView1", actions);

        VBox tableView1Box = new VBox(10, new Label("TableView1"), tableView1);
        tableView1Box.setPadding(new Insets(10));
        return tableView1Box;
    }

    public VBox createTableView2() {
        TableView<ActionData> tableView2 = tableManager.createNewTableView("TableView2", actions);

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ActionData newRow = new ActionData("", "", "", InputType.NONE); // Correct InputType should be set here
            tableView2.getItems().add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ActionData selectedItem = tableView2.getSelectionModel().getSelectedItem();
            if (selectedItem != null && tableView2.getItems().size() > 1) {
                tableView2.getItems().remove(selectedItem);
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        VBox tableView2Box = new VBox(10, new Label("TableView2"), tableView2, rowButtons);
        tableView2Box.setPadding(new Insets(10));
        return tableView2Box;
    }

    public VBox createPropertiesView() {
        TableView<ActionData> propertiesView = tableManager.createNewTableView("PropertiesView", actions);

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ActionData newRow = new ActionData("", "", "", InputType.NONE); // Correct InputType should be set here
            propertiesView.getItems().add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ActionData selectedItem = propertiesView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && propertiesView.getItems().size() > 1) {
                propertiesView.getItems().remove(selectedItem);
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        VBox propertiesBox = new VBox(10, new Label("PropertiesView"), propertiesView, rowButtons);
        propertiesBox.setPadding(new Insets(10));
        return propertiesBox;
    }

    public TableManager getTableManager() {
        return tableManager;
    }
}
