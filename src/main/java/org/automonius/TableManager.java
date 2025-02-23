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
    private final Map<String, List<ActionData>> tableDataMap = new HashMap<>();
    private VBox tableView1Layout;
    private final Map<String, Boolean> initializationMap = new HashMap<>();

    public TableManager(List<Method> actions) {
        this.actions = actions;
        System.out.println("TableManager instance created");
        initializeInitialData();
    }

    // Initialize the data for all tables at the start
    private void initializeInitialData() {
        tableDataMap.put("TableView1", new ArrayList<>());
        tableDataMap.put("Loaded TableView", new ArrayList<>());
        initializationMap.put("TableView1", true);
        initializationMap.put("Loaded TableView", true);
    }

    public void saveTableViewState(String tableName, List<ActionData> data) {
        if (!initializationMap.getOrDefault(tableName, false)) {
            if (data.stream().anyMatch(actionData -> !actionData.getObject().isEmpty() || !actionData.getMethod().isEmpty() ||
                    !actionData.getDescription().isEmpty() || !actionData.getInput().toString().isEmpty())) {
                System.out.println("Saving state for table: " + tableName + ", data: " + data);
                tableDataMap.put(tableName, data);
                System.out.println("Current tableDataMap: " + tableDataMap);
            } else {
                System.out.println("Skipping state save for table: " + tableName + " as it has no meaningful data.");
            }
        } else {
            System.out.println("Initialization in progress for table: " + tableName + ", state saving skipped.");
        }
    }

    public List<ActionData> getTableViewState(String tableName) {
        return tableDataMap.getOrDefault(tableName, new ArrayList<>());
    }

    public List<Method> getActions() {
        return actions;
    }

    public TableView<ActionData> createNewTableView(String name, List<Method> actions) {
        System.out.println("Creating new table view with name: " + name);
        TableView<ActionData> newTableView = new TableView<>();

        List<String> objectList = actions.stream()
                .map(action -> action.getAnnotation(Action.class).object().toString())
                .distinct()
                .collect(Collectors.toList());

        if (!columnExists(newTableView, "Object")) {
            TableColumn<ActionData, String> objectColumn = new TableColumn<>("Object");
            objectColumn.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(objectList)));
            objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
            objectColumn.setEditable(true);
            objectColumn.setOnEditCommit(event -> {
                System.out.println("onEditCommit: " + event.getNewValue());
                String newObject = event.getNewValue();
                ActionData actionData = event.getRowValue();
                actionData.setObject(newObject);
                filterMethodsDropdown(newTableView, actionData, newObject);
                updateTableDataMap(name, actionData);
            });
            newTableView.getColumns().add(objectColumn);
        }

        if (!columnExists(newTableView, "Method")) {
            TableColumn<ActionData, String> methodColumn = new TableColumn<>("Method");
            methodColumn.setCellFactory(column -> {
                ComboBoxTableCell<ActionData, String> cell = new ComboBoxTableCell<>();
                cell.setEditable(true);
                cell.setOnMouseClicked(event -> cell.startEdit());
                return cell;
            });
            methodColumn.setCellValueFactory(new PropertyValueFactory<>("method"));
            methodColumn.setOnEditCommit(event -> {
                ActionData actionData = event.getRowValue();
                actionData.setMethod(event.getNewValue());
                updateTableDataMap(name, actionData);
            });
            newTableView.getColumns().add(methodColumn);
        }

        if (!columnExists(newTableView, "Description")) {
            TableColumn<ActionData, String> descriptionColumn = new TableColumn<>("Description");
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            descriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            descriptionColumn.setEditable(true);
            descriptionColumn.setOnEditCommit(event -> {
                ActionData actionData = event.getRowValue();
                actionData.setDescription(event.getNewValue());
                updateTableDataMap(name, actionData);
            });
            newTableView.getColumns().add(descriptionColumn);
        }

        if (!columnExists(newTableView, "Input")) {
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
                        setText(item);
                        setGraphic(editButton);
                    }
                }
            });
            newTableView.getColumns().add(inputColumn);
        }

        newTableView.setEditable(true);

        List<ActionData> initialData = getTableViewState(name);
        if (initialData.isEmpty()) {
            initialData.add(new ActionData("", "", "", InputType.NONE));
        }
        System.out.println("Setting initial data for " + name + ": " + initialData);
        newTableView.setItems(FXCollections.observableArrayList(initialData));
        tableViewMap.put(name, newTableView);
        return newTableView;
    }

    private boolean columnExists(TableView<ActionData> tableView, String columnHeader) {
        return tableView.getColumns().stream()
                .anyMatch(column -> column.getText().equals(columnHeader));
    }

    public void updateTableDataMap(String tableName, ActionData actionData) {
        List<ActionData> dataList = tableDataMap.get(tableName);
        if (dataList == null) {
            dataList = new ArrayList<>();
            tableDataMap.put(tableName, dataList);
        }
        int index = dataList.indexOf(actionData);
        if (index != -1) {
            dataList.set(index, actionData);
        } else {
            dataList.add(actionData);
        }
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

        String inputText = actionData.getInput() != InputType.NONE ? actionData.getInput().toString() : actionData.getAdditionalProperty("Input");
        TextArea inputArea = new TextArea(inputText);

        inputArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SLASH) {
                String selectedText = inputArea.getSelectedText();
                InputType currentInputType = determineInputType(inputArea.getText());
                String commentedText = toggleComment(selectedText, currentInputType);
                int start = inputArea.getSelection().getStart();
                int end = inputArea.getSelection().getEnd();
                inputArea.replaceText(start, end, commentedText);
                inputArea.selectRange(start, start + commentedText.length());
                event.consume();
            }
        });

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            String editedText = inputArea.getText();
            actionData.setAdditionalProperty("Input", editedText);
            tableView.refresh();
            popupStage.close();
        });

        VBox popupVBox = new VBox(10, new Label("Edit Input (XML/JSON)"), inputArea, saveButton);
        popupVBox.setPadding(new Insets(10));
        Scene popupScene = new Scene(popupVBox, 400, 300);
        popupStage.setScene(popupScene);
        popupStage.showAndWait();

    }


    private String toggleComment(String selectedText, InputType inputType) {
        if (inputType == InputType.XML && selectedText.startsWith("<!--") && selectedText.endsWith("-->")) {
            return selectedText.substring(4, selectedText.length() - 3).trim();
        } else if (inputType == InputType.JSON && selectedText.startsWith("/") && selectedText.endsWith("/")) {
            return selectedText.substring(2, selectedText.length() - 2).trim();
        } else {
            if (inputType == InputType.XML) {
                return "<!--" + selectedText + "-->";
            } else if (inputType == InputType.JSON) {
                return "/" + selectedText + "/";
            }
        }
        return selectedText;
    }

    private InputType determineInputType(String text) {
        text = text.trim();
        if (text.startsWith("<") && text.endsWith(">")) {
            return InputType.XML;
        } else if (text.startsWith("{") || text.startsWith("[")) {
            return InputType.JSON;
        } else {
            return InputType.NONE;
        }
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
        if (tableView1Layout == null) {
            System.out.println("Creating table view 1 layout");
            TableView<ActionData> tableView1 = tableViewMap.get("TableView1");
            if (tableView1 == null) {
                tableView1 = createNewTableView("TableView1", actions);
                tableViewMap.put("TableView1", tableView1);
            }
            tableView1Layout = createCommonTableViewLayout(tableView1, "TableView1");
        }
        return tableView1Layout;
    }

    public Map<String, TableView<ActionData>> getTableViewMap() {
        return tableViewMap;
    }

    public void deleteTableView(String name) {
        tableViewMap.remove(name);
    }
}
