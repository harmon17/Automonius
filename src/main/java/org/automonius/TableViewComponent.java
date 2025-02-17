package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import org.utils.ActionDiscovery;
import org.Commands.SampleCommands;
import org.annotations.Action;
import org.annotations.InputType;
import org.annotations.ObjectType;

public class TableViewComponent {

    private final ObservableList<ObservableList<String>> defaultTableViewData1 = FXCollections.observableArrayList();
    private final ObservableList<ObservableList<String>> defaultTableViewData2 = FXCollections.observableArrayList();
    private final Map<String, TableView<ObservableList<String>>> tableViewMap = new HashMap<>();
    private final TreeTableViewComponent treeTableViewComponent;
    private final VBox mainContainer;
    private final Stack<String> undoStack = new Stack<>();
    private final ObservableList<ActionData> tableView1Data = FXCollections.observableArrayList();

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

        // Discover actions and populate tableView1Data
        List<Method> actions = ActionDiscovery.discoverActions(SampleCommands.class);
        for (Method action : actions) {
            Action annotation = action.getAnnotation(Action.class);
            tableView1Data.add(new ActionData(annotation.object().toString(), annotation.desc(), action.getName(), annotation.input()));
        }
    }

    public ObservableList<ActionData> getTableView1Data() {
        return tableView1Data;
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

    public VBox createCommonTableViewLayout(TableView<ObservableList<String>> tableView, String caption) {
        Label tableCaption = new Label(caption);

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ObservableList<String> newRow = FXCollections.observableArrayList("New Test Step", "Condition 1", "Remarks 1", "Condition1", "Reference1");
            for (int i = newRow.size(); i < tableView.getColumns().size(); i++) {
                newRow.add("");
            }
            tableView.getItems().add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ObservableList<String> selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && tableView.getItems().size() > 1) {
                tableView.getItems().remove(selectedItem);
            }
        });

        Button addColumnButton = new Button("Add Column");
        Button deleteColumnButton = new Button("Delete Column");

        addColumnButton.setOnAction(e -> {
            String header = "New Column";
            addColumn(tableView, header);
            for (ObservableList<String> row : tableView.getItems()) {
                row.add("");
            }
        });

        deleteColumnButton.setOnAction(e -> {
            if (tableView.getColumns().size() > 5) { // Default columns are 5
                TableColumn<ObservableList<String>, ?> lastColumn = tableView.getColumns().remove(tableView.getColumns().size() - 1);
                for (ObservableList<String> row : tableView.getItems()) {
                    row.remove(row.size() - 1);
                }
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        HBox columnButtons = new HBox(10, addColumnButton, deleteColumnButton);
        VBox tableViewBox = new VBox(10, tableCaption, tableView, rowButtons, columnButtons);
        tableViewBox.setPadding(new Insets(10));
        return tableViewBox;
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

    public VBox createObjectRepositoryView() {
        return new VBox(new Label("Object Repository"), new Label("Placeholder for Object Repository"));
    }

    public VBox createReusableComponentTableView() {
        VBox reusableComponentBox = treeTableViewComponent.createTreeTableView(true);

        HBox buttons = new HBox();
        reusableComponentBox.getChildren().add(buttons);

        return reusableComponentBox;
    }

    public VBox createPropertiesView() {
        return new VBox(new Label("Properties"), new Label("Placeholder for Properties"));
    }

    public VBox createTestPlanTableView() {
        VBox testPlanBox = treeTableViewComponent.createTreeTableView(true);

        HBox buttons = new HBox();
        testPlanBox.getChildren().add(buttons);

        return testPlanBox;
    }

    public VBox createTableView1() {
        ComboBox<ObjectType> filterComboBox = new ComboBox<>(FXCollections.observableArrayList(ObjectType.values()));
        filterComboBox.getSelectionModel().selectFirst();

        TableView<ActionData> tableView1 = new TableView<>();
        filterComboBox.setOnAction(e -> {
            ObjectType selectedObjectType = filterComboBox.getSelectionModel().getSelectedItem();
            tableView1.setItems(FXCollections.observableArrayList(tableView1Data.stream()
                    .filter(action -> action.getObject().equals(selectedObjectType.toString()))
                    .collect(Collectors.toList())));
        });

        TableColumn<ActionData, String> objectColumn = new TableColumn<>("Object");
        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));

        TableColumn<ActionData, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<ActionData, String> methodColumn = new TableColumn<>("Method");
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));

        TableColumn<ActionData, InputType> inputColumn = new TableColumn<>("Requires Input");
        inputColumn.setCellValueFactory(new PropertyValueFactory<>("input"));

        tableView1.getColumns().addAll(objectColumn, descriptionColumn, methodColumn, inputColumn);

        VBox tableView1Box = new VBox(10, new Label("TableView1"), filterComboBox, tableView1);
        tableView1Box.setPadding(new Insets(10));
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

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        HBox columnButtons = new HBox(10, addColumnButton, deleteColumnButton);
        VBox tableView2Box = new VBox(10, new Label("TableView2"), tableView2, rowButtons, columnButtons);
        tableView2Box.setPadding(new Insets(10));
        return tableView2Box;
    }
}
