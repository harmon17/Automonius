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
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.Stack;

public class TableViewComponent {

    private final ObservableList<ObservableList<String>> defaultTableViewData1 = FXCollections.observableArrayList();
    private final ObservableList<ObservableList<String>> defaultTableViewData2 = FXCollections.observableArrayList();
    private final Stack<String> undoStack = new Stack<>();

    public TableViewComponent(boolean loadProject) {
        if (loadProject) {
            ObservableList<String> loadedRow1 = FXCollections.observableArrayList("Loaded Step", "Loaded ObjectName", "Loaded Action", "Loaded Input", "Loaded Condition");
            defaultTableViewData1.add(loadedRow1);
            defaultTableViewData2.add(loadedRow1);
        } else {
            ObservableList<String> defaultRow1 = FXCollections.observableArrayList("New Test Step", "Condition 1", "Remarks 1", "Condition1", "Reference1");
            defaultTableViewData1.add(defaultRow1);
            defaultTableViewData2.add(defaultRow1);
        }
    }

    public VBox createObjectRepositoryView() {
        return new VBox(new Label("Object Repository"), new Label("Placeholder for Object Repository"));
    }

    public VBox createReusableComponentTableView() {
        return createTableViewWithControls("Reusable Component", defaultTableViewData1);
    }

    public VBox createPropertiesView() {
        return new VBox(new Label("Properties"), new Label("Placeholder for Properties"));
    }

    public VBox createTestPlanTableView() {
        return createTableViewWithControls("Test Plan", defaultTableViewData1);
    }

    public VBox createTableView1() {
        return createTableViewWithControls("TableView 1", defaultTableViewData1);
    }

    public VBox createTableView2() {
        return createTableViewWithControls("TableView 2", defaultTableViewData2);
    }

    private VBox createTableViewWithControls(String label, ObservableList<ObservableList<String>> data) {
        TableView<ObservableList<String>> tableView = new TableView<>(data);
        addColumn(tableView, "Steps");
        addColumn(tableView, "ObjectName");
        addColumn(tableView, "Action");
        addColumn(tableView, "Input");
        addColumn(tableView, "Condition");

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ObservableList<String> newRow = FXCollections.observableArrayList("New Test Step", "Condition 1", "Remarks 1", "Condition1", "Reference1");
            for (int i = newRow.size(); i < tableView.getColumns().size(); i++) {
                newRow.add("");
            }
            data.add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ObservableList<String> selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && data.size() > 1) {
                data.remove(selectedItem);
            }
        });

        Button addColumnButton = new Button("Add Column");
        Button deleteColumnButton = new Button("Delete Column");

        addColumnButton.setOnAction(e -> {
            String header = "New Column";
            addColumn(tableView, header);
            for (ObservableList<String> row : data) {
                row.add("");
            }
        });

        deleteColumnButton.setOnAction(e -> {
            if (tableView.getColumns().size() > 5) { // Default columns are 5
                TableColumn<ObservableList<String>, ?> lastColumn = tableView.getColumns().remove(tableView.getColumns().size() - 1);
                for (ObservableList<String> row : data) {
                    row.remove(row.size() - 1);
                }
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        rowButtons.setAlignment(Pos.CENTER_LEFT);
        rowButtons.setPadding(new Insets(5));

        HBox columnButtons = new HBox(10, addColumnButton, deleteColumnButton);
        columnButtons.setAlignment(Pos.CENTER_LEFT);
        columnButtons.setPadding(new Insets(5));

        VBox tableViewBox = new VBox(10, new Label(label), tableView, rowButtons, columnButtons);
        tableViewBox.setPadding(new Insets(10));
        return tableViewBox;
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
