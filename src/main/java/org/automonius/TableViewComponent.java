package org.automonius;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.annotations.Action;
import org.annotations.InputType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class TableViewComponent {
    private final TableManager tableManager;
    private final List<Method> actions;

    public TableViewComponent(boolean loadProject, TableManager tableManager) {
        this.tableManager = tableManager;
        this.actions = tableManager.getActions();
    }

//    public VBox createTableView2() {
//        TableView<ActionData> tableView2 = tableManager.createNewTableView("TableView2", actions);
//        Button addRowButton = new Button("Add Row");
//        Button deleteRowButton = new Button("Delete Row");
//
//        addRowButton.setOnAction(e -> {
//            ActionData newRow = new ActionData("", "", "", InputType.NONE);
//            tableView2.getItems().add(newRow);
//        });
//
//        deleteRowButton.setOnAction(e -> {
//            ActionData selectedItem = tableView2.getSelectionModel().getSelectedItem();
//            if (selectedItem != null && tableView2.getItems().size() > 1) {
//                tableView2.getItems().remove(selectedItem);
//            }
//        });
//
//        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
//        VBox tableView2Box = new VBox(10, new Label("TableView2"), tableView2, rowButtons);
//        tableView2Box.setPadding(new Insets(10));
//
//        return tableView2Box;
//    }

    public VBox createCommonTableViewLayout(String tableName) {
        TableView<ActionData> tableView = tableManager.getTableViewMap().get(tableName);

        // Create columns for Object and Method with empty cells initially
        ObservableList<String> objectList = FXCollections.observableArrayList(
                actions.stream()
                        .map(action -> action.getAnnotation(Action.class).object().toString())
                        .distinct()
                        .collect(Collectors.toList())
        );

        ObservableList<String> methodList = FXCollections.observableArrayList(
                actions.stream()
                        .map(Method::getName)
                        .distinct()
                        .collect(Collectors.toList())
        );

        TableColumn<ActionData, String> objectColumn = new TableColumn<>("Object");
        objectColumn.setCellFactory(ComboBoxTableCell.forTableColumn(objectList));
        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setEditable(true);

        TableColumn<ActionData, String> methodColumn = new TableColumn<>("Method");
        methodColumn.setCellFactory(ComboBoxTableCell.forTableColumn(methodList));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));
        methodColumn.setEditable(true);

        tableView.setEditable(true);
        tableView.getColumns().addAll(objectColumn, methodColumn);

        Label tableCaption = new Label(tableName);
        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");

        addRowButton.setOnAction(e -> {
            ActionData newRow = new ActionData("", "", "", InputType.NONE);
            tableView.getItems().add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ActionData selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && tableView.getItems().size() > 1) {
                tableView.getItems().remove(selectedItem);
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        VBox tableViewBox = new VBox(10, tableCaption, tableView, rowButtons);
        tableViewBox.setPadding(new Insets(10));

        return tableViewBox;
    }
}