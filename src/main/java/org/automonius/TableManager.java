package org.automonius;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.annotations.Action;
import org.annotations.InputType;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class TableManager {
    private final Map<String, TableView<ActionData>> tableViewMap = new HashMap<>();
    private final Stack<String> undoStack = new Stack<>();
    private final List<Method> actions;

    public TableManager(List<Method> actions) {
        this.actions = actions;
    }

    public List<Method> getActions() {
        return actions;
    }

    public TableView<ActionData> createNewTableView(String name, List<Method> actions) {
        TableView<ActionData> newTableView = new TableView<>();

        List<String> objectList = actions.stream()
                .map(action -> action.getAnnotation(Action.class).object().toString())
                .distinct()
                .collect(Collectors.toList());

        List<String> methodList = actions.stream()
                .map(Method::getName)
                .distinct()
                .collect(Collectors.toList());

        addComboBoxColumn(newTableView, "Object", objectList, "object");
        addComboBoxColumn(newTableView, "Method", methodList, "method");
        addColumn(newTableView, "Description", "description");
        addColumn(newTableView, "Input", "input");

        actions.forEach(action -> {
            Action annotation = action.getAnnotation(Action.class);
            newTableView.getItems().add(new ActionData(annotation.object().toString(), annotation.desc(), action.getName(), annotation.input()));
        });

        tableViewMap.put(name, newTableView);

        return newTableView;
    }

    private void addComboBoxColumn(TableView<ActionData> tableView, String header, List<String> options, String property) {
        TableColumn<ActionData, String> column = new TableColumn<>(header);
        column.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(options)));
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setSortable(false);
        tableView.getColumns().add(column);
    }

    private void addColumn(TableView<ActionData> tableView, String header, String property) {
        TableColumn<ActionData, String> column = new TableColumn<>(header);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setSortable(false);
        tableView.getColumns().add(column);
    }

    public VBox createCommonTableViewLayout(TableView<ActionData> tableView, String caption) {
        Label tableCaption = new Label(caption);

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

    public VBox createTableView1Layout() {
        TableView<ActionData> tableView1 = createNewTableView("TableView1", actions);
        return createCommonTableViewLayout(tableView1, "TableView1");
    }

    public Map<String, TableView<ActionData>> getTableViewMap() {
        return tableViewMap;
    }

    public void deleteTableView(String name) {
        tableViewMap.remove(name);
    }
}
