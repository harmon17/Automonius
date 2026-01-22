package org.automonius;

import java.io.*;
import java.net.URL;
import java.util.*;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
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
import javafx.stage.FileChooser;
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

import java.lang.reflect.Method;

import javafx.scene.control.TextArea;

import java.util.logging.Logger;
import java.util.stream.Collectors;


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

    private final Map<String, ObservableList<TestStep>> scenarioSteps = new HashMap<>();


    // Cache of extra column names per scenario
    private final Map<String, List<String>> scenarioColumns = new HashMap<>();
    @FXML
    private final DataFormatter formatter = new DataFormatter();
    private Map<String, List<String>> actionsByObject;
    @FXML
    private TextArea executionPreviewArea;
    private static final Logger log = Logger.getLogger(MainController.class.getName());
    @FXML
    private CheckBox showStepsToggle;
    private static boolean tableDirty = false;
    private final Map<String, List<String>> argsByObject = new HashMap<>();
    private final Map<String, Integer> maxArgsByObject = new HashMap<>();
    private final ObservableList<TestStep> steps = FXCollections.observableArrayList();
    private TreeItem<TestNode> draggedItem;


    @FXML
    public void initialize() {
        // Load available actions by object
        actionsByObject = TestExecutor.getActionsByObject(ActionLibrary.class);

// Build argsByObject and maxArgsByObject dynamically
        argsByObject.clear();
        maxArgsByObject.clear();


        // Discover actions and build argsByObject / maxArgsByObject
        Map<String, List<TestCase>> discovered = TestExecutor.discoverActions(ActionLibrary.class);
        for (Map.Entry<String, List<TestCase>> entry : discovered.entrySet()) {
            String objectName = entry.getKey();

            // Collect all distinct inputs across this object's actions
            List<String> allInputs = entry.getValue().stream()
                    .flatMap(tc -> tc.getInputs().stream())
                    .distinct()
                    .toList();

            argsByObject.put(objectName, allInputs);
            maxArgsByObject.put(objectName, allInputs.size());

            // Collect all action names for this object
            List<String> actions = entry.getValue().stream()
                    .map(TestCase::getActionName)
                    .toList();
            actionsByObject.put(objectName, actions);
        }

// ✅ Select the object with the most args
        String defaultObject = argsByObject.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (defaultObject != null) {
            String defaultAction = actionsByObject.getOrDefault(defaultObject, List.of())
                    .stream().findFirst().orElse(null);

            // Seed the blank step
            if (!steps.isEmpty()) {
                TestStep blank = steps.get(0);
                blank.setObject(defaultObject);
                blank.setAction(defaultAction);

                List<String> defaultArgs = argsByObject.getOrDefault(defaultObject, List.of());
                blank.setExtras(defaultArgs.stream()
                        .collect(Collectors.toMap(arg -> arg, arg -> "")));
                blank.setMaxArgs(defaultArgs.size());

                log.info("Initialized with default object=" + defaultObject +
                        ", action=" + defaultAction +
                        ", args=" + defaultArgs);
            }
        }


        // --- TreeView setup ---
        TreeItem<TestNode> root = new TreeItem<>(new TestNode("Directory Structure", NodeType.ROOT));
        root.setExpanded(true);
        treeView.setRoot(root);

        // --- Copy/Paste shortcuts ---
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent clipboardContent = new ClipboardContent();

        treeView.setOnKeyPressed(event -> {
            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                if (selected.getValue().getType() == NodeType.SUITE) {
                    clipboardContent.putString("SUITE:" + selected.getValue().getSuiteRef().getId());
                    clipboard.setContent(clipboardContent);
                } else if (selected.getValue().getType() == NodeType.TEST_SCENARIO) {
                    clipboardContent.putString("SCENARIO:" + selected.getValue().getScenarioRef().getId());
                    clipboard.setContent(clipboardContent);
                }
            }
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                String data = clipboard.getString();
                if (data != null) {
                    if (data.startsWith("SUITE:") && selected.getValue().getType() == NodeType.ROOT) {
                        TestSuite original = selected.getValue().getSuiteRef();
                        TestSuite copy = cloneSuite(original);
                        TreeItem<TestNode> copyItem = buildSuiteNode(copy);
                        selected.getChildren().add(copyItem);
                    }
                    if (data.startsWith("SCENARIO:") &&
                            (selected.getValue().getType() == NodeType.SUITE || selected.getValue().getType() == NodeType.SUB_SUITE)) {
                        TestScenario original = selected.getValue().getScenarioRef();
                        TestScenario copy = cloneScenario(original);
                        TreeItem<TestNode> copyItem = buildScenarioNode(copy);
                        selected.getChildren().add(copyItem);
                    }
                }
            }
        });

        // --- Context menu for right-click ---
        ContextMenu contextMenu = new ContextMenu();

        MenuItem copySuiteItem = new MenuItem("Copy Suite");
        copySuiteItem.setOnAction(e -> {
            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue().getType() == NodeType.SUITE) {
                TestSuite original = selected.getValue().getSuiteRef();
                if (original != null) {
                    TestSuite copy = cloneSuite(original);
                    TreeItem<TestNode> copyItem = buildSuiteNode(copy);
                    selected.getParent().getChildren().add(copyItem);

                    for (TestScenario scenario : copy.getScenarios()) {
                        scenarioSteps.put(scenario.getId(),
                                FXCollections.observableArrayList(scenario.getSteps().stream()
                                        .map(TestStep::new)
                                        .toList()));
                        scenarioColumns.put(scenario.getId(), List.of());
                    }

                    updateExecutionPreview("Copied suite: " + copy.getName() +
                            " (id=" + copy.getId() + ")");
                }
            }
        });


        MenuItem copyScenarioItem = new MenuItem("Copy Scenario");
        copyScenarioItem.setOnAction(e -> {
            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
                TestScenario original = selected.getValue().getScenarioRef();
                if (original != null) {
                    TestScenario copy = cloneScenario(original);
                    TreeItem<TestNode> copyItem = buildScenarioNode(copy);
                    selected.getParent().getChildren().add(copyItem);

                    scenarioSteps.put(copy.getId(),
                            FXCollections.observableArrayList(copy.getSteps().stream()
                                    .map(TestStep::new)
                                    .toList()));
                    scenarioColumns.put(copy.getId(), List.of());

                    updateExecutionPreview("Copied scenario: " + copy.getName() +
                            " (id=" + copy.getId() + ")");
                }
            }
        });


        contextMenu.getItems().addAll(copySuiteItem, copyScenarioItem);
        treeView.setContextMenu(contextMenu);

        // --- Drag and Drop setup ---

        treeView.setOnDragDetected(event -> {
            draggedItem = treeView.getSelectionModel().getSelectedItem();
            if (draggedItem != null) {
                Dragboard db = treeView.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(draggedItem.getValue().getName());
                db.setContent(content);
            }
            event.consume();
        });

        treeView.setOnDragOver(event -> {
            if (event.getGestureSource() != treeView && event.getDragboard().hasString()) {
                Node targetNode = event.getPickResult().getIntersectedNode();
                TreeCell<TestNode> cell = null;
                while (targetNode != null && !(targetNode instanceof TreeCell)) {
                    targetNode = targetNode.getParent();
                }
                if (targetNode instanceof TreeCell) {
                    cell = (TreeCell<TestNode>) targetNode;
                }

                if (cell != null && cell.getTreeItem() != null) {
                    TreeItem<TestNode> targetItem = cell.getTreeItem();
                    if (draggedItem != null && isValidDrop(draggedItem, targetItem)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
            }
            event.consume();
        });

        treeView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                Node targetNode = event.getPickResult().getIntersectedNode();
                TreeCell<TestNode> cell = null;
                while (targetNode != null && !(targetNode instanceof TreeCell)) {
                    targetNode = targetNode.getParent();
                }
                if (targetNode instanceof TreeCell) {
                    cell = (TreeCell<TestNode>) targetNode;
                }

                if (cell != null && cell.getTreeItem() != null) {
                    TreeItem<TestNode> targetItem = cell.getTreeItem();
                    if (draggedItem != null && isValidDrop(draggedItem, targetItem)) {
                        draggedItem.getParent().getChildren().remove(draggedItem);
                        targetItem.getChildren().add(draggedItem);
                        event.setDropCompleted(true);
                    } else {
                        event.setDropCompleted(false);
                    }
                }
            }
            event.consume();
        });


        // --- Toggle listener rebuilds tree ---
        showStepsToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            TreeItem<TestNode> currentRoot = treeView.getRoot();
            if (currentRoot != null) {
                TreeItem<TestNode> newRoot = new TreeItem<>(new TestNode("Directory Structure", NodeType.ROOT));
                newRoot.setExpanded(true);
                for (TreeItem<TestNode> childSuite : currentRoot.getChildren()) {
                    TestSuite suiteModel = childSuite.getValue().getSuiteRef();
                    if (suiteModel != null) {
                        newRoot.getChildren().add(buildSuiteNode(suiteModel));
                    }
                }
                treeView.setRoot(newRoot);
            }
        });

        // --- TreeView cell factory with icons ---
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(TestNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item.getName());
                    setStyle("");
                    switch (item.getType()) {
                        case ROOT -> setGraphic(makeIcon("/icons/bank.png", 16, 16));
                        case SUITE -> setGraphic(makeIcon("/icons/MainSuite.png", 16, 16));
                        case SUB_SUITE -> setGraphic(makeIcon("/icons/SubSuite.png", 16, 16));
                        case TEST_SCENARIO -> setGraphic(makeIcon("/icons/TestSuite.png", 16, 16));
                        case TEST_STEP -> {
                            setGraphic(makeIcon("/icons/Step.png", 14, 14));
                            if (item.getName() == null || item.getName().isBlank()) {
                                setText("<empty step>");
                                setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                            }
                        }
                    }
                }
            }
        });

        // --- Selection listener delegates to loadTestScenario ---
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            // --- Commit edits back into the old scenario before switching ---
            if (oldItem != null
                    && oldItem.getValue().getType() == NodeType.TEST_SCENARIO
                    && MainController.isTableDirty()) {

                TestScenario oldScenario = oldItem.getValue().getScenarioRef();
                if (oldScenario != null) {
                    ObservableList<TestStep> committedSteps = FXCollections.observableArrayList();
                    for (TestStep step : tableView.getItems()) {
                        committedSteps.add(new TestStep(step)); // ✅ deep copy includes extras
                    }

                    // Persist steps in both the model and scenarioSteps map
                    oldScenario.getSteps().setAll(committedSteps);
                    scenarioSteps.put(oldScenario.getId(), committedSteps);

                    log.info(String.format(
                            "Commit snapshot: scenario=%s, steps list identity=%d",
                            oldScenario.getId(),
                            System.identityHashCode(committedSteps)
                    ));
                    for (int i = 0; i < committedSteps.size(); i++) {
                        TestStep s = committedSteps.get(i);
                        log.info(String.format(
                                "Commit row=%d, step identity=%d, stepId=%s, extras=%s",
                                i + 1,
                                System.identityHashCode(s),
                                s.getId(),
                                s.getArgs()
                        ));
                    }

                    // Persist the dynamic argument names for this scenario
                    List<String> argNamesForScenario = tableView.getColumns().stream()
                            .filter(c -> "Dynamic".equals(c.getUserData()))
                            .map(TableColumn::getText)
                            .toList();
                    scenarioColumns.put(oldScenario.getId(), argNamesForScenario);


                    String timestamp = java.time.LocalDateTime.now().toString();
                    logScenarioSnapshot("Commit", oldScenario, committedSteps, timestamp);
                    writeScenarioSnapshotToFile(oldScenario, committedSteps, "Commit", timestamp);
                }

                MainController.resetTableDirty(); // ✅ clear dirty flag after commit
            }

            // --- Load the new scenario into the TableView ---
            if (newItem != null && newItem.getValue().getType() == NodeType.TEST_SCENARIO) {
                TestScenario newScenario = newItem.getValue().getScenarioRef();
                if (newScenario != null) {
                    ObservableList<TestStep> stepsCopy = FXCollections.observableArrayList(
                            newScenario.getSteps().stream()
                                    .map(TestStep::new)
                                    .toList()
                    );
                    scenarioSteps.put(newScenario.getId(), stepsCopy);
                    tableView.setItems(stepsCopy);

                    log.info(String.format(
                            "Load snapshot: scenario=%s, steps list identity=%d",
                            newScenario.getId(),
                            System.identityHashCode(stepsCopy)
                    ));
                    for (int i = 0; i < stepsCopy.size(); i++) {
                        TestStep s = stepsCopy.get(i);
                        log.info(String.format(
                                "Load row=%d, step identity=%d, stepId=%s, extras=%s",
                                i + 1,
                                System.identityHashCode(s),
                                s.getId(),
                                s.getArgs()
                        ));
                    }

                    List<String> cols = scenarioColumns.getOrDefault(newScenario.getId(), List.of());
                    rebuildArgumentColumns(cols);

                    updateExecutionPreview("Loaded scenario: " + newScenario.getName() +
                            " (id=" + newScenario.getId() + ")");

                    String timestamp = java.time.LocalDateTime.now().toString();
                    logScenarioSnapshot("Load", newScenario, stepsCopy, timestamp);
                    writeScenarioSnapshotToFile(newScenario, stepsCopy, "Load", timestamp);
                }
            }
        });



        // --- Auto-select first scenario (if any) ---
        Platform.runLater(() -> {
            if (!root.getChildren().isEmpty()) {
                TreeItem<TestNode> firstSuite = root.getChildren().get(0);
                if (!firstSuite.getChildren().isEmpty()) {
                    TreeItem<TestNode> firstScenarioItem = firstSuite.getChildren().get(0);
                    treeView.getSelectionModel().select(firstScenarioItem);
                    treeView.getFocusModel().focus(treeView.getRow(firstScenarioItem));
                }
            }
        });


        // --- Table setup ---
        tableView.setEditable(true);

        // Item column
        itemColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(tableView.getItems().indexOf(cellData.getValue()) + 1))
        );
        itemColumn.setEditable(false);
        itemColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // Description column
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        descriptionColumn.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            tableDirty = true;
            log.info(() -> "Edited description for step: " + event.getRowValue());
        });


        // Input column
        inputColumn.setCellValueFactory(new PropertyValueFactory<>("input"));
        inputColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        inputColumn.setOnEditCommit(event -> {
            event.getRowValue().setInput(event.getNewValue());
            tableDirty = true;
            log.info(() -> "Edited input for step: " + event.getRowValue());
        });

        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setEditable(true);
        objectColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();
                ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(actionsByObject.keySet()));
                combo.valueProperty().bindBidirectional(step.objectProperty());

                combo.setOnAction(event -> {
                    String selected = combo.getValue();
                    if (selected != null) {
                        step.setObject(selected);

                        // Auto‑assign first action for this object
                        List<String> methods = actionsByObject.getOrDefault(selected, List.of());
                        if (!methods.isEmpty()) {
                            step.setAction(methods.get(0));
                        }

                        // Restore args and maxArgs
                        List<String> restoredArgs = argsByObject.getOrDefault(selected, List.of());
                        step.setExtras(restoredArgs.stream().collect(Collectors.toMap(arg -> arg, arg -> "")));
                        step.setMaxArgs(restoredArgs.size());

                        // ✅ Persist headers for current scenario
                        TreeItem<TestNode> selectedScenario = treeView.getSelectionModel().getSelectedItem();
                        if (selectedScenario != null && selectedScenario.getValue().getScenarioRef() != null) {
                            String scenarioId = selectedScenario.getValue().getScenarioRef().getId();
                            scenarioColumns.put(scenarioId, new ArrayList<>(step.getExtras().keySet()));
                        }

                        tableDirty = true;
                    }
                });

                setGraphic(combo);
            }
        });



        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setEditable(true);
        actionColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();
                List<String> availableActions = step.getObject() != null && !step.getObject().isBlank()
                        ? actionsByObject.getOrDefault(step.getObject(), List.of())
                        : List.of();

                ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(availableActions));
                combo.setDisable(availableActions.isEmpty());
                combo.valueProperty().bindBidirectional(step.actionProperty());

                combo.setOnAction(event -> {
                    String selected = combo.getValue();
                    if (selected != null) {
                        step.setAction(selected);

                        // Restore args for this action
                        String[] inputs = getInputsForAction(selected);
                        step.setExtras(Arrays.stream(inputs).collect(Collectors.toMap(arg -> arg, arg -> "")));
                        step.setMaxArgs(inputs.length);

                        // ✅ Persist headers for current scenario
                        TreeItem<TestNode> selectedScenario = treeView.getSelectionModel().getSelectedItem();
                        if (selectedScenario != null && selectedScenario.getValue().getScenarioRef() != null) {
                            String scenarioId = selectedScenario.getValue().getScenarioRef().getId();
                            scenarioColumns.put(scenarioId, new ArrayList<>(step.getExtras().keySet()));
                        }

                        tableDirty = true;
                    }
                });

                setGraphic(combo);
            }
        });



        // Set static columns first
        tableView.getColumns().setAll(itemColumn, objectColumn, actionColumn, descriptionColumn, inputColumn);


        tableView.setFixedCellSize(25);
        // After tableView setup in initialize()
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldStep, newStep) -> {
            if (newStep != null) {
                log.info(() -> "Selected step: item=" + newStep.getItem() +
                        ", action=" + newStep.getAction() +
                        ", object=" + newStep.getObject());
            } else {
                log.info("TableView selection cleared.");
            }
        });


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
        return scenario.getValue().getId(); // use UUID, not name path
    }


    private void saveProject() {
        // Ensure project directory exists under resources
        File projectDir = new File("src/main/resources/project");
        if (!projectDir.exists() && !projectDir.mkdirs()) {
            showError("Failed to create project directory: " + projectDir.getAbsolutePath());
            return;
        }

        // Iterate over all top-level suites
        for (TreeItem<TestNode> suiteItem : treeView.getRoot().getChildren()) {
            TestNode suiteNode = suiteItem.getValue();
            if (suiteNode.getType() == NodeType.SUITE && suiteNode.getSuiteRef() != null) {
                TestSuite suiteModel = suiteNode.getSuiteRef();

                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet(suiteModel.getName());
                int[] rowIndex = {0};

                // ✅ Export all scenarios/sub-suites recursively using model references
                saveScenariosRecursive(sheet, suiteItem, rowIndex);

                // Write each suite to its own file
                File outFile = new File(projectDir, suiteModel.getName() + ".xlsx");
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    workbook.write(out);
                    updateExecutionPreview("Saved suite: " + suiteModel.getName() +
                            " (id=" + suiteModel.getId() + ") → " + outFile.getName());
                } catch (IOException e) {
                    showError("Failed to save suite " + suiteModel.getName() + ": " + e.getMessage());
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
            TestScenario scenario = node.getValue().getScenarioRef();
            if (scenario != null) {
                // Scenario header row
                Row row = sheet.createRow(rowIndex[0]++);
                row.createCell(0).setCellValue("Scenario: " + scenario.getName());

                // ✅ Build header row with default + extras
                Row header = sheet.createRow(rowIndex[0]++);
                int colIndex = 0;
                header.createCell(colIndex++).setCellValue("Item");
                header.createCell(colIndex++).setCellValue("Object");
                header.createCell(colIndex++).setCellValue("Action");
                header.createCell(colIndex++).setCellValue("Description");
                header.createCell(colIndex++).setCellValue("Input");

                List<String> extras = scenario.getExtras(); // now owned by the model
                for (String colName : extras) {
                    header.createCell(colIndex++).setCellValue(colName);
                }

                // ✅ Write each step row with extras
                for (TestStep step : scenario.getSteps()) {
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
        }

        // Recurse into children
        for (TreeItem<TestNode> child : node.getChildren()) {
            saveScenariosRecursive(sheet, child, rowIndex);
        }
    }


    private void resetDefaultColumns() {
        tableView.getColumns().setAll(itemColumn, objectColumn, actionColumn, descriptionColumn, inputColumn);
    }

    @FXML
    private void handleRun(ActionEvent event) {
        if (tableView.getEditingCell() != null) {
            tableView.edit(-1, null);
        }

        TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
        if (selectedStep == null && !tableView.getItems().isEmpty()) {
            tableView.getSelectionModel().selectFirst();
            selectedStep = tableView.getSelectionModel().getSelectedItem();
        }

        if (selectedStep == null) {
            updateExecutionPreview("⚠️ No step selected to run.");
            return;
        }

        String[] inputs = getInputsForAction(selectedStep.getAction());
        updateExecutionPreview("Annotation input names: " + Arrays.toString(inputs));
        updateExecutionPreview("Extras map: " + selectedStep.getExtras());

        Object resultObj = TestExecutor.runTest(selectedStep);

        String message = "Ran step: " + selectedStep.getAction() +
                " on " + selectedStep.getObject() +
                " → Result: " + resultObj;

        updateExecutionPreview(message);
        logStepChange("Run", selectedStep, Map.of("result", resultObj));

    }


    public static TestCase getTestByAction(Class<?> clazz, String actionName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = method.getAnnotation(ActionMeta.class);
                if (method.getName().equals(actionName)) {
                    // ✅ Pass inputs as a List<String>
                    return new TestCase(
                            meta.objectName(),
                            method.getName(),
                            meta.description(),
                            Arrays.asList(meta.inputs())
                    );
                }
            }
        }
        return null; // not found
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

    private void rebuildArgumentColumns(List<String> persistedArgNames) {
        tableView.getColumns().setAll(itemColumn, objectColumn, actionColumn, descriptionColumn, inputColumn);

        // Prefer persisted headers; if none, derive union of extras
        Set<String> allArgNames = tableView.getItems().stream()
                .flatMap(step -> step.getArgs().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> argNames = !persistedArgNames.isEmpty()
                ? persistedArgNames
                : new ArrayList<>(allArgNames);

        if (argNames.isEmpty()) {
            log.info("Rebuilding dynamic columns skipped: no args for current scenario.");
            return;
        }

        log.info("Rebuilding dynamic columns: argNames=" + argNames);

        for (String key : argNames) {
            TableColumn<TestStep, String> col = new TableColumn<>(key);
            col.setUserData("Dynamic");
            col.setEditable(true);

            col.setCellFactory(tc -> new TableCell<>() {
                private final TextField textField = new TextField();
                private final Label floatingLabel = new Label();
                private final StackPane stack = new StackPane(floatingLabel, textField);
                private SimpleStringProperty currentProp;

                {
                    floatingLabel.getStyleClass().add("floating-label");
                    floatingLabel.setMouseTransparent(true);
                    textField.getStyleClass().add("arg-text-field");
                    setGraphic(stack);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (currentProp != null) {
                        textField.textProperty().unbindBidirectional(currentProp);
                        currentProp = null;
                    }

                    if (empty || getTableRow() == null) {
                        setGraphic(null);
                        return;
                    }

                    TestStep step = getTableRow().getItem();
                    if (step == null) {
                        setGraphic(null);
                        return;
                    }

                    floatingLabel.setText(key);

                    if (step.getArgs().contains(key)) {
                        textField.setDisable(false);
                        SimpleStringProperty newProp = step.getExtraProperty(key);
                        textField.textProperty().bindBidirectional(newProp);
                        currentProp = newProp;
                    } else {
                        textField.setText("");
                        textField.setDisable(true);
                        textField.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    }

                    setGraphic(stack);
                }
            });

            tableView.getColumns().add(col);
        }

        tableView.refresh();
    }


    private String[] getInputsForAction(String actionName) {
        return Arrays.stream(ActionLibrary.class.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(ActionMeta.class)).filter(m -> m.getName().equals(actionName)).findFirst().map(m -> m.getAnnotation(ActionMeta.class).inputs()).orElse(new String[0]);
    }


    private void updateExecutionPreview(String message) {
        if (executionPreviewArea != null) {
            executionPreviewArea.appendText("Preview: " + message + "\n");
        }
        log.info(() -> "Execution preview updated: " + message);
    }


    @FXML
    private void handleRunScenario(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
            TestScenario scenario = selected.getValue().getScenarioRef();
            if (scenario != null) {
                // Start message
                updateExecutionPreview("Running " + scenario.getName() + " in Test environment...");

                // Loop through all steps
                for (TestStep step : scenario.getSteps()) {
                    Object resultObj = TestExecutor.runTest(step);

                    // Append each step result into preview pane
                    updateExecutionPreview("Step: " + step.getAction() +
                            " on " + step.getObject() +
                            " → Result: " + resultObj);
                }

                // Finish message
                updateExecutionPreview("Finished running " + scenario.getName());
            } else {
                updateExecutionPreview("No scenario linked to this node.");
            }
        }
    }

    private void commitTableEditsToScenario(TestScenario scenario) {
        // Clear the scenario's original steps
        scenario.getSteps().clear();

        // Copy each row from the TableView back into the scenario
        for (TestStep edited : tableView.getItems()) {
            TestStep copy = new TestStep(
                    edited.getItem(),
                    edited.getAction(),
                    edited.getObject(),
                    edited.getInput()
            );
            copy.setDescription(edited.getDescription());

            if (edited.getExtras() != null) {
                for (Map.Entry<String, SimpleStringProperty> entry : edited.getExtras().entrySet()) {
                    copy.setExtra(entry.getKey(), entry.getValue().get());
                }
            }

            scenario.getSteps().add(copy);
        }
    }


    private TestScenario importScenarioFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // First row: scenario header
            Row headerRow = sheet.getRow(0);
            String scenarioName = headerRow.getCell(0).getStringCellValue().replace("Scenario: ", "");

            TestScenario scenario = new TestScenario(UUID.randomUUID().toString(), scenarioName);

            // Second row: column headers
            Row colHeaderRow = sheet.getRow(1);
            List<String> extras = new ArrayList<>();
            for (int i = 5; i < colHeaderRow.getLastCellNum(); i++) {
                extras.add(colHeaderRow.getCell(i).getStringCellValue());
            }
            scenario.setExtras(extras);

            // Remaining rows: steps
            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String item = row.getCell(0).getStringCellValue();
                String object = row.getCell(1).getStringCellValue();
                String action = row.getCell(2).getStringCellValue();
                String description = row.getCell(3).getStringCellValue();
                String input = row.getCell(4).getStringCellValue();

                TestStep step = new TestStep(item, action, object, input);
                step.setDescription(description);

                for (int i = 0; i < extras.size(); i++) {
                    String extraName = extras.get(i);
                    String extraVal = row.getCell(5 + i).getStringCellValue();
                    step.setExtra(extraName, extraVal);
                }

                scenario.getSteps().add(step);
            }

            return scenario;

        } catch (IOException e) {
            showError("Failed to load scenario from " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public TestSuite importSuiteFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // First row: suite header
            Row headerRow = sheet.getRow(0);
            String suiteName = headerRow.getCell(0).getStringCellValue().replace("Suite: ", "");

            TestSuite suite = new TestSuite(UUID.randomUUID().toString(), suiteName);

            // Walk through rows
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String cellVal = row.getCell(0).getStringCellValue();

                // Handle sub-suites
                if (cellVal.startsWith("Sub-Suite: ")) {
                    String subName = cellVal.replace("Sub-Suite: ", "");
                    TestSuite subSuite = new TestSuite(UUID.randomUUID().toString(), subName);
                    suite.addSubSuite(subSuite);   // ✅ use subSuites list
                    continue;
                }

                // Handle scenarios
                if (cellVal.startsWith("Scenario: ")) {
                    String scenarioName = cellVal.replace("Scenario: ", "");
                    TestScenario scenario = new TestScenario(UUID.randomUUID().toString(), scenarioName);

                    // Next row is header
                    Row colHeaderRow = sheet.getRow(++r);
                    List<String> extras = new ArrayList<>();
                    for (int i = 5; i < colHeaderRow.getLastCellNum(); i++) {
                        extras.add(colHeaderRow.getCell(i).getStringCellValue());
                    }
                    scenario.setExtras(extras);

                    // Step rows until next marker
                    while (++r <= sheet.getLastRowNum()) {
                        Row stepRow = sheet.getRow(r);
                        if (stepRow == null) continue;
                        String marker = stepRow.getCell(0).getStringCellValue();
                        if (marker.startsWith("Scenario: ") || marker.startsWith("Sub-Suite: ")) {
                            r--; // back up one row so outer loop can handle marker
                            break;
                        }

                        String item = marker;
                        String object = stepRow.getCell(1).getStringCellValue();
                        String action = stepRow.getCell(2).getStringCellValue();
                        String description = stepRow.getCell(3).getStringCellValue();
                        String input = stepRow.getCell(4).getStringCellValue();

                        TestStep step = new TestStep(item, action, object, input);
                        step.setDescription(description);

                        for (int i = 0; i < extras.size(); i++) {
                            String extraName = extras.get(i);
                            String extraVal = stepRow.getCell(5 + i).getStringCellValue();
                            step.setExtra(extraName, extraVal);
                        }

                        scenario.getSteps().add(step);
                    }

                    suite.addScenario(scenario);   // ✅ use addScenario helper
                }
            }

            return suite;

        } catch (IOException e) {
            showError("Failed to load suite from " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private TreeItem<TestNode> buildSuiteNode(TestSuite suite) {
        TestNode suiteNode = new TestNode(suite.getName(), NodeType.SUITE);
        suiteNode.setSuiteRef(suite);
        TreeItem<TestNode> suiteItem = new TreeItem<>(suiteNode);
        suiteItem.setExpanded(true);

        for (TestScenario scenario : suite.getScenarios()) {
            TestNode scenarioNode = new TestNode(scenario.getName(), NodeType.TEST_SCENARIO);
            scenarioNode.setScenarioRef(scenario);
            TreeItem<TestNode> scenarioItem = new TreeItem<>(scenarioNode);
            scenarioItem.setExpanded(true);

            // ✅ Toggle decides if steps are shown
            if (showStepsToggle != null && showStepsToggle.isSelected()) {
                for (TestStep step : scenario.getSteps()) {
                    String stepName = (step.getDescription() != null && !step.getDescription().isBlank())
                            ? step.getDescription()
                            : step.getAction();
                    TestNode stepNode = new TestNode(stepName, NodeType.TEST_STEP);
                    stepNode.setStepRef(step);
                    scenarioItem.getChildren().add(new TreeItem<>(stepNode));
                }
            }

            suiteItem.getChildren().add(scenarioItem);
        }

        // Recursive sub-suites
        for (TestSuite subSuite : suite.getSubSuites()) {
            suiteItem.getChildren().add(buildSuiteNode(subSuite));
        }

        return suiteItem;
    }

    private TestSuite cloneSuite(TestSuite original) {
        TestSuite copy = new TestSuite(original.getName() + " Copy");

        for (TestScenario scenario : original.getScenarios()) {
            TestScenario scenarioCopy = cloneScenario(scenario);
            copy.addScenario(scenarioCopy);
        }

        return copy;
    }


    @FXML
    private void handleCopySuite(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.SUITE) {
            TestSuite original = selected.getValue().getSuiteRef();
            if (original != null) {
                TestSuite copyModel = cloneSuite(original);

                TestNode copyNode = new TestNode(copyModel.getName(), NodeType.SUITE);
                copyNode.setSuiteRef(copyModel);

                TreeItem<TestNode> copyItem = new TreeItem<>(
                        copyNode,
                        makeIcon("/icons/MainSuite.png", 16, 16)
                );
                copyItem.setExpanded(true);

                selected.getParent().getChildren().add(copyItem);

                updateExecutionPreview("Copied suite: " + original.getName() +
                        " → " + copyModel.getName() + " (id=" + copyModel.getId() + ")");
            }
        } else {
            showError("Copy Suite can only be used on a Suite node.");
        }
    }

    private TestScenario cloneScenario(TestScenario original) {
        TestScenario copy = new TestScenario(original.getName() + " Copy");

        for (TestStep step : original.getSteps()) {
            copy.addStep(new TestStep(step)); // use copy constructor
        }

        copy.setExtras(new ArrayList<>(original.getExtras()));
        return copy;
    }


    private TreeItem<TestNode> buildScenarioNode(TestScenario scenario) {
        // Create a TestNode wrapper for the scenario
        TestNode node = new TestNode(scenario.getName(), NodeType.TEST_SCENARIO);
        node.setScenarioRef(scenario);

        // Create the TreeItem with the correct icon
        TreeItem<TestNode> scenarioItem = new TreeItem<>(
                node,
                makeIcon("/icons/TestSuite.png", 16, 16)
        );
        scenarioItem.setExpanded(true);

        // Add step children if "showStepsToggle" is enabled
        if (showStepsToggle.isSelected()) {
            for (TestStep step : scenario.getSteps()) {
                TestNode stepNode = new TestNode(step.getAction(), NodeType.TEST_STEP);
                stepNode.setStepRef(step);

                TreeItem<TestNode> stepItem = new TreeItem<>(
                        stepNode,
                        makeIcon("/icons/Step.png", 14, 14)
                );
                scenarioItem.getChildren().add(stepItem);
            }
        }

        return scenarioItem;
    }

    private boolean isValidDrop(TreeItem<TestNode> dragged, TreeItem<TestNode> target) {
        if (dragged == null || target == null) {
            log.info("Drop rejected: dragged or target is null");
            return false;
        }

        NodeType dType = dragged.getValue().getType();
        NodeType tType = target.getValue().getType();

        boolean allowed = switch (dType) {
            case SUITE -> (tType == NodeType.ROOT);
            case SUB_SUITE -> (tType == NodeType.SUITE);
            case TEST_SCENARIO -> (tType == NodeType.SUITE || tType == NodeType.SUB_SUITE);
            default -> false;
        };

        log.info(String.format("Drag type=%s, Target type=%s, Allowed=%s", dType, tType, allowed));
        return allowed;
    }


    private ImageView makeIcon(String path, double width, double height) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            System.err.println("Icon not found: " + path);
            return new ImageView(); // fallback
        }
        ImageView icon = new ImageView(new Image(resource.toExternalForm()));
        icon.setFitWidth(width);
        icon.setFitHeight(height);
        icon.setPreserveRatio(true);
        return icon;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Action");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

    @FXML
    private void handleNewSuite(ActionEvent event) {
        TreeItem<TestNode> root = treeView.getRoot();
        if (root != null) {
            // Backing model
            TestSuite suiteModel = new TestSuite("Suite");

            // Wrap in TestNode
            TestNode node = new TestNode("Suite", NodeType.SUITE);
            node.setSuiteRef(suiteModel);

            // TreeItem
            TreeItem<TestNode> newSuite = new TreeItem<>(node);
            newSuite.setExpanded(true);

            // Attach to root
            root.getChildren().add(newSuite);

            updateExecutionPreview("Created new suite: " + suiteModel.getName() +
                    " (id=" + suiteModel.getId() + ")");
        }
    }


    @FXML
    private void handleNewSubSuite(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.SUITE) {
            // Backing model
            TestSuite subSuiteModel = new TestSuite("Sub-Suite");

            // Wrap in TestNode
            TestNode node = new TestNode("Sub-Suite", NodeType.SUB_SUITE);
            node.setSuiteRef(subSuiteModel);

            // TreeItem
            TreeItem<TestNode> subSuiteItem = new TreeItem<>(node);
            subSuiteItem.setExpanded(true);

            // Attach to parent suite
            selected.getChildren().add(subSuiteItem);

            updateExecutionPreview("Created new sub-suite: " + subSuiteModel.getName() +
                    " (id=" + subSuiteModel.getId() + ")");
        } else {
            showError("Sub-Suite can only be added inside a Suite.");
        }
    }


    @FXML
    private void handleNewTestScenario(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null &&
                (selected.getValue().getType() == NodeType.SUITE ||
                        selected.getValue().getType() == NodeType.SUB_SUITE)) {

            TextInputDialog dialog = new TextInputDialog("Scenario");
            dialog.setTitle("New Scenario");
            dialog.setHeaderText("Create a new TestScenario");
            dialog.setContentText("Enter Scenario name:");

            dialog.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    // Backing model
                    TestScenario scenarioModel = new TestScenario(trimmed);

                    // Wrap in TestNode
                    TestNode node = new TestNode(trimmed, NodeType.TEST_SCENARIO);
                    node.setScenarioRef(scenarioModel);

                    TreeItem<TestNode> scenarioItem = new TreeItem<>(node);
                    scenarioItem.setExpanded(true);

                    selected.getChildren().add(scenarioItem);

                    scenarioSteps.put(scenarioModel.getId(),
                            FXCollections.observableArrayList(scenarioModel.getSteps().stream()
                                    .map(TestStep::new)
                                    .toList()));
                    scenarioColumns.put(scenarioModel.getId(), List.of());

                    if (treeView.getSelectionModel().getSelectedItem() == scenarioItem) {
                        tableView.setItems(scenarioSteps.get(scenarioModel.getId()));
                    }

                    updateExecutionPreview("Created new scenario: " + scenarioModel.getName() +
                            " (id=" + scenarioModel.getId() + ")");
                } else {
                    showError("Name cannot be empty.");
                }
            });
        } else {
            showError("Scenario can only be added inside a Suite or Sub-Suite.");
        }
    }


    @FXML
    private void handleDelete(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getParent() != null) {
            NodeType type = selected.getValue().getType();
            String name = selected.getValue().getName();

            // Confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText("Delete " + type);
            confirm.setContentText("Are you sure you want to delete: " + name + "?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // Remove from tree
                    selected.getParent().getChildren().remove(selected);

                    if (type == NodeType.TEST_SCENARIO) {
                        TestScenario scenario = selected.getValue().getScenarioRef();
                        if (scenario != null) {
                            // ✅ Clean up per-scenario state
                            scenarioSteps.remove(scenario.getId());
                            scenarioColumns.remove(scenario.getId());
                        }

                        tableView.getItems().clear();
                        updateExecutionPreview("Deleted scenario: " + name);

                    } else if (type == NodeType.SUITE || type == NodeType.SUB_SUITE) {
                        TestSuite suite = selected.getValue().getSuiteRef();
                        if (suite != null) {
                            // ✅ Clean up all child scenarios inside this suite/sub-suite
                            for (TestScenario scenario : suite.getScenarios()) {
                                scenarioSteps.remove(scenario.getId());
                                scenarioColumns.remove(scenario.getId());
                            }
                        }

                        updateExecutionPreview("Deleted " + type + ": " + name);

                    } else {
                        updateExecutionPreview("Deleted: " + name);
                    }
                }
            });
        } else {
            showError("No item selected to delete.");
        }
    }


    @FXML
    private void handleSave(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
            saveTestScenario(selected);
            updateExecutionPreview("Saved scenario: " + selected.getValue().getName());
        }
        // Always export project when saving
        saveProject();
        updateExecutionPreview("Project saved successfully.");
    }

    private void saveTestScenario(TreeItem<TestNode> scenarioItem) {
        TestNode node = scenarioItem.getValue();
        TestScenario scenario = node.getScenarioRef();
        if (scenario == null) {
            showError("No scenario model attached to this node.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Scenario");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File file = fileChooser.showSaveDialog(treeView.getScene().getWindow());

            if (file != null) {
                // Example: serialize steps to JSON
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (Writer writer = new FileWriter(file)) {
                    gson.toJson(scenario.getSteps(), writer);
                }
                updateExecutionPreview("Scenario saved to: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Error saving scenario: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddRow(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();

        // Guard: only allow adding when a TEST_SCENARIO is selected
        if (selected == null || selected.getValue().getType() != NodeType.TEST_SCENARIO) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Add Step");
            alert.setHeaderText(null);
            alert.setContentText("⚠️ Please select a Test Scenario before adding a step.");
            alert.show();
            return;
        }

        // Create and add new step
        TestStep newStep = new TestStep();
        tableView.getItems().add(newStep);

        // Commit to scenario model
        TestScenario scenario = selected.getValue().getScenarioRef();
        if (scenario != null) {
            scenario.getSteps().add(new TestStep(newStep)); // deep copy
        }
    }


    @FXML
    private void handleDeleteRow(ActionEvent event) {
        TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
        if (selectedStep != null) {
            int index = tableView.getSelectionModel().getSelectedIndex();
            tableView.getItems().remove(index);

            // Immediately commit to scenario model
            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
                TestScenario scenario = selected.getValue().getScenarioRef();
                if (scenario != null) {
                    scenario.getSteps().removeIf(s -> s.getId().equals(selectedStep.getId()));
                }
            }
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

    private void writeScenarioSnapshotToFile(TestScenario scenario,
                                             List<TestStep> steps,
                                             String phase,
                                             String timestamp) {
        File logDir = new File("logs");
        if (!logDir.exists() && !logDir.mkdirs()) {
            log.severe("Failed to create log directory: " + logDir.getAbsolutePath());
            return;
        }

        // Rotate daily: include date in filename
        String date = java.time.LocalDate.now().toString(); // e.g. 2026-01-22
        File outFile = new File(logDir, "scenario_" + scenario.getId() + "_" + date + ".csv");

        boolean newFile = !outFile.exists();

        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile, true))) {
            // Write header only once per file
            if (newFile) {
                pw.println("phase,timestamp,row,stepId,item,object,action,description,input,args,maxArgs");
            }

            for (int i = 0; i < steps.size(); i++) {
                final int rowIndex = i + 1;
                final TestStep step = steps.get(i);

                if (step == null) {
                    pw.printf("%s,%s,%d,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>%n",
                            phase, timestamp, rowIndex);
                    continue;
                }

                pw.printf("%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%d%n",
                        phase,
                        timestamp,
                        rowIndex,
                        quoteCsv(safeString(step.getId())),
                        quoteCsv(safeString(step.getItem())),
                        quoteCsv(safeString(step.getObject())),
                        quoteCsv(safeString(step.getAction())),
                        quoteCsv(safeString(step.getDescription())),
                        quoteCsv(safeString(step.getInput())),
                        quoteCsv(step.getArgs().toString()),
                        step.getMaxArgs());
            }
        } catch (IOException e) {
            log.severe("Failed to write snapshot file: " + e.getMessage());
        }
    }

    // Helper to quote CSV fields that may contain commas or quotes
    private String quoteCsv(String value) {
        if (value == null) return "<empty>";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }


    private void logStepChange(String type, TestStep step, Map<String, Object> details) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(": row=").append(getIndexSafe(step))
                .append(", stepId=").append(step.getId());

        details.forEach((k, v) -> sb.append(", ").append(k).append("=").append(v));

        log.info(sb::toString);
    }


    // Utility to safely get row index
    private int getIndexSafe(TestStep step) {
        return (tableView.getItems().contains(step))
                ? tableView.getItems().indexOf(step) + 1
                : -1;
    }


    private void logScenarioSnapshot(String phase,
                                     TestScenario scenario,
                                     List<TestStep> steps,
                                     String timestamp) {
        log.info(() -> String.format(
                "=== %s snapshot for scenario '%s' (id=%s) at %s ===",
                phase,
                scenario.getName(),
                scenario.getId(),
                timestamp
        ));

        for (int i = 0; i < steps.size(); i++) {
            final int rowIndex = i + 1;
            final TestStep step = steps.get(i);

            if (step == null) {
                log.fine(() -> String.format("row=%d skipped (null step)", rowIndex));
                continue;
            }

            // Only log meaningful rows (object/action/item not blank)
            boolean hasContent =
                    (step.getObject() != null && !step.getObject().isBlank()) ||
                            (step.getAction() != null && !step.getAction().isBlank()) ||
                            (step.getItem() != null && !step.getItem().isBlank());

            if (hasContent) {
                log.info(() -> String.format(
                        "row=%d, stepId=%s, item=%s, object=%s, action=%s, description=%s, input=%s, args=%s, maxArgs=%d",
                        rowIndex,
                        step.getId(),
                        safeString(step.getItem()),
                        safeString(step.getObject()),
                        safeString(step.getAction()),
                        safeString(step.getDescription()),
                        safeString(step.getInput()),
                        step.getArgs(),
                        step.getMaxArgs()
                ));
            } else {
                log.fine(() -> String.format("row=%d skipped (empty step)", rowIndex));
            }
        }

        log.info(() -> String.format("=== End %s snapshot ===", phase));
    }

    // Helper to avoid nulls in file output
    private String safeString(String value) {
        return (value == null || value.isBlank()) ? "<empty>" : value;
    }


    // --- New static helper ---
    public static void markTableDirty() {
        tableDirty = true;
    }

    // --- Optional getter if you want to check from outside ---
    public static boolean isTableDirty() {
        return tableDirty;
    }

    // --- Optional reset after commit ---
    public static void resetTableDirty() {
        tableDirty = false;
    }


}