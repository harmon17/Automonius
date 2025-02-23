package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
        System.out.println("Initializing TreeTableViewManager...");
        this.tableManager = tableManager;
        this.mainContainer = mainContainer;
        this.tableViewMap = new HashMap<>();
        this.rootItem = new TreeItem<>("Root");
        this.treeTableView = new TreeTableView<>(rootItem);
        actions = tableManager.getActions();
        setupTreeTableView(loadProject);
    }

    private void setupTreeTableView(boolean loadProject) {
        System.out.println("Setting up TreeTableView...");
        TreeItem<String> mainDirectory = new TreeItem<>("Main");
        TreeItem<String> defaultTableView = new TreeItem<>("TableView1");
        mainDirectory.getChildren().add(defaultTableView);
        rootItem.getChildren().add(mainDirectory);

        if (!tableViewMap.containsKey("TableView1")) {
            TableView<ActionData> defaultTableViewInstance = tableManager.createNewTableView("TableView1", actions);
            tableViewMap.put("TableView1", defaultTableViewInstance);
        }

        if (loadProject) {
            TreeItem<String> existingDirectory = new TreeItem<>("Loaded Directory");
            TreeItem<String> tableViewItem = new TreeItem<>("Loaded TableView");
            existingDirectory.getChildren().add(tableViewItem);
            rootItem.getChildren().add(existingDirectory);

            if (!tableViewMap.containsKey("Loaded TableView")) {
                TableView<ActionData> loadedTableViewInstance = tableManager.createNewTableView("Loaded TableView", actions);
                tableViewMap.put("Loaded TableView", loadedTableViewInstance);
            }
        }

        rootItem.setExpanded(true);
        treeTableView.setShowRoot(false);
        TreeTableColumn<String, String> column = new TreeTableColumn<>("Directory Structure");
        column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));
        treeTableView.getColumns().add(column);

        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            if (oldValue != null && oldValue.equals(newValue)) {
                return;
            }
            System.out.println("Selected item: " + newValue.getValue());
            if (oldValue != null) {
                System.out.println("Previous item: " + oldValue.getValue());
            }
            if (newValue != null) {
                String selectedValue = newValue.getValue();
                TableView<ActionData> tableView = tableViewMap.get(selectedValue);
                if (tableView != null) {
                    if (oldValue != null) {
                        System.out.println("Saving state of: " + oldValue.getValue());
                        tableManager.saveTableViewState(oldValue.getValue(), new ArrayList<>(tableView.getItems()));
                    }
                    System.out.println("Table view found: " + tableView);
                    System.out.println("Restoring state of: " + selectedValue);
                    List<ActionData> restoredData = tableManager.getTableViewState(selectedValue);
                    tableView.setItems(FXCollections.observableArrayList(restoredData));
                    mainContainer.getChildren().clear();
                    mainContainer.getChildren().add(tableManager.createCommonTableViewLayout(tableView, selectedValue));
                } else {
                    System.out.println("No TableView found for: " + selectedValue);
                }
            }
        });

        // Select the default TableView1 on initial load
        treeTableView.getSelectionModel().select(defaultTableView);
        mainContainer.getChildren().clear();
        mainContainer.getChildren().add(tableManager.createTableView1Layout());
    }



    public VBox createTreeTableView(boolean loadProject) {
        System.out.println("Creating TreeTableView...");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(
                new Label("TreeTableView"),
                treeTableView
        );
        return layout;
    }

    public TreeTableView<String> getTreeTableView() {
        return treeTableView;
    }

    public void addMainDirectory() {
        System.out.println("Adding main directory...");
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && isTableView(selectedItem)) {
            showAlert("Cannot add a main directory inside a TableView.");
            return;
        }
        TreeItem<String> newItem = new TreeItem<>("New Main Directory");
        rootItem.getChildren().add(newItem);
        // Add to rootItem so it’s on the same level as Default Directory
    }

    public void addSubDirectory() {
        System.out.println("Adding sub-directory...");
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
        System.out.println("Adding TableView...");
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && isTableView(selectedItem)) {
            showAlert("Cannot add a TableView inside another TableView.");
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
        System.out.println("Deleting item...");
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