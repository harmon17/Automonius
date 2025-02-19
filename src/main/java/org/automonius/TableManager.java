package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.annotations.Action;
import org.annotations.InputType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class TableManager {
    private final Map<String, TableView<ActionData>> tableViewMap = new HashMap<>();
    private final Stack<String> undoStack = new Stack<>();
    private final List<Method> actions;
    private final List<TableColumn<ActionData, ?>> defaultColumns = new ArrayList<>();
    private final List<TableColumn<ActionData, ?>> additionalColumns = new ArrayList<>();

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

        TableColumn<ActionData, String> objectColumn = new TableColumn<>("Object");
        objectColumn.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(objectList)));
        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setOnEditCommit(event -> {
            ActionData actionData = event.getRowValue();
            String newObject = event.getNewValue();
            actionData.setObject(newObject);

            // Update methods dropdown based on selected object
            filterMethodsDropdown(newTableView, actionData, newObject);
        });
        objectColumn.setEditable(true);
        defaultColumns.add(objectColumn);

        TableColumn<ActionData, String> methodColumn = new TableColumn<>("Method");
        methodColumn.setCellFactory(column -> {
            ComboBoxTableCell<ActionData, String> cell = new ComboBoxTableCell<>();
            cell.setEditable(true);
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    String selectedObject = cell.getTableView().getItems().get(cell.getIndex()).getObject();
                    List<String> relevantMethods = actions.stream()
                            .filter(action -> action.getAnnotation(Action.class).object().toString().equals(selectedObject))
                            .map(Method::getName)
                            .distinct()
                            .collect(Collectors.toList());
                    cell.getItems().setAll(relevantMethods);
                    cell.startEdit();  // Open dropdown on single click
                }
            });
            return cell;
        });
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));
        methodColumn.setOnEditCommit(event -> event.getRowValue().setMethod(event.getNewValue()));
        methodColumn.setEditable(true);
        defaultColumns.add(methodColumn);

        TableColumn<ActionData, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        descriptionColumn.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        descriptionColumn.setEditable(true);
        defaultColumns.add(descriptionColumn);

        TableColumn<ActionData, String> inputColumn = new TableColumn<>("Input");
        inputColumn.setCellValueFactory(cellData -> {
            String input = cellData.getValue().getInput().toString();
            if (InputType.NONE.toString().equals(input)) {
                input = cellData.getValue().getAdditionalProperty("Input");
            }
            return new SimpleStringProperty(input);
        });
        inputColumn.setCellFactory(column -> new TableCell<ActionData, String>() {
            final Button editButton = new Button("Edit");

            {
                editButton.setOnAction(event -> {
                    ActionData actionData = getTableView().getItems().get(getIndex());
                    openEditPopup(actionData, getTableView());
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setText(item); // Ensure the input value is displayed in the cell
                    setGraphic(editButton);
                }
            }
        });
        defaultColumns.add(inputColumn);

        newTableView.setEditable(true);
        newTableView.getColumns().addAll(objectColumn, methodColumn, descriptionColumn, inputColumn);

        // Add a single initial row
        ActionData initialRow = new ActionData("", "", "", InputType.NONE);
        newTableView.getItems().add(initialRow);

        tableViewMap.put(name, newTableView);
        return newTableView;
    }

    private void addComboBoxColumn(TableView<ActionData> tableView, String header, List<String> options, String property) {
        TableColumn<ActionData, String> column = new TableColumn<>(header);
        column.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(options)));
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setSortable(false);
        additionalColumns.add(column);
        tableView.getColumns().add(column);
    }

    private void addColumn(TableView<ActionData> tableView, String header) {
        TableColumn<ActionData, String> column = new TableColumn<>(header);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdditionalProperty(header)));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> event.getRowValue().setAdditionalProperty(header, event.getNewValue()));
        column.setSortable(false);
        addColumnContextMenu(tableView, column);
        additionalColumns.add(column);
        tableView.getColumns().add(column);
    }

    private void addColumnContextMenu(TableView<ActionData> tableView, TableColumn<ActionData, ?> column) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Column");
        deleteItem.setOnAction(event -> tableView.getColumns().remove(column));
        contextMenu.getItems().add(deleteItem);

        MenuItem renameItem = new MenuItem("Rename Column");
        renameItem.setOnAction(event -> openRenamePopup(column));
        contextMenu.getItems().add(renameItem);

        column.setContextMenu(contextMenu);
    }

    private void openRenamePopup(TableColumn<ActionData, ?> column) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Rename Column");

        TextField renameField = new TextField(column.getText());

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            column.setText(renameField.getText());
            popupStage.close();
        });

        VBox popupVBox = new VBox(10, new Label("Rename Column"), renameField, saveButton);
        popupVBox.setPadding(new Insets(10));
        Scene popupScene = new Scene(popupVBox, 300, 150);
        popupStage.setScene(popupScene);
        popupStage.showAndWait();
    }

    private void filterMethodsDropdown(TableView<ActionData> tableView, ActionData actionData, String newObject) {
        List<String> relevantMethods = actions.stream()
                .filter(action -> action.getAnnotation(Action.class).object().toString().equals(newObject))
                .map(Method::getName)
                .distinct()
                .collect(Collectors.toList());

        for (ActionData item : tableView.getItems()) {
            if (item == actionData) {
                item.setMethod(""); // Clear the method selection
            }
        }

        TableColumn<ActionData, String> methodColumn = (TableColumn<ActionData, String>) tableView.getColumns().stream()
                .filter(column -> column.getText().equals("Method"))
                .findFirst()
                .orElse(null);

        if (methodColumn != null) {
            methodColumn.setCellFactory(column -> {
                ComboBoxTableCell<ActionData, String> cell = new ComboBoxTableCell<>(FXCollections.observableArrayList(relevantMethods));
                cell.setEditable(true);
                cell.setOnMouseClicked(event -> cell.startEdit());  // Open dropdown on single click
                return cell;
            });
        }
    }

    private void openEditPopup(ActionData actionData, TableView<ActionData> tableView) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Edit Input");

        TextArea inputArea = new TextArea(actionData.getInput().toString());  // Ensure InputType is converted to String

        // Add key event handler to handle Ctrl + /
        inputArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SLASH) {
                String text = inputArea.getText();
                if (actionData.getInput() == InputType.XML) {
                    if (text.startsWith("<!--") && text.endsWith("-->")) {
                        text = text.substring(4, text.length() - 3).trim();
                    } else {
                        text = "<!--" + text + "-->";
                    }
                } else if (actionData.getInput() == InputType.JSON) {
                    if (text.startsWith("/*") && text.endsWith("*/")) {
                        text = text.substring(2, text.length() - 2).trim();
                    } else {
                        text = "/*" + text + "*/";
                    }
                }
                inputArea.setText(text);
                event.consume();
            }
        });

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            String inputText = inputArea.getText();
            try {
                actionData.setInput(InputType.valueOf(inputText.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle invalid enum constant by treating it as a custom string
                actionData.setInput(InputType.NONE);
                actionData.setAdditionalProperty("Input", inputText);
            }
            popupStage.close();
            tableView.refresh();  // Refresh the table to display the updated input value
        });

        VBox popupVBox = new VBox(10, new Label("Edit Input (XML/JSON)"), inputArea, saveButton);
        popupVBox.setPadding(new Insets(10));
        Scene popupScene = new Scene(popupVBox, 400, 300);
        popupStage.setScene(popupScene);
        popupStage.showAndWait();
    }

    public VBox createCommonTableViewLayout(TableView<ActionData> tableView, String caption) {
        Label tableCaption = new Label(caption);

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        Button addColumnButton = new Button("Add Column");
        Button deleteColumnButton = new Button("Delete Column");
        Button printValuesButton = new Button("Print Values");

        addRowButton.setOnAction(e -> {
            ActionData newRow = new ActionData("", "", "", InputType.NONE);
            tableView.getItems().add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ActionData selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                tableView.getItems().remove(selectedItem);
            }
        });

        addColumnButton.setOnAction(e -> {
            String columnName = "NewColumn" + (additionalColumns.size() + 1);
            addColumn(tableView, columnName);
        });

        deleteColumnButton.setOnAction(e -> {
            if (!additionalColumns.isEmpty()) {
                TableColumn<ActionData, ?> lastAddedColumn = additionalColumns.remove(additionalColumns.size() - 1);
                tableView.getColumns().remove(lastAddedColumn);
            }
        });

        printValuesButton.setOnAction(e -> {
            for (ActionData actionData : tableView.getItems()) {
                System.out.println("Object: " + actionData.getObject());
                System.out.println("Method: " + actionData.getMethod());
                System.out.println("Description: " + actionData.getDescription());
                String input = actionData.getInput().toString();
                if (InputType.NONE.toString().equals(input)) {
                    input = actionData.getAdditionalProperty("Input");
                }
                System.out.println("Input: " + input);
                for (Map.Entry<String, String> entry : actionData.getAdditionalProperties().entrySet()) {
                    if (!"Input".equals(entry.getKey())) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                }
                System.out.println("-----");
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton, addColumnButton, deleteColumnButton, printValuesButton);
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
