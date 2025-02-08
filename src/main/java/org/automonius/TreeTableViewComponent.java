package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class TreeTableViewComponent {

    private TreeItem<String> rootItem;
    private TreeTableView<String> treeTableView;
    private final Map<String, TableView<ObservableList<String>>> tableViewMap = new HashMap<>();
    private final TableViewComponent tableViewComponent;
    private final VBox mainContainer;

    public TreeTableViewComponent(boolean loadProject, TableViewComponent tableViewComponent, VBox mainContainer) {
        this.tableViewComponent = tableViewComponent;
        this.mainContainer = mainContainer;

        rootItem = new TreeItem<>("Root");
        TreeItem<String> defaultDirectory = new TreeItem<>("Default Directory");
        TreeItem<String> defaultTableView = new TreeItem<>("TableView1");
        defaultDirectory.getChildren().add(defaultTableView);
        rootItem.getChildren().add(defaultDirectory);

        // Add the default TableView to the map
        TableView<ObservableList<String>> defaultTableViewInstance = tableViewComponent.createNewTableView("TableView1");
        tableViewMap.put("TableView1", defaultTableViewInstance);

        if (loadProject) {
            // Load existing project data into the tree
            TreeItem<String> existingDirectory = new TreeItem<>("Loaded Directory");
            TreeItem<String> tableViewItem = new TreeItem<>("Loaded TableView");
            existingDirectory.getChildren().add(tableViewItem);
            rootItem.getChildren().add(existingDirectory);

            // Add the loaded TableView to the map
            TableView<ObservableList<String>> loadedTableViewInstance = tableViewComponent.createNewTableView("Loaded TableView");
            tableViewMap.put("Loaded TableView", loadedTableViewInstance);
        }

        rootItem.setExpanded(true);
        treeTableView = new TreeTableView<>(rootItem);
        treeTableView.setShowRoot(false);  // Hide the root item itself

        TreeTableColumn<String, String> column = new TreeTableColumn<>("Directory Structure");
        column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue()));
        treeTableView.getColumns().add(column);

        treeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String selectedValue = newValue.getValue();
                TableView<ObservableList<String>> tableView = tableViewMap.get(selectedValue);

                if (tableView != null) {
                    mainContainer.getChildren().clear();
                    mainContainer.getChildren().add(tableViewComponent.createCommonTableViewLayout(tableView, selectedValue));
                }
            }
        });
    }

    public VBox createTreeTableView(boolean isForDirectory) {
        VBox vBox = new VBox();

        if (isForDirectory) {
            // Directory buttons, only add them for Test Plan and Reusable Component views, not for TableView1
            Button addDirectoryButton = new Button("Add Main Directory");
            Button addSubDirectoryButton = new Button("Add Sub-Directory");
            Button addTableViewButton = new Button("Add TableView");
            Button deleteButton = new Button("Delete");

            addDirectoryButton.setOnAction(e -> addMainDirectory());
            addSubDirectoryButton.setOnAction(e -> addSubDirectory());
            addTableViewButton.setOnAction(e -> addTableView());
            deleteButton.setOnAction(e -> deleteItem());

            vBox.getChildren().addAll(treeTableView, addDirectoryButton, addSubDirectoryButton, addTableViewButton, deleteButton);
        } else {
            vBox.getChildren().add(treeTableView);
        }

        return vBox;
    }

    public TreeTableView<String> getTreeTableView() {
        return treeTableView;
    }

    private void addMainDirectory() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && isTableView(selectedItem)) {
            showAlert("Invalid Action", "Cannot add a main directory inside a TableView.");
            return;
        }
        TreeItem<String> newItem = new TreeItem<>("New Main Directory");
        rootItem.getChildren().add(newItem);  // Add to rootItem so it’s on the same level as Default Directory
    }

    private void addSubDirectory() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem == rootItem) {
            showAlert("Invalid Action", "No directory selected for sub-directory. Please select a directory first.");
            return;
        }
        if (isTableView(selectedItem)) {
            showAlert("Invalid Action", "Cannot add sub-directory inside a TableView.");
            return;
        }
        TreeItem<String> newItem = new TreeItem<>("New Sub-Directory");
        selectedItem.getChildren().add(newItem);
        sortChildren(selectedItem);
    }

    private void addTableView() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || isTableView(selectedItem)) {
            showAlert("Invalid Action", "No directory selected for TableView. Please select a directory or sub-directory first.");
            return;
        }
        String tableViewName = "TableView" + (tableViewMap.size() + 1);
        TreeItem<String> newItem = new TreeItem<>(tableViewName);

        // Add the new TableView as the last TableView item in the selected directory or sub-directory
        selectedItem.getChildren().add(newItem);
        sortChildren(selectedItem);

        // Create and add the new TableView to the map
        TableView<ObservableList<String>> newTableViewInstance = tableViewComponent.createNewTableView(tableViewName);
        tableViewMap.put(tableViewName, newTableViewInstance);
    }

    private void deleteItem() {
        TreeItem<String> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem != rootItem && !isDefaultDirectory(selectedItem)) {
            TreeItem<String> parent = selectedItem.getParent();
            parent.getChildren().remove(selectedItem);

            // Remove the TableView from the map
            tableViewMap.remove(selectedItem.getValue());
        } else {
            showAlert("Invalid Action", "Cannot delete the Default Directory or Root.");
        }
    }

    private boolean isDefaultDirectory(TreeItem<String> item) {
        // Check if the item is the Default Directory
        return item.getValue().equals("Default Directory");
    }

    private boolean isTableView(TreeItem<String> item) {
        // Basic check to determine if an item is a TableView
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
