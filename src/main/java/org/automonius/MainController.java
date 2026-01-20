package org.automonius;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.automonius.Actions.ActionLibrary;
import org.automonius.Annotations.ActionMeta;
import org.automonius.Model.Variable;
import org.automonius.Model.VariableTreeController;
import org.automonius.exec.TestCase;
import org.automonius.exec.TestExecutor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {
    @FXML
    private TreeView<TestNode> treeView;
    @FXML
    private TableView<TestStep> tableView;
    @FXML
    private TableColumn<TestStep, String> itemColumn;
    @FXML
    private TableColumn<TestStep, String> actionColumn;
    @FXML
    private TableColumn<TestStep, String> objectColumn;
    @FXML
    private TableColumn<TestStep, String> inputColumn;
    @FXML
    private Button newSuiteBtn;
    @FXML
    private Button subSuiteBtn;
    @FXML
    private Button testScenarioBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Label testExplorerLabel;
    @FXML
    private TableColumn<TestStep, String> descriptionColumn;
    @FXML
    private VariableTreeController variableTreeController;
    private final Map<String, List<TestStep>> scenarioSteps = new HashMap<>();
    // Cache of extra column names per scenario
    private final Map<String, List<String>> scenarioColumns = new HashMap<>();
    @FXML
    private final DataFormatter formatter = new DataFormatter();
    // Holds mapping of object → list of actions
    private Map<String, List<String>> actionsByObject;
    private final Node rootIcon = makeIcon("/icons/bank.png", 16, 16);
    private final Node suiteIcon = makeIcon("/icons/MainSuite.png", 16, 16);
    private final Node subSuiteIcon = makeIcon("/icons/SubSuite.png", 16, 16);
    private final Node testScenarioIcon = makeIcon("/icons/TestSuite.png", 16, 16);


    @FXML
    public void initialize() {
        actionsByObject = TestExecutor.getActionsByObject(org.automonius.Actions.ActionLibrary.class);
        // --- Test Explorer TreeView setup ---
        TreeItem<TestNode> explorerRoot = new TreeItem<>(new TestNode("Directory Structure", NodeType.ROOT));
        explorerRoot.setExpanded(true);
        treeView.setRoot(explorerRoot);

        // Root node setup
        TreeItem<TestNode> root = new TreeItem<>(new TestNode("Directory Structure", NodeType.ROOT));
        root.setExpanded(true);

        TreeItem<TestNode> defaultSuite = new TreeItem<>(new TestNode("Suite 1", NodeType.SUITE));
        defaultSuite.setExpanded(true);

        TreeItem<TestNode> defaultScenario = new TreeItem<>(new TestNode("Test Suite", NodeType.TEST_SCENARIO));
        defaultSuite.getChildren().

                add(defaultScenario);

        String key = makeKey(defaultScenario);

        // ✅ Use ObservableList directly
        ObservableList<TestStep> steps = FXCollections.observableArrayList();

        // Optionally seed with a sample TestCase
        TestCase sample = TestExecutor.getTestByAction(org.automonius.Actions.ActionLibrary.class, "checkFileReadable");

        if (sample != null) {
            steps.add(new TestStep(sample));  // clean and correct
        }

        // Add a blank row for user editing
        steps.add(new

                TestStep("", "", "", ""));

        // Bind to table and scenario map
        tableView.setItems(steps);
        scenarioSteps.put(key, steps);

        // Tree setup
        root.getChildren().

                add(defaultSuite);
        treeView.setRoot(root);

// ✅ Force initial selection on the TestScenario
        Platform.runLater(() ->

        {
            treeView.getSelectionModel().select(defaultScenario);
            treeView.getFocusModel().focus(treeView.getRow(defaultScenario));
        });


        // Setup columns
        tableView.setEditable(true);
        objectColumn.setEditable(true);
        actionColumn.setEditable(true);

        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        descriptionColumn.setOnEditCommit(event -> event.getRowValue().

                setDescription(event.getNewValue()));

        inputColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        inputColumn.setOnEditCommit(event -> event.getRowValue().

                setInput(event.getNewValue()));


        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        inputColumn.setCellValueFactory(new PropertyValueFactory<>("input"));


        itemColumn.setCellValueFactory(cellData -> new

                ReadOnlyStringWrapper(String.valueOf(tableView.getItems().

                indexOf(cellData.getValue()) + 1)));

        itemColumn.setEditable(false);

        rebuildArgumentColumns();
//One
        itemColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER;"); // center horizontally
            }
        });

        Map<String, List<String>> actionsByObject = TestExecutor.getActionsByObject(org.automonius.Actions.ActionLibrary.class);
        ObservableList<String> objectOptions = FXCollections.observableArrayList(actionsByObject.keySet());
        // Item column (simple text cell)
        itemColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER;"); // center horizontally
            }
        });

// Object column (ComboBox bound to objectProperty)

        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setEditable(true);
        objectColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            private final ComboBox<String> combo = new ComboBox<>(objectOptions);

            {
                setGraphic(combo);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();

                combo.valueProperty().unbindBidirectional(step.objectProperty());
                combo.valueProperty().bindBidirectional(step.objectProperty());

                combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        List<String> methods = actionsByObject.getOrDefault(newVal, List.of());
                        if (!methods.isEmpty()) {
                            step.setAction(methods.get(0));
                            rebuildArgumentColumns();
                            updateColumnHeadersForAction();
                        }
                    }
                });

                setGraphic(combo);
            }
        });
// Action column (ComboBox bound to actionProperty)
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setEditable(true);
        actionColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            private final ComboBox<String> combo = new ComboBox<>();

            {
                setGraphic(combo);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();

                // Update items based on current object
                combo.setItems(FXCollections.observableArrayList(actionsByObject.getOrDefault(step.getObject(), List.of())));

                combo.valueProperty().unbindBidirectional(step.actionProperty());
                combo.valueProperty().bindBidirectional(step.actionProperty());

                combo.setOnAction(event -> {
                    rebuildArgumentColumns();
                    updateColumnHeadersForAction();
                });

                setGraphic(combo);
            }
        });

// Optional: handle direct edit commits
        actionColumn.setOnEditCommit(event -> {
            TestStep step = event.getRowValue();
            step.setAction(event.getNewValue());
            rebuildArgumentColumns();
            updateColumnHeadersForAction();
        });
// Input column (drag & drop + click-to-edit dialog)
        inputColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            {
                // Drag & Drop support
                setOnDragOver(event -> {
                    if (event.getGestureSource() != this && event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.COPY);
                        setStyle("-fx-background-color: lightgreen;");
                    }
                    event.consume();
                });

                setOnDragExited(event -> setStyle(""));

                setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasString()) {
                        String data = db.getString();
                        String[] parts = data.split("::", 2);
                        String varName = parts[0];
                        String varValue = parts.length > 1 ? parts[1] : "";

                        TestStep step = getTableView().getItems().get(getIndex());
                        step.setInput(varName);

                        if (!varValue.isEmpty()) {
                            String[] values = varValue.split("\\|");
                            for (Method m : ActionLibrary.class.getDeclaredMethods()) {
                                if (m.getName().equals(step.getAction()) && m.isAnnotationPresent(ActionMeta.class)) {
                                    ActionMeta meta = m.getAnnotation(ActionMeta.class);
                                    String[] inputs = meta.inputs();
                                    for (int i = 0; i < inputs.length && i < values.length; i++) {
                                        step.setExtra(inputs[i], values[i].trim());
                                    }
                                    break;
                                }
                            }
                        }

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Variable Added");
                        alert.setHeaderText(null);
                        alert.setContentText("You added value: " + varValue + " into input.");
                        alert.initModality(Modality.NONE);
                        alert.show();
                    }
                    event.setDropCompleted(true);
                    event.consume();
                });

                // Click-to-edit dialog
                setOnMouseClicked(event -> {
                    if (!isEmpty()) {
                        TestStep step = getTableView().getItems().get(getIndex());

                        TextArea editor = new TextArea(step.getInput());
                        editor.setPrefWidth(600);
                        editor.setPrefHeight(400);
                        editor.setWrapText(true);
                        editor.setStyle("-fx-font-family: Consolas; -fx-font-size: 12;");

                        Dialog<String> dialog = new Dialog<>();
                        dialog.setTitle("Edit Input");
                        dialog.setResizable(true);

                        DialogPane pane = dialog.getDialogPane();
                        pane.setContent(editor);
                        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                        dialog.setResultConverter(button -> {
                            if (button == ButtonType.OK) {
                                return editor.getText();
                            }
                            return null;
                        });

                        dialog.showAndWait().ifPresent(result -> {
                            if (result != null) {
                                step.setInput(result);
                            }
                        });
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(item == null ? "" : item);
                }
            }
        });


        testExplorerLabel.setGraphic(

                makeIcon("/icons/explorer.png", 50, 50));
        testExplorerLabel.setContentDisplay(ContentDisplay.LEFT); // icon left of text


        // Attach icons to buttons later if i still want to persist
//        newSuiteBtn.setGraphic(makeIcon("/icons/MainSuite.png", 20, 20));
//        subSuiteBtn.setGraphic(makeIcon("/icons/SubSuite.png", 20, 20));
//        testScenarioBtn.setGraphic(makeIcon("/icons/TestSuite.png", 20, 20));
//        deleteBtn.setGraphic(makeIcon("/icons/delete.png", 20, 20));

        treeView.setCellFactory(tv ->

        {
            TreeCell<TestNode> cell = new TreeCell<>() {
                @Override
                protected void updateItem(TestNode item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getName());

                        // ✅ Use cached icons instead of recreating each time
                        switch (item.getType()) {
                            case ROOT -> setGraphic(rootIcon);
                            case SUITE -> setGraphic(suiteIcon);
                            case SUB_SUITE -> setGraphic(subSuiteIcon);
                            case TEST_SCENARIO -> setGraphic(testScenarioIcon);
                            default -> setGraphic(null);
                        }
                    }

                    // Optional: reset style only if you need to clear custom styles
                    setStyle("");
                }
            };


            // ✅ Double‑click to rename
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    handleRenameNode(cell.getTreeItem());
                }
            });


            // ✅ Context menu with Rename, Copy, Paste (preserving node type)
            ContextMenu menu = new ContextMenu();

            MenuItem renameItem = new MenuItem("Rename…");
            renameItem.setOnAction(e -> {
                if (cell.getItem() != null) {
                    handleRenameNode(cell.getTreeItem());
                }
            });

            MenuItem copyItem = new MenuItem("Copy");
            copyItem.setOnAction(e -> {
                if (cell.getItem() != null) {
                    NodeType type = cell.getItem().getType();
                    String name = cell.getItem().getName();

                    ClipboardContent content = new ClipboardContent();

                    if (type == NodeType.TEST_SCENARIO) {
                        String scenarioKey = makeKey(cell.getTreeItem());
                        List<TestStep> scenarioData = scenarioSteps.getOrDefault(scenarioKey, List.of());
                        List<String> columns = scenarioColumns.getOrDefault(scenarioKey, List.of());

                        JsonObject json = new JsonObject();
                        json.addProperty("type", type.name());
                        json.addProperty("name", name);

                        JsonArray stepArray = new JsonArray();
                        for (TestStep step : scenarioData) {
                            JsonObject s = new JsonObject();
                            s.addProperty("item", step.getItem());
                            s.addProperty("action", step.getAction());
                            s.addProperty("object", step.getObject());
                            s.addProperty("input", step.getInput());
                            s.addProperty("description", step.getDescription());

                            JsonObject extras = new JsonObject();
                            step.getExtras().forEach((k, v) -> extras.addProperty(k, v.get()));
                            s.add("extras", extras);

                            stepArray.add(s);
                        }
                        json.add("steps", stepArray);

                        JsonArray colArray = new JsonArray();
                        columns.forEach(colArray::add);
                        json.add("columns", colArray);

                        content.putString(json.toString());
                    } else {
                        content.putString(type + "::" + name);
                    }

                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    clipboard.setContent(content);
                }
            });


            MenuItem pasteItem = new MenuItem("Paste");
            pasteItem.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                if (clipboard.hasString()) {
                    String data = clipboard.getString();
                    TreeItem<TestNode> target = cell.getTreeItem();

                    if (target != null) {
                        try {
                            // Try JSON payload (TestScenario copy)
                            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                            NodeType copiedType = NodeType.valueOf(json.get("type").getAsString());
                            String pastedName = json.get("name").getAsString();

                            if (isValidDrop(new TreeItem<>(new TestNode(pastedName, copiedType)), target)) {
                                TreeItem<TestNode> newNode = new TreeItem<>(new TestNode(pastedName, copiedType));
                                target.getChildren().add(newNode);

                                if (copiedType == NodeType.TEST_SCENARIO) {
                                    String scenarioKey = makeKey(newNode);

                                    // Rebuild steps
                                    List<TestStep> rebuiltSteps = new ArrayList<>();
                                    for (JsonElement el : json.getAsJsonArray("steps")) {
                                        JsonObject s = el.getAsJsonObject();
                                        TestStep step = new TestStep(s.get("item").getAsString(), s.get("action").getAsString(), s.get("object").getAsString(), s.get("input").getAsString());
                                        step.setDescription(s.get("description").getAsString());

                                        JsonObject extras = s.getAsJsonObject("extras");
                                        for (String k : extras.keySet()) {
                                            step.setExtra(k, extras.get(k).getAsString());
                                        }
                                        rebuiltSteps.add(step);
                                    }
                                    scenarioSteps.put(scenarioKey, rebuiltSteps);

                                    // Rebuild columns
                                    List<String> rebuiltColumns = new ArrayList<>();
                                    for (JsonElement col : json.getAsJsonArray("columns")) {
                                        rebuiltColumns.add(col.getAsString());
                                    }
                                    scenarioColumns.put(scenarioKey, rebuiltColumns);
                                }
                            } else {
                                showError(copiedType + " can only be pasted inside a valid parent.");
                            }

                        } catch (Exception ex) {
                            // Fallback: simple type::name string
                            String[] parts = data.split("::", 2);
                            if (parts.length == 2) {
                                NodeType copiedType = NodeType.valueOf(parts[0]);
                                String pastedName = parts[1];

                                if (isValidDrop(new TreeItem<>(new TestNode(pastedName, copiedType)), target)) {
                                    TreeItem<TestNode> newNode = new TreeItem<>(new TestNode(pastedName, copiedType));
                                    target.getChildren().add(newNode);

                                    if (copiedType == NodeType.TEST_SCENARIO) {
                                        scenarioSteps.put(makeKey(newNode), new ArrayList<>(List.of(new TestStep("", "", "", ""))));
                                        scenarioColumns.put(makeKey(newNode), new ArrayList<>());
                                    }
                                } else {
                                    showError(copiedType + " can only be pasted inside a valid parent.");
                                }
                            }
                        }
                    }
                }
            });


            menu.getItems().addAll(renameItem, copyItem, pasteItem);
            cell.setContextMenu(menu);

            // Drag detected
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem().getName()); // use TestNode name
                    db.setContent(content);
                    event.consume();
                }
            });

            // Drag over
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && !cell.isEmpty()) {
                    TreeItem<TestNode> draggedItem = findItem(treeView.getRoot(), ((TreeCell<TestNode>) event.getGestureSource()).getItem().getName());
                    TreeItem<TestNode> targetItem = cell.getTreeItem();

                    if (isValidDrop(draggedItem, targetItem)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        cell.setStyle("-fx-background-color: lightgreen;"); // valid target
                    } else {
                        cell.setStyle("-fx-background-color: lightcoral;"); // invalid target
                    }
                }
                event.consume();
            });

            // Drag exited → remove highlight
            cell.setOnDragExited(event -> cell.setStyle(""));

            // Drop
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    TreeItem<TestNode> draggedItem = findItem(treeView.getRoot(), db.getString());
                    TreeItem<TestNode> targetItem = cell.getTreeItem();

                    if (draggedItem != null && targetItem != null && isValidDrop(draggedItem, targetItem)) {
                        // Capture old key BEFORE moving
                        String oldKey = makeKey(draggedItem);

                        // Move the node once
                        draggedItem.getParent().getChildren().remove(draggedItem);
                        targetItem.getChildren().add(draggedItem);

                        // Compute new key AFTER move
                        String newKey = makeKey(draggedItem);

                        // Move scenario data to new key
                        if (scenarioSteps.containsKey(oldKey)) {
                            scenarioSteps.put(newKey, scenarioSteps.remove(oldKey));
                        }
                        if (scenarioColumns.containsKey(oldKey)) {
                            scenarioColumns.put(newKey, scenarioColumns.remove(oldKey));
                        }

                        success = true;
                    } else {
                        showError("Invalid drop target for " + (draggedItem != null ? draggedItem.getValue().getName() : ""));
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return cell;
        });


        // Listener: when TestScenario selected, load its steps
        treeView.getSelectionModel().

                selectedItemProperty().

                addListener((obs, oldVal, newVal) ->

                {
                    if (oldVal != null && oldVal.getValue().getType() == NodeType.TEST_SCENARIO) {
                        saveTestScenario(oldVal);
                    }
                    if (newVal != null && newVal.getValue().getType() == NodeType.TEST_SCENARIO) {
                        loadTestScenario(newVal);
                    } else {
                        tableView.getItems().clear();
                    }
                });


        // Ctrl+S shortcut
        Platform.runLater(() ->

        {
            treeView.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
                            if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
                                saveTestScenario(selected);
                            }
                            saveProject(); // export included
                            event.consume();
                        }

                    });
                }
            });

        });

        tableView.getColumns().

                setAll(itemColumn, objectColumn, actionColumn, descriptionColumn, inputColumn);
        tableView.setFixedCellSize(25);

        // 👉 Enable drag-and-drop row reordering
        tableView.setRowFactory(tv -> {
            TableRow<TestStep> row = new TableRow<>();

            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(Integer.toString(row.getIndex())); // index as string
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    String data = db.getString();
                    if (data.matches("\\d+")) { // ✅ only parse if numeric
                        int draggedIndex = Integer.parseInt(data);
                        if (row.getIndex() != draggedIndex) {
                            event.acceptTransferModes(TransferMode.MOVE);
                            row.setStyle("-fx-background-color: lightgreen;");
                        }
                    }
                }
                event.consume();
            });

            row.setOnDragExited(event -> row.setStyle(""));

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString()) {
                    String data = db.getString();

                    if (data.matches("\\d+")) {
                        int draggedIndex = Integer.parseInt(data);
                        if (draggedIndex >= 0 && draggedIndex < tableView.getItems().size()) {
                            TestStep draggedStep = tableView.getItems().remove(draggedIndex);

                            int dropIndex = row.isEmpty() ? tableView.getItems().size() : row.getIndex();
                            if (draggedIndex < dropIndex) {
                                dropIndex--;
                            }

                            tableView.getItems().add(dropIndex, draggedStep);
                            tableView.getSelectionModel().select(dropIndex);
                            success = true;
                        }
                    }
                }

                event.setDropCompleted(success);
                event.consume();
            });

            return row;
        });


        // 👉 Add TableView context menu here
        ContextMenu tableMenu = new ContextMenu();

        MenuItem copyRowItem = new MenuItem("Copy Row");
        copyRowItem.setOnAction(e ->

        {
            TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
            if (selectedStep != null) {
                ClipboardContent content = new ClipboardContent();
                StringBuilder sb = new StringBuilder();
                sb.append(selectedStep.getItem()).append("|").append(selectedStep.getAction()).append("|").append(selectedStep.getObject()).append("|").append(selectedStep.getInput()).append("|").append(selectedStep.getDescription());
                selectedStep.getExtras().forEach((k, v) -> sb.append("|").append(k).append("=").append(v.get()));
                content.putString(sb.toString());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });

        MenuItem pasteRowItem = new MenuItem("Paste Row");
        pasteRowItem.setOnAction(e ->

        {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                String[] parts = clipboard.getString().split("\\|");
                if (parts.length >= 5) {
                    TestStep newStep = new TestStep(parts[0], parts[1], parts[2], parts[3]);
                    newStep.setDescription(parts[4]);
                    for (int i = 5; i < parts.length; i++) {
                        String[] kv = parts[i].split("=", 2);
                        if (kv.length == 2) newStep.setExtra(kv[0], kv[1]);
                    }
                    int index = tableView.getSelectionModel().getSelectedIndex();
                    if (index >= 0) {
                        tableView.getItems().add(index + 1, newStep);
                    } else {
                        tableView.getItems().add(newStep);
                    }
                }
            }
        });

        tableMenu.getItems().

                addAll(copyRowItem, pasteRowItem);
        tableView.setContextMenu(tableMenu);

    }

    @FXML
    private void handleNewSuite(ActionEvent event) {
        TreeItem<TestNode> root = treeView.getRoot();
        if (root != null) {
            // ✅ Removed numbering sequence, default name is just "Suite"
            TreeItem<TestNode> newSuite = new TreeItem<>(new TestNode("Suite", NodeType.SUITE));
            newSuite.setExpanded(true);
            root.getChildren().add(newSuite);
        }
    }

    @FXML
    private void handleNewSubSuite(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.SUITE) {
            // ✅ Removed numbering sequence, default name is just "Sub-Suite"
            TreeItem<TestNode> subSuite = new TreeItem<>(new TestNode("Sub-Suite", NodeType.SUB_SUITE));
            subSuite.setExpanded(true);
            selected.getChildren().add(subSuite);
        } else {
            showError("Sub-Suite can only be added inside a Suite.");
        }
    }


    @FXML
    private void handleNewTestScenario(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && (selected.getValue().getType() == NodeType.SUITE || selected.getValue().getType() == NodeType.SUB_SUITE)) {

            TextInputDialog dialog = new TextInputDialog("TestSuite");
            dialog.setTitle("New TestSuite");
            dialog.setHeaderText("Create a new TestSuite");
            dialog.setContentText("Enter TestSuite name:");

            dialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    TreeItem<TestNode> testScenario = new TreeItem<>(new TestNode(name.trim(), NodeType.TEST_SCENARIO));
                    selected.getChildren().add(testScenario);

                    // ✅ Seed with one truly blank row (nulls, not "")
                    String key = makeKey(testScenario);
                    List<TestStep> steps = new ArrayList<>();
                    steps.add(new TestStep(null, null, null, null)); // <-- important change
                    scenarioSteps.put(key, steps);

                    // ✅ If this new scenario is selected immediately, show it
                    if (treeView.getSelectionModel().getSelectedItem() == testScenario) {
                        tableView.setItems(FXCollections.observableArrayList(steps));
                    }
                } else {
                    showError("Name cannot be empty.");
                }
            });
        } else {
            showError("TestScenario can only be added inside a Suite or Sub-Suite.");
        }
    }


    @FXML
    private void handleDelete(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getParent() != null) {
            selected.getParent().getChildren().remove(selected);
        }
    }


    @FXML
    private void handleSave(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
            saveTestScenario(selected);
        }
        // Always export project when saving
        saveProject();
    }


    private void saveTestScenario(TreeItem<TestNode> scenario) {
        String key = makeKey(scenario);
        List<TestStep> copy = new ArrayList<>();
        int rowNum = 1;
        for (TestStep step : tableView.getItems()) {
            TestStep clone = new TestStep(String.valueOf(rowNum++), step.getAction(), step.getObject(), step.getInput());
            clone.setDescription(step.getDescription());

            // Copy extras
            for (Map.Entry<String, SimpleStringProperty> entry : step.getExtras().entrySet()) {
                clone.setExtra(entry.getKey(), entry.getValue().get());
            }

            copy.add(clone);
        }
        scenarioSteps.put(key, copy);
    }

    private void loadTestScenario(TreeItem<TestNode> scenario) {
        // Reset to base columns
        tableView.getColumns().setAll(itemColumn, objectColumn, actionColumn, descriptionColumn, inputColumn);

        String key = makeKey(scenario);
        List<TestStep> steps = scenarioSteps.getOrDefault(key, new ArrayList<>());

        // Clone steps
        ObservableList<TestStep> clonedSteps = FXCollections.observableArrayList();
        for (TestStep step : steps) {
            TestStep clone = new TestStep(step.getItem(), step.getAction(), step.getObject(), step.getInput());
            clone.setDescription(step.getDescription());
            for (Map.Entry<String, SimpleStringProperty> entry : step.getExtras().entrySet()) {
                clone.setExtra(entry.getKey(), entry.getValue().get());
            }
            clonedSteps.add(clone);
        }
        tableView.setItems(clonedSteps);

        // --- Re‑apply factories for all base columns ---

        // Item column (read‑only index)
        itemColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(tableView.getItems().indexOf(cellData.getValue()) + 1)));
        itemColumn.setEditable(false);

        // Description column (editable text)
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        descriptionColumn.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));

        // Input column (custom dialog editor)
        inputColumn.setCellValueFactory(new PropertyValueFactory<>("input"));
        // keep your custom dialog cell factory from initialize()

        // Object column (editable via ComboBox)
        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setEditable(true);
        objectColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            private final ComboBox<String> combo = new ComboBox<>();

            {
                setGraphic(combo);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();

                // ✅ Update items list once per refresh
                combo.setItems(FXCollections.observableArrayList(actionsByObject.keySet()));

                // ✅ Rebind to this step’s object property
                combo.valueProperty().unbindBidirectional(step.objectProperty());
                combo.valueProperty().bindBidirectional(step.objectProperty());

                combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        List<String> methods = actionsByObject.getOrDefault(newVal, List.of());
                        if (!methods.isEmpty()) {
                            step.setAction(methods.get(0));
                            rebuildArgumentColumns();
                            updateColumnHeadersForAction();
                        }
                    }
                });

                setGraphic(combo);
            }
        });


        // Action column (editable via ComboBox)
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setEditable(true);
        actionColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            private final ComboBox<String> combo = new ComboBox<>();

            {
                setGraphic(combo);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();

                // ✅ Update items based on current object
                combo.setItems(FXCollections.observableArrayList(actionsByObject.getOrDefault(step.getObject(), List.of())));

                // ✅ Rebind to this step’s action property
                combo.valueProperty().unbindBidirectional(step.actionProperty());
                combo.valueProperty().bindBidirectional(step.actionProperty());

                combo.setOnAction(event -> {
                    rebuildArgumentColumns();
                    updateColumnHeadersForAction();
                });

                setGraphic(combo);
            }
        });


        // --- Rebuild ArgN columns consistently ---
        rebuildArgumentColumns();

        // Merge scenario extras into ArgN placeholders
        List<String> extras = scenarioColumns.getOrDefault(key, List.of());
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            TableColumn<TestStep, ?> baseCol = tableView.getColumns().get(i);
            if ("Dynamic".equals(baseCol.getUserData())) {
                @SuppressWarnings("unchecked") TableColumn<TestStep, String> col = (TableColumn<TestStep, String>) baseCol;
                final int colIndex = i - 5; // offset after base columns
                final String extraName = (colIndex >= 0 && colIndex < extras.size()) ? extras.get(colIndex) : null;

                col.setCellFactory(tc -> new TableCell<TestStep, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || getTableRow() == null) {
                            setText(null);
                            return;
                        }
                        TestStep step = getTableRow().getItem();
                        String[] inputs = getInputsForAction(step.getAction());
                        if (colIndex >= 0 && colIndex < inputs.length) {
                            String inputName = inputs[colIndex];
                            if (item == null || item.isBlank()) {
                                setText(extraName != null ? extraName : inputName);
                                setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                            } else {
                                setText(item);
                                setStyle("");
                            }
                        } else {
                            setText("");
                            setStyle("-fx-text-fill: lightgray;");
                        }
                    }
                });
            }
        }
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Action");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private TreeItem<TestNode> findItem(TreeItem<TestNode> root, String name) {
        if (root.getValue().getName().equals(name)) return root;
        for (TreeItem<TestNode> child : root.getChildren()) {
            TreeItem<TestNode> result = findItem(child, name);
            if (result != null) return result;
        }
        return null;
    }

    private boolean isValidDrop(TreeItem<TestNode> dragged, TreeItem<TestNode> target) {
        if (dragged == null || target == null) return false;

        NodeType dType = dragged.getValue().getType();
        NodeType tType = target.getValue().getType();

        if (dType == NodeType.SUITE && tType == NodeType.ROOT) return true;
        if (dType == NodeType.SUB_SUITE && tType == NodeType.SUITE) return true;
        if (dType == NodeType.TEST_SCENARIO && (tType == NodeType.SUITE || tType == NodeType.SUB_SUITE)) return true;

        return false;
    }

    private ImageView makeIcon(String path, double width, double height) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(path)));
        icon.setFitWidth(width);
        icon.setFitHeight(height);
        icon.setPreserveRatio(true);
        return icon;
    }

    @FXML
    private void handleAddRow(ActionEvent event) {
        int nextIndex = tableView.getItems().size() + 1;
        tableView.getItems().add(new TestStep("", "", "", ""));
    }

    @FXML
    private void handleDeleteRow(ActionEvent event) {
        if (!tableView.getItems().isEmpty()) {
            tableView.getItems().remove(tableView.getItems().size() - 1);
        }
    }

    @FXML
    private void handleAddColumn(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().getType() != NodeType.TEST_SCENARIO) {
            showError("Extra columns can only be added inside a TestScenario.");
            return;
        }

        String colName = "Extra" + (tableView.getColumns().size() - 4);
        addColumnForScenario(selected, colName);
    }

    private void addColumnForScenario(TreeItem<TestNode> scenario, String colName) {
        String key = makeKey(scenario);

        // 🔑 Prevent duplicates
        boolean exists = tableView.getColumns().stream().anyMatch(c -> c.getText().equals(colName));
        if (exists) return;

        TableColumn<TestStep, String> extraColumn = new TableColumn<>(colName);
        extraColumn.setCellValueFactory(cellData -> cellData.getValue().getExtraProperty(colName));
        extraColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        extraColumn.setOnEditCommit(evt -> evt.getRowValue().setExtra(colName, evt.getNewValue()));

        MenuItem renameCol = new MenuItem("Rename…");
        renameCol.setOnAction(e -> handleRenameColumn(extraColumn, scenario));
        extraColumn.setContextMenu(new ContextMenu(renameCol));

        tableView.getColumns().add(extraColumn);

        scenarioColumns.computeIfAbsent(key, k -> new ArrayList<>()).add(colName);
    }

    private void clearArgumentColumns() {
        tableView.getColumns().removeIf(c -> scenarioColumns.values().stream().anyMatch(cols -> cols.contains(c.getText())));
    }


    @FXML
    private void handleDeleteColumn(ActionEvent event) {
        // number of default columns you want to protect
        int defaultColumnCount = 5; // item, object, action, description, input

        if (tableView.getColumns().size() > defaultColumnCount) {
            tableView.getColumns().remove(tableView.getColumns().size() - 1);
        } else {
            showError("Default columns cannot be deleted.");
        }
    }

    private Map<String, Integer> buildColumnMap(Row header) {
        Map<String, Integer> map = new HashMap<>();
        if (header != null) {
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String name = formatter.formatCellValue(header.getCell(c));
                if (!name.isBlank()) {
                    map.put(name.trim(), c);
                }
            }
        }
        return map;
    }

    private String makeKey(TreeItem<TestNode> scenario) {
        List<String> names = new ArrayList<>();
        TreeItem<TestNode> current = scenario;
        while (current != null && current.getValue().getType() != NodeType.ROOT) {
            names.add(0, current.getValue().getName());
            current = current.getParent();
        }
        return String.join("/", names);
    }

    private void saveProject() {
        // Create project directory under resources
        File projectDir = new File("src/main/resources/project");
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }

        for (TreeItem<TestNode> suite : treeView.getRoot().getChildren()) {
            if (suite.getValue().getType() == NodeType.SUITE) {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet(suite.getValue().getName());
                int[] rowIndex = {0};

                // ✅ Export all scenarios/sub-suites recursively
                saveScenariosRecursive(sheet, suite, rowIndex);

                // Write each suite to its own file
                File outFile = new File(projectDir, suite.getValue().getName() + ".xlsx");
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    workbook.write(out);
                } catch (IOException e) {
                    showError("Failed to save suite " + suite.getValue().getName() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Project saved to " + projectDir.getAbsolutePath());
    }


    private void saveScenariosRecursive(Sheet sheet, TreeItem<TestNode> node, int[] rowIndex) {
        NodeType type = node.getValue().getType();

        if (type == NodeType.SUB_SUITE) {
            Row row = sheet.createRow(rowIndex[0]++);
            row.createCell(0).setCellValue("Sub-Suite: " + node.getValue().getName());
        }

        if (type == NodeType.TEST_SCENARIO) {
            String key = makeKey(node);
            List<TestStep> steps = scenarioSteps.getOrDefault(key, List.of());

            // Scenario header row
            Row row = sheet.createRow(rowIndex[0]++);
            row.createCell(0).setCellValue("Scenario: " + node.getValue().getName());

            // ✅ Build header row with default + extras
            Row header = sheet.createRow(rowIndex[0]++);
            int colIndex = 0;
            header.createCell(colIndex++).setCellValue("Item");
            header.createCell(colIndex++).setCellValue("Object");
            header.createCell(colIndex++).setCellValue("Action");
            header.createCell(colIndex++).setCellValue("Description");
            header.createCell(colIndex++).setCellValue("Input");

            List<String> extras = scenarioColumns.getOrDefault(key, List.of());
            for (String colName : extras) {
                header.createCell(colIndex++).setCellValue(colName);
            }

            // ✅ Write each step row with extras
            for (TestStep step : steps) {
                Row stepRow = sheet.createRow(rowIndex[0]++);
                colIndex = 0;
                stepRow.createCell(colIndex++).setCellValue(step.getItem());
                stepRow.createCell(colIndex++).setCellValue(step.getObject());
                stepRow.createCell(colIndex++).setCellValue(step.getAction());
                stepRow.createCell(colIndex++).setCellValue(step.getDescription());
                stepRow.createCell(colIndex++).setCellValue(step.getInput());

                for (String colName : extras) {
                    stepRow.createCell(colIndex++).setCellValue(step.getExtra(colName));
                }
            }
        }

        for (TreeItem<TestNode> child : node.getChildren()) {
            saveScenariosRecursive(sheet, child, rowIndex);
        }
    }


    private void resetDefaultColumns() {
        tableView.getColumns().setAll(itemColumn, objectColumn, actionColumn, descriptionColumn, inputColumn);
    }

    @FXML
    private void handleRun(ActionEvent event) {
        // ✅ Commit any active edits first
        if (tableView.getEditingCell() != null) {
            tableView.edit(-1, null); // forces commit of current edit
        }

        TestStep step = tableView.getSelectionModel().getSelectedItem();
        if (step == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Run Test");
            alert.setHeaderText(null);
            alert.setContentText("⚠️ No step selected to run.");
            alert.show();
            return;
        }

        // 🔎 Debug: show annotation names vs extras map
        String[] inputs = Arrays.stream(ActionLibrary.class.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(ActionMeta.class)).filter(m -> m.getName().equals(step.getAction())).findFirst().map(m -> m.getAnnotation(ActionMeta.class).inputs()).orElse(new String[0]);

        System.out.println("Annotation input names: " + Arrays.toString(inputs));
        System.out.println("Extras map: " + step.getExtras());

        // Run the step
        Object resultObj = TestExecutor.runTest(step);
        System.out.println("Final Result: " + resultObj);
    }


    public static TestCase getTestByAction(Class<?> clazz, String actionName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = method.getAnnotation(ActionMeta.class);
                if (method.getName().equals(actionName)) {
                    return new TestCase(meta.objectName(), method.getName(), meta.description(), String.join(",", meta.inputs()));
                }
            }
        }
        return null;
    }

    private void handleRenameNode(TreeItem<TestNode> node) {
        TextInputDialog dialog = new TextInputDialog(node.getValue().getName());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + node.getValue().getType());
        dialog.setContentText("Enter new name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                node.getValue().setName(newName.trim());
                treeView.refresh();

                // ✅ If it's a TestScenario, update keys in scenarioSteps/scenarioColumns
                if (node.getValue().getType() == NodeType.TEST_SCENARIO) {
                    String oldKey = makeKey(node);
                    String newKey = makeKey(node);
                    if (scenarioSteps.containsKey(oldKey)) {
                        scenarioSteps.put(newKey, scenarioSteps.remove(oldKey));
                    }
                    if (scenarioColumns.containsKey(oldKey)) {
                        scenarioColumns.put(newKey, scenarioColumns.remove(oldKey));
                    }
                }
            }
        });
    }

    private void handleRenameColumn(TableColumn<TestStep, String> column, TreeItem<TestNode> scenario) {
        TextInputDialog dialog = new TextInputDialog(column.getText());
        dialog.setTitle("Rename Column");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                String oldName = column.getText();
                column.setText(newName.trim());

                String key = makeKey(scenario);
                List<String> cols = scenarioColumns.getOrDefault(key, new ArrayList<>());
                int idx = cols.indexOf(oldName);
                if (idx >= 0) cols.set(idx, newName.trim());

                for (TestStep step : tableView.getItems()) {
                    String value = step.getExtra(oldName);
                    step.setExtra(newName.trim(), value);
                }
            }
        });
    }

    @FXML
    private void handleMoveUp(ActionEvent event) {
        int index = tableView.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            ObservableList<TestStep> items = tableView.getItems();
            TestStep step = items.remove(index);
            items.add(index - 1, step);
            tableView.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void handleMoveDown(ActionEvent event) {
        int index = tableView.getSelectionModel().getSelectedIndex();
        ObservableList<TestStep> items = tableView.getItems();
        if (index >= 0 && index < items.size() - 1) {
            TestStep step = items.remove(index);
            items.add(index + 1, step);
            tableView.getSelectionModel().select(index + 1);
        }
    }

    /**
     * Ensure argument columns exist for the given step
     */
    private void ensureArgumentColumns(TestStep step) {
        if (step == null || step.getAction() == null || step.getAction().isBlank()) {
            return;
        }

        for (Method m : ActionLibrary.class.getDeclaredMethods()) {
            if (m.getName().equals(step.getAction()) && m.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = m.getAnnotation(ActionMeta.class);
                TreeItem<TestNode> scenario = treeView.getSelectionModel().getSelectedItem();
                if (scenario != null && scenario.getValue().getType() == NodeType.TEST_SCENARIO) {
                    for (String inputName : meta.inputs()) {
                        boolean exists = tableView.getColumns().stream().anyMatch(c -> c.getText().equals(inputName));
                        if (!exists) {
                            addColumnForScenario(scenario, inputName);
                        }
                    }
                }
                break; // stop once we’ve handled the matching method
            }
        }
    }

    private void rebuildArgumentColumns() {
        tableView.setEditable(true);
        // Remove old dynamic columns
        tableView.getColumns().removeIf(c -> "Dynamic".equals(c.getUserData()));

        // Find max number of inputs across all actions
        int maxInputs = Arrays.stream(ActionLibrary.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ActionMeta.class))
                .mapToInt(m -> m.getAnnotation(ActionMeta.class).inputs().length)
                .max()
                .orElse(0);

        for (int i = 0; i < maxInputs; i++) {
            final int colIndex = i;
            TableColumn<TestStep, String> col = new TableColumn<>("Arg" + (i + 1));
            col.setUserData("Dynamic");
            col.setEditable(true);

            // Bind to extras property if available
            col.setCellValueFactory(cellData -> {
                TestStep step = cellData.getValue();
                if (step == null || step.getAction() == null || step.getAction().isBlank()) {
                    return new ReadOnlyStringWrapper("");
                }
                String[] inputs = getInputsForAction(step.getAction());
                if (colIndex < inputs.length) {
                    return step.getExtraProperty(inputs[colIndex]);
                }
                return new ReadOnlyStringWrapper("");
            });

            // Custom cell with floating label + text field
            col.setCellFactory(tc -> new TableCell<>() {
                private final TextField textField = new TextField();
                private final Label floatingLabel = new Label();
                private final StackPane stack = new StackPane();

                {
                    floatingLabel.getStyleClass().add("floating-label");
                    floatingLabel.setMouseTransparent(true);
                    textField.getStyleClass().add("arg-text-field");

                    stack.getChildren().addAll(floatingLabel, textField);
                    setGraphic(stack);

                    textField.setOnAction(e -> commitEdit(textField.getText()));
                    textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal) commitEdit(textField.getText());
                    });

                    textField.textProperty().addListener((obs, oldVal, newVal) ->
                            updateLabelState(newVal, textField.isFocused()));
                    textField.focusedProperty().addListener((obs, oldVal, newVal) ->
                            updateLabelState(textField.getText(), newVal));
                }

                private void updateLabelState(String text, boolean focused) {
                    if ((text == null || text.isBlank()) && !focused) {
                        floatingLabel.getStyleClass().remove("floated");
                    } else if (!floatingLabel.getStyleClass().contains("floated")) {
                        floatingLabel.getStyleClass().add("floated");
                    }
                }

                @Override
                public void startEdit() {
                    TestStep step = getTableRow() != null ? getTableRow().getItem() : null;
                    if (step == null || step.getAction() == null || step.getAction().isBlank()) return;
                    String[] inputs = getInputsForAction(step.getAction());
                    if (colIndex >= inputs.length) return;

                    super.startEdit();
                    textField.setText(getItem());
                    floatingLabel.setText(inputs[colIndex]);
                    updateLabelState(getItem(), true);
                    textField.requestFocus();
                }

                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null) {
                        setGraphic(null);
                        return;
                    }

                    TestStep step = getTableRow().getItem();
                    if (step == null || step.getAction() == null || step.getAction().isBlank()) {
                        floatingLabel.setText("");
                        textField.setText("");
                        textField.setDisable(true);
                        return;
                    }

                    String[] inputs = getInputsForAction(step.getAction());
                    if (colIndex < inputs.length) {
                        String key = inputs[colIndex];
                        floatingLabel.setText(key);
                        textField.setDisable(false);

                        // Bind text field directly to extras property
                        SimpleStringProperty prop = step.getExtraProperty(key);
                        textField.textProperty().unbindBidirectional(prop);
                        textField.textProperty().bindBidirectional(prop);

                        textField.setText(item);
                        updateLabelState(item, textField.isFocused());
                    } else {
                        floatingLabel.setText("");
                        textField.setText("");
                        textField.setDisable(true);
                    }
                }
            });

            // Commit handler: store under annotation-driven key
            col.setOnEditCommit(event -> {
                TestStep step = event.getRowValue();
                if (step == null || step.getAction() == null || step.getAction().isBlank()) return;

                String[] inputs = getInputsForAction(step.getAction());
                if (colIndex < inputs.length) {
                    String key = inputs[colIndex];
                    String value = event.getNewValue();
                    step.setExtra(key, value);
                    System.out.println("Committed: " + key + "=" + value);
                }
            });

            tableView.getColumns().add(col);
        }
    }



    private String[] getInputsForAction(String actionName) {
        return Arrays.stream(ActionLibrary.class.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(ActionMeta.class)).filter(m -> m.getName().equals(actionName)).findFirst().map(m -> m.getAnnotation(ActionMeta.class).inputs()).orElse(new String[0]);
    }

    private void updateColumnHeadersForAction() {
        List<TableColumn<TestStep, ?>> dynamicCols = tableView.getColumns().stream().filter(c -> "Dynamic".equals(c.getUserData())).toList();

        for (int i = 0; i < dynamicCols.size(); i++) {
            @SuppressWarnings("unchecked") TableColumn<TestStep, String> col = (TableColumn<TestStep, String>) dynamicCols.get(i);
            col.setText("Arg" + (i + 1)); // ✅ stable header only
        }
    }


}