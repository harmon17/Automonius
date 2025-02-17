package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeTableViewManager {
    private final TreeItem<String> rootItem;
    private final Map<String, TableView<ActionData>> tableViewMap;
    private final TableManager tableManager;
    private final VBox mainContainer;
    private final TreeTableView<String> treeTableView;
    private final List<Method> actions;

    public TreeTableViewManager(boolean loadProject, TableManager tableManager, VBox mainContainer) {
        this.tableManager = tableManager;
        this.mainContainer = mainContainer;
        this.tableViewMap = new HashMap<>();
        this.rootItem = new TreeItem<>("Root");
        this.treeTableView = new TreeTableView<>(rootItem);

        // Initialize the list for discovered actions
        TableViewComponent tableViewComponent = new TableViewComponent(loadProject);
        actions = tableViewComponent.getActions();

        setupTreeTableView(loadProject);
    }

    private void setupTreeTableView(boolean loadProject) {
        TreeItem<String> defaultDirectory = new TreeItem<>("Default Directory");
        TreeItem<String> defaultTableView = new TreeItem<>("TableView1");
        defaultDirectory.getChildren().add(defaultTableView);
        rootItem.getChildren().add(defaultDirectory);

        TableView<ActionData> defaultTableViewInstance = tableManager.createNewTableView("TableView1", actions);
        tableViewMap.put("TableView1", defaultTableViewInstance);

        if (loadProject) {
            TreeItem<String> existingDirectory = new TreeItem<>("Loaded Directory");
            TreeItem<String> tableViewItem = new TreeItem<>("Loaded TableView");
            existingDirectory.getChildren().add(tableViewItem);
            rootItem.getChildren().add(existingDirectory);

            TableView<ActionData> loadedTableViewInstance = tableManager.createNewTableView("Loaded TableView", actions);
            tableViewMap.put("Loaded TableView", loadedTableViewInstance);
        }

        rootItem.setExpanded(true);
        treeTableView.setShowRoot(false);

        TreeTableColumn<String, String> column = new TreeTableColumn<>("Directory Structure");
        column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));
        treeTableView.getColumns().add(column);

        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String selectedValue = newValue.getValue();
                TableView<ActionData> tableView = tableViewMap.get(selectedValue);
                if (tableView != null) {
                    mainContainer.getChildren().clear();
                    mainContainer.getChildren().add(tableManager.createCommonTableViewLayout(tableView, selectedValue));
                }
            }
        });
    }

    public TreeTableView<String> getTreeTableView() {
        return treeTableView;
    }

    public void addMainDirectory() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && isTableView(selectedItem)) {
            showAlert("Cannot add a main directory inside a TableView.");
            return;
        }
        TreeItem<String> newItem = new TreeItem<>("New Main Directory");
        rootItem.getChildren().add(newItem);  // Add to rootItem so it’s on the same level as Default Directory
    }

    public void addSubDirectory() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem == rootItem) {
            showAlert("No directory selected for sub-directory. Please select a directory first.");
            return;
        }
        if (isTableView(selectedItem)) {
            showAlert("Cannot add sub-directory inside a TableView.");
            return;
        }
        TreeItem<String> newItem = new TreeItem<>("New Sub-Directory");
        selectedItem.getChildren().add(newItem);
        sortChildren(selectedItem);
    }

    public void addTableView() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || isTableView(selectedItem)) {
            showAlert("No directory selected for TableView. Please select a directory or sub-directory first.");
            return;
        }
        String tableViewName = "TableView" + (tableViewMap.size() + 1);
        TreeItem<String> newItem = new TreeItem<>(tableViewName);

        // Add the new TableView as the last TableView item in the selected directory or sub-directory
        selectedItem.getChildren().add(newItem);
        sortChildren(selectedItem);

        // Create and add the new TableView to the map
        TableView<ActionData> newTableViewInstance = tableManager.createNewTableView(tableViewName, actions);
        tableViewMap.put(tableViewName, newTableViewInstance);
    }

    public void deleteItem() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem != rootItem && !isDefaultDirectory(selectedItem)) {
            TreeItem<String> parent = selectedItem.getParent();
            parent.getChildren().remove(selectedItem);

            // Remove the TableView from the map
            tableViewMap.remove(selectedItem.getValue());
        } else {
            showAlert("Cannot delete the Default Directory or Root.");
        }
    }

    private boolean isDefaultDirectory(TreeItem<String> item) {
        return item.getValue().equals("Default Directory");
    }

    private boolean isTableView(TreeItem<String> item) {
        return item.getValue().contains("TableView");
    }

    private void sortChildren(TreeItem<String> parent) {
        parent.getChildren().sort((a, b) -> {
            if (isTableView(a) && !isTableView(b)) {
                return 1;
            } else if (!isTableView(a) && isTableView(b)) {
                return -1;
            }
            return 0;
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Invalid Action");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
