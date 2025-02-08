package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class TableViewComponent {

    private final ObservableList<ObservableList<String>> defaultTableViewData1 = FXCollections.observableArrayList();
    private final ObservableList<ObservableList<String>> defaultTableViewData2 = FXCollections.observableArrayList();
    private final Map<String, TableView<ObservableList<String>>> tableViewMap = new HashMap<>();
    private final TreeTableViewComponent treeTableViewComponent;
    private final VBox mainContainer;
    private final Stack<String> undoStack = new Stack<>();

    public TableViewComponent(boolean loadProject) {
        mainContainer = new VBox(); // Initialize the main container
        treeTableViewComponent = new TreeTableViewComponent(loadProject, this, mainContainer); // Initialize the TreeTableViewComponent

        if (loadProject) {
            // Load existing project data into the tables
            ObservableList<String> loadedRow1 = FXCollections.observableArrayList("Loaded Step", "Loaded ObjectName", "Loaded Action", "Loaded Input", "Loaded Condition");
            defaultTableViewData1.add(loadedRow1);
            defaultTableViewData2.add(loadedRow1);
        } else {
            // Default setup for new project
            ObservableList<String> defaultRow1 = FXCollections.observableArrayList("New Test Step", "Condition 1", "Remarks 1", "Condition1", "Reference1");
            defaultTableViewData1.add(defaultRow1);
            defaultTableViewData2.add(defaultRow1);
        }
    }

    public VBox createObjectRepositoryView() {
        return new VBox(new Label("Object Repository"), new Label("Placeholder for Object Repository"));
    }

    public VBox createReusableComponentTableView() {
        VBox reusableComponentBox = treeTableViewComponent.createTreeTableView();
        TreeTableView<String> reusableComponentTreeTableView = treeTableViewComponent.getTreeTableView();

        Button addDirectoryButton = new Button("Add Directory");
        Button addTableViewButton = new Button("Add TableView");
        Button deleteButton = new Button("Delete");

        addDirectoryButton.setOnAction(e -> {
            TreeItem<String> newDirectory = new TreeItem<>("New Directory");
            reusableComponentTreeTableView.getRoot().getChildren().add(newDirectory);
            tableViewMap.put(newDirectory.getValue(), createNewTableView(newDirectory.getValue()));

        });

        addTableViewButton.setOnAction(e -> {
            TreeItem<String> selectedItem = reusableComponentTreeTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                TreeItem<String> newTableView = new TreeItem<>("New TableView");
                selectedItem.getChildren().add(newTableView);
                tableViewMap.put(newTableView.getValue(), createNewTableView(newTableView.getValue()));

            }
        });

        deleteButton.setOnAction(e -> {
            TreeItem<String> selectedItem = reusableComponentTreeTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getParent() != null && !selectedItem.getValue().equals("Default Directory")) {
                selectedItem.getParent().getChildren().remove(selectedItem);
                tableViewMap.remove(selectedItem.getValue());
            }
        });

        HBox buttons = new HBox(addDirectoryButton, addTableViewButton, deleteButton);
        reusableComponentBox.getChildren().add(buttons);
        return reusableComponentBox;
    }

    public VBox createPropertiesView() {
        return new VBox(new Label("Properties"), new Label("Placeholder for Properties"));
    }

    public VBox createTestPlanTableView() {
        VBox testPlanBox = treeTableViewComponent.createTreeTableView();
        TreeTableView<String> testPlanTreeTableView = treeTableViewComponent.getTreeTableView();

        Button addDirectoryButton = new Button("Add Directory");
        Button addTableViewButton = new Button("Add TableView");
        Button deleteButton = new Button("Delete");

        addDirectoryButton.setOnAction(e -> {
            TreeItem<String> newDirectory = new TreeItem<>("New Directory");
            testPlanTreeTableView.getRoot().getChildren().add(newDirectory);
            tableViewMap.put(newDirectory.getValue(), createNewTableView(newDirectory.getValue()));

        });

        addTableViewButton.setOnAction(e -> {
            TreeItem<String> selectedItem = testPlanTreeTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                TreeItem<String> newTableView = new TreeItem<>("New TableView");
                selectedItem.getChildren().add(newTableView);
                tableViewMap.put(newTableView.getValue(), createNewTableView(newTableView.getValue()));

            }
        });

        deleteButton.setOnAction(e -> {
            TreeItem<String> selectedItem = testPlanTreeTableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getParent() != null && !selectedItem.getValue().equals("Default Directory")) {
                selectedItem.getParent().getChildren().remove(selectedItem);
                tableViewMap.remove(selectedItem.getValue());
            }
        }); 

        HBox buttons = new HBox(addDirectoryButton, addTableViewButton, deleteButton);
        testPlanBox.getChildren().add(buttons);
        return testPlanBox;
    }

    public TableView<ObservableList<String>> createNewTableView(String name) {
        TableView<ObservableList<String>> newTableView = new TableView<>();
        addColumn(newTableView, "Steps");
        addColumn(newTableView, "ObjectName");
        addColumn(newTableView, "Action");
        addColumn(newTableView, "Input");
        addColumn(newTableView, "Condition");

        ObservableList<String> defaultRow = FXCollections.observableArrayList(name + " Step", name + " ObjectName", name + " Action", name + " Input", name + " Condition");
        newTableView.getItems().add(defaultRow);

        return newTableView;
    }


    public VBox createTableView1() {
        TableView<ObservableList<String>> tableView1 = new TableView<>(defaultTableViewData1);
        addColumn(tableView1, "Steps");
        addColumn(tableView1, "ObjectName");
        addColumn(tableView1, "Action");
        addColumn(tableView1, "Input");
        addColumn(tableView1, "Condition");

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ObservableList<String> newRow = FXCollections.observableArrayList("New Test Step", "Condition 1", "Remarks 1", "Condition1", "Reference1");
            for (int i = newRow.size(); i < tableView1.getColumns().size(); i++) {
                newRow.add("");
            }
            defaultTableViewData1.add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ObservableList<String> selectedItem = tableView1.getSelectionModel().getSelectedItem();
            if (selectedItem != null && defaultTableViewData1.size() > 1) {
                defaultTableViewData1.remove(selectedItem);
            }
        });

        Button addColumnButton = new Button("Add Column");
        Button deleteColumnButton = new Button("Delete Column");

        addColumnButton.setOnAction(e -> {
            String header = "New Column";
            addColumn(tableView1, header);
            for (ObservableList<String> row : defaultTableViewData1) {
                row.add("");
            }
        });

        deleteColumnButton.setOnAction(e -> {
            if (tableView1.getColumns().size() > 5) { // Default columns are 5
                TableColumn<ObservableList<String>, ?> lastColumn = tableView1.getColumns().remove(tableView1.getColumns().size() - 1);
                for (ObservableList<String> row : defaultTableViewData1) {
                    row.remove(row.size() - 1);
                }
            }
        });

        HBox rowButtons = new HBox(addRowButton, deleteRowButton);
        HBox columnButtons = new HBox(addColumnButton, deleteColumnButton);
        VBox tableView1Box = new VBox(new Label("TableView1"), tableView1, rowButtons, columnButtons);
        return tableView1Box;
    }

    public VBox createTableView2() {
        TableView<ObservableList<String>> tableView2 = new TableView<>(defaultTableViewData2);
        addColumn(tableView2, "Steps");
        addColumn(tableView2, "Iteration");
        addColumn(tableView2, "Sub-Iteration");
        addColumn(tableView2, "Column");

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ObservableList<String> newRow = FXCollections.observableArrayList("New Test Step", "Condition 1", "Remarks 1", "Condition1", "Reference1");
            for (int i = newRow.size(); i < tableView2.getColumns().size(); i++) {
                newRow.add("");
            }
            defaultTableViewData2.add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ObservableList<String> selectedItem = tableView2.getSelectionModel().getSelectedItem();
            if (selectedItem != null && defaultTableViewData2.size() > 1) {
                defaultTableViewData2.remove(selectedItem);
            }
        });

        Button addColumnButton = new Button("Add Column");
        Button deleteColumnButton = new Button("Delete Column");

        addColumnButton.setOnAction(e -> {
            String header = "New Column";
            addColumn(tableView2, header);
            for (ObservableList<String> row : defaultTableViewData2) {
                row.add("");
            }
        });

        deleteColumnButton.setOnAction(e -> {
            if (tableView2.getColumns().size() > 4) { // Default columns are 4
                TableColumn<ObservableList<String>, ?> lastColumn = tableView2.getColumns().remove(tableView2.getColumns().size() - 1);
                for (ObservableList<String> row : defaultTableViewData2) {
                    row.remove(row.size() - 1);
                }
            }
        });

        HBox rowButtons = new HBox(addRowButton, deleteRowButton);
        HBox columnButtons = new HBox(addColumnButton, deleteColumnButton);
        VBox tableView2Box = new VBox(new Label("TableView2"), tableView2, rowButtons, columnButtons);
        return tableView2Box;
    }

    private void addColumn(TableView<ObservableList<String>> tableView, String header) {
        TableColumn<ObservableList<String>, String> column = new TableColumn<>(header);
        column.setCellValueFactory(param -> {
            int index = tableView.getColumns().indexOf(column);
            return new SimpleStringProperty(param.getValue().size() > index ? param.getValue().get(index) : "");
        });
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> event.getRowValue().set(tableView.getColumns().indexOf(column), event.getNewValue()));
        column.setSortable(false);

        // Add double-click functionality for renaming columns
        column.setGraphic(new Label(header));
        column.getGraphic().setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TextField textField = new TextField(header);
                textField.setOnAction(e -> {
                    column.setText(textField.getText());
                    column.setGraphic(new Label(textField.getText()));
                });
                column.setGraphic(textField);
            }
        });

        // Add editor popup on mouse over with context-aware commenting and undo
        column.setCellFactory(col -> {
            TableCell<ObservableList<String>, String> cell = new TableCell<ObservableList<String>, String>() {
                private final Button editButton = new Button("Edit");

                {
                    editButton.setOnAction(evt -> {
                        Stage editorStage = new Stage();
                        TextArea textArea = new TextArea(getItem());
                        textArea.setPrefHeight(400); // Set initial preferred height
                        textArea.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                            if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.SLASH) {
                                String selectedText = textArea.getSelectedText();
                                String fullText = textArea.getText();
                                if (!selectedText.isEmpty()) {
                                    if (fullText.contains(selectedText)) {
                                        String modifiedText;
                                        if (fullText.trim().startsWith("{") || fullText.trim().startsWith("[")) {
                                            modifiedText = toggleCommentJson(selectedText, fullText);
                                        } else if (fullText.trim().startsWith("<")) {
                                            modifiedText = toggleCommentXml(selectedText, fullText);
                                        } else {
                                            modifiedText = fullText;
                                        }
                                        undoStack.push(textArea.getText());
                                        textArea.setText(modifiedText);
                                    }
                                }
                                keyEvent.consume();
                            } else if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.Z) {
                                if (!undoStack.isEmpty()) {
                                    textArea.setText(undoStack.pop());
                                    keyEvent.consume();
                                }
                            }
                        });
                        VBox vbox = new VBox(textArea);
                        Scene scene = new Scene(vbox);
                        editorStage.setScene(scene);
                        editorStage.setMinHeight(500); // Minimum height of editor window
                        editorStage.setMinWidth(600); // Minimum width of editor window
                        editorStage.show();
                        editorStage.setOnHiding(h -> {
                            int rowIndex = getIndex();
                            int colIndex = tableView.getColumns().indexOf(column);
                            tableView.getItems().get(rowIndex).set(colIndex, textArea.getText());
                            tableView.refresh();
                        });
                    });

                    setOnMouseEntered(e -> {
                        if (getItem() != null && !getItem().isEmpty()) {
                            setGraphic(editButton);
                        }
                    });
                    setOnMouseExited(e -> setGraphic(null));
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                    }
                }
            };
            return cell;
        });

        tableView.getColumns().add(column);
    }

    private String toggleCommentJson(String selectedText, String fullText) {
        String[] lines = selectedText.split("\n");
        StringBuilder commentedContent = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith("//")) {
                commentedContent.append(line.replaceFirst("//", "").trim()).append("\n");
            } else {
                commentedContent.append("// ").append(line).append("\n");
            }
        }
        return fullText.replace(selectedText, commentedContent.toString());
    }

    private String toggleCommentXml(String selectedText, String fullText) {
        String[] lines = selectedText.split("\n");
        StringBuilder commentedContent = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith("<!--") && line.trim().endsWith("-->")) {
                commentedContent.append(line.replaceFirst("<!--", "").replaceFirst("-->", "").trim()).append("\n");
            } else {
                commentedContent.append("<!-- ").append(line).append(" -->\n");
            }
        }
        return fullText.replace(selectedText, commentedContent.toString());
    }
}