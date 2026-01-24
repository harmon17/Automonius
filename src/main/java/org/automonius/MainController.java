package org.automonius;

import java.io.*;
import java.net.URL;
import java.util.*;

import com.google.gson.*;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.automonius.Annotations.ActionMeta;
import org.automonius.Model.VariableTreeController;
import org.automonius.exec.TestCase;
import org.automonius.exec.TestExecutor;

import java.lang.reflect.Method;

import javafx.scene.control.TextArea;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;



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
    @FXML
//    private TextArea executionPreviewArea;
    private static final Logger log = Logger.getLogger(MainController.class.getName());
    @FXML
    private CheckBox showStepsToggle;
    private static boolean tableDirty = false;
    private final Map<String, List<String>> argsByObject = new HashMap<>();
    private final Map<String, Integer> maxArgsByObject = new HashMap<>();
    private final ObservableList<TestStep> steps = FXCollections.observableArrayList();
    private TreeItem<TestNode> draggedItem;
    @FXML
    private TableColumn<TestStep, String> typeColumn;
    @FXML
    private TableColumn<TestStep, String> statusColumn;
    @FXML
    private ChoiceBox<String> environmentChoice;
    @FXML
    private ChoiceBox<String> entityChoice;
    @FXML
    private ListView<String> resolvedVariableList;
    @FXML
    private TextArea resolvedPayloadArea;
    private final Map<String, Map<String, String>> contextVariables = new HashMap<>();
    private final Map<String, String> contextPayloads = new HashMap<>();
    @FXML
    private TextArea lastPayloadArea;
    private final Map<String, List<String>> argsByAction = new HashMap<>();   // action → inputs
    private final Map<String, List<String>> actionsByObject = new HashMap<>();
    @FXML private TextFlow executionPreviewFlow;




    @FXML
    public void initialize() {
        // --- Discover actions ---
        Map<String, TestCase> discovered = TestExecutor.discoverActionsByAction("org.automonius.Actions");
        log.info("Discovered actions: " + discovered.keySet());

        // Reset caches
        argsByObject.clear();
        actionsByObject.clear();
        argsByAction.clear();

        // Populate maps from discovery
        for (Map.Entry<String, TestCase> entry : discovered.entrySet()) {
            String actionName = entry.getKey();
            TestCase tc = entry.getValue();
            String objectName = tc.getObjectName();
            List<String> inputs = tc.getInputs();

            argsByAction.put(actionName, new ArrayList<>(inputs));
            actionsByObject.computeIfAbsent(objectName, k -> new ArrayList<>()).add(actionName);
            argsByObject.computeIfAbsent(objectName, k -> new ArrayList<>()).addAll(inputs);
        }

        // --- Initialize first step defaults ---
        if (!steps.isEmpty()) {
            String defaultObject = argsByObject.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (defaultObject != null) {
                String defaultAction = actionsByObject.getOrDefault(defaultObject, List.of())
                        .stream().findFirst().orElse(null);

                List<String> defaultArgs = argsByObject.getOrDefault(defaultObject, List.of());

                TestStep blank = steps.get(0);
                blank.setObject(defaultObject);
                blank.setAction(defaultAction);
                blank.setExtras(defaultArgs.stream()
                        .collect(Collectors.toMap(
                                arg -> arg,
                                arg -> new SimpleStringProperty(""),
                                (a, b) -> a,
                                LinkedHashMap::new
                        )));
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

        // --- Context menu ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copySuiteItem = new MenuItem("Copy Suite");
        copySuiteItem.setOnAction(e -> { /* keep original copy suite logic */ });
        MenuItem copyScenarioItem = new MenuItem("Copy Scenario");
        copyScenarioItem.setOnAction(e -> { /* keep original copy scenario logic */ });
        contextMenu.getItems().addAll(copySuiteItem, copyScenarioItem);
        treeView.setContextMenu(contextMenu);

        // --- Drag and Drop setup ---
        treeView.setOnDragDetected(event -> { /* keep original drag logic */ });
        treeView.setOnDragOver(event -> { /* keep original drag logic */ });
        treeView.setOnDragDropped(event -> { /* keep original drop logic */ });

        // --- Toggle listener ---
        showStepsToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> { /* keep original toggle logic */ });

        // --- TreeView cell factory with icons ---
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(TestNode item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    textProperty().unbind();
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    textProperty().unbind(); // avoid stale bindings
                    setStyle("");

                    switch (item.getType()) {
                        case ROOT -> {
                            setText(item.getName());
                            setGraphic(makeIcon("/icons/bank.png", 16, 16));
                        }
                        case SUITE -> {
                            TestSuite suite = item.getSuiteRef();
                            if (suite != null) {
                                textProperty().bind(suite.nameProperty());
                                suite.nameProperty().addListener((obs, oldVal, newVal) ->
                                        log.info(() -> "TreeView label updated for suite " + suite.getId()
                                                + ": " + oldVal + " -> " + newVal));
                            }
                            setGraphic(makeIcon("/icons/MainSuite.png", 16, 16));
                        }
                        case SUB_SUITE -> {
                            TestSuite subSuite = item.getSuiteRef();
                            if (subSuite != null) {
                                textProperty().bind(subSuite.nameProperty());
                                subSuite.nameProperty().addListener((obs, oldVal, newVal) ->
                                        log.info(() -> "TreeView label updated for sub-suite " + subSuite.getId()
                                                + ": " + oldVal + " -> " + newVal));
                            }
                            setGraphic(makeIcon("/icons/SubSuite.png", 16, 16));
                        }
                        case TEST_SCENARIO -> {
                            TestScenario scenario = item.getScenarioRef();
                            if (scenario != null) {
                                textProperty().bind(scenario.nameProperty());
                                scenario.nameProperty().addListener((obs, oldVal, newVal) ->
                                        log.info(() -> "TreeView label updated for scenario " + scenario.getId()
                                                + ": " + oldVal + " -> " + newVal));
                            }
                            setGraphic(makeIcon("/icons/TestSuite.png", 16, 16));
                        }
                        case TEST_STEP -> {
                            setGraphic(makeIcon("/icons/Step.png", 14, 14));

                            TestStep step = item.getStepRef();
                            String stepName = item.getName();
                            String status = step != null ? step.getStatus() : "";

                            if (status == null || status.isBlank()) {
                                // Pending / not run yet
                                setText(stepName == null || stepName.isBlank() ? "<empty step>" : stepName);
                                setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                            } else if ("PASS".equalsIgnoreCase(status)) {
                                setText(stepName);
                                setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                log.info(() -> "TreeView styled PASS for step " + step.getId());
                            } else if ("FAIL".equalsIgnoreCase(status)) {
                                setText(stepName);
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                log.info(() -> "TreeView styled FAIL for step " + step.getId());
                            } else {
                                // Any other status (e.g. SKIPPED, ERROR)
                                setText(stepName);
                                setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                log.info(() -> "TreeView styled " + status + " for step " + step.getId());
                            }
                        }

                    }
                }
            }
        });

        // --- Selection listener ---
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            // ✅ Keep commit snapshot logging + UUID assignment
            if (oldItem != null && oldItem.getValue().getType() == NodeType.TEST_SCENARIO && MainController.isTableDirty()) {
                TestScenario oldScenario = oldItem.getValue().getScenarioRef();
                if (oldScenario != null) {
                    ObservableList<TestStep> committedSteps = FXCollections.observableArrayList();
                    for (TestStep step : tableView.getItems()) {
                        committedSteps.add(new TestStep(step)); // deep copy includes extras
                    }
                    oldScenario.getSteps().setAll(committedSteps);
                    scenarioSteps.put(oldScenario.getId(), committedSteps);
                    logScenarioSnapshot("Commit", oldScenario, committedSteps, java.time.LocalDateTime.now().toString());
                    writeScenarioSnapshotToFile(oldScenario, committedSteps, "Commit", java.time.LocalDateTime.now().toString());
                }
                MainController.resetTableDirty();
            }

            if (newItem != null && newItem.getValue().getType() == NodeType.TEST_SCENARIO) {
                TestScenario newScenario = newItem.getValue().getScenarioRef();
                if (newScenario != null) {
                    ObservableList<TestStep> stepsCopy = FXCollections.observableArrayList(
                            newScenario.getSteps().stream().map(TestStep::new).toList()
                    );
                    scenarioSteps.put(newScenario.getId(), stepsCopy);
                    tableView.setItems(stepsCopy);
                    adjustArgsColumnWidth();
                    refreshScenarioUI(newScenario); // 🔄 sync ListView
                    logScenarioSnapshot("Load", newScenario, stepsCopy, java.time.LocalDateTime.now().toString());
                    writeScenarioSnapshotToFile(newScenario, stepsCopy, "Load", java.time.LocalDateTime.now().toString());
                }
            }
        });

        // --- ListView setup ---
        resolvedVariableList.setEditable(true);
        resolvedVariableList.setCellFactory(TextFieldListCell.forListView(new DefaultStringConverter()));
        resolvedVariableList.setPlaceholder(new Label("No arguments discovered"));
        // ✅ Keep search + filter logic
        // ✅ Keep edit commit logic (updates contextVariables + step extras)

        // --- TableView setup (structural only) ---
        tableView.setEditable(true);
        tableView.getColumns().setAll(
                itemColumn,
                objectColumn,
                actionColumn,
                typeColumn,
                descriptionColumn,
                statusColumn
        );

// Item column → centered numbering
        itemColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(tableView.getItems().indexOf(cellData.getValue()) + 1))
        );
        itemColumn.setEditable(false);
        itemColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
            }
        });

// Object column → ComboBox with auto‑assign + refresh
        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setEditable(true);
        objectColumn.setCellFactory(col -> buildObjectComboCell()); // your helper we refactored

// Action column → ComboBox with auto‑assign + refresh
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setEditable(true);
        actionColumn.setCellFactory(col -> buildActionComboCell()); // your helper we refactored

// Type column → classification, read‑only
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setEditable(false);
        typeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #555; -fx-font-style: italic;");
            }
        });

// Description column → editable text field with auto‑commit
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(col -> new AutoCommitTextFieldTableCell<>());
        descriptionColumn.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            tableDirty = true;
            log.info(() -> "Edited description for step: " + event.getRowValue());
        });

// Status column → execution results, styled
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setEditable(false);
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PASS".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if ("FAIL".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #333;");
                    }
                }
            }
        });

        // --- Row-level styling based on step status ---
        // --- TableView row factory: selection + status styling ---
        tableView.setRowFactory(tv -> {
            TableRow<TestStep> row = new TableRow<>() {
                @Override
                protected void updateItem(TestStep step, boolean empty) {
                    super.updateItem(step, empty);

                    if (empty || step == null) {
                        setStyle("");
                    } else {
                        String status = step.getStatus();
                        if ("PASS".equalsIgnoreCase(status)) {
                            setStyle("-fx-background-color: #e6ffe6; -fx-text-fill: green; -fx-font-weight: bold;");
                        } else if ("FAIL".equalsIgnoreCase(status)) {
                            setStyle("-fx-background-color: #ffe6e6; -fx-text-fill: red; -fx-font-weight: bold;");
                        } else if (status == null || status.isBlank() || "PENDING".equalsIgnoreCase(status)) {
                            setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: gray; -fx-font-style: italic;");
                        } else {
                            setStyle("-fx-background-color: #fff5e6; -fx-text-fill: orange; -fx-font-weight: bold;");
                        }
                    }
                }
            };

            // Force selection when clicking anywhere in the row
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getItem());
                }
            });

            return row;
        });

// --- ListView cell factory: editable args + commit on focus loss + highlight ---
        resolvedVariableList.setCellFactory(list -> new TextFieldListCell<>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setEditable(false);
                    return;
                }
                if (!item.trim().startsWith("--")) {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2a2a2a;");
                    setEditable(false);
                } else {
                    setText(item);
                    setStyle("");
                    setEditable(true);
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    // Commit when focus leaves the text field
                    tf.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            commitEdit(tf.getText());
                        }
                    });
                }
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);

                // 🔥 Highlight effect after commit
                setStyle("-fx-background-color: #d6f5ff; -fx-font-weight: bold;");

                // Optional fade back after 2 seconds
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> setStyle(""));
                pause.play();
            }
        });




    }



    @FXML
    private void handleDeleteColumn(ActionEvent event) {
        // Only allow deletion of non-default columns
        int defaultColumnCount = 4; // item, object, action, description
        if (tableView.getColumns().size() > defaultColumnCount) {
            tableView.getColumns().remove(tableView.getColumns().size() - 1);
        } else {
            showError("Default columns cannot be deleted.");
        }
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

                // ✅ Collect all extras across steps
                Set<String> allExtras = scenario.getSteps().stream()
                        .flatMap(s -> s.getExtras().keySet().stream())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                // ✅ Build header row
                Row header = sheet.createRow(rowIndex[0]++);
                int colIndex = 0;
                header.createCell(colIndex++).setCellValue("Item");
                header.createCell(colIndex++).setCellValue("Object");
                header.createCell(colIndex++).setCellValue("Action");
                header.createCell(colIndex++).setCellValue("Description");
                header.createCell(colIndex++).setCellValue("Input");
                for (String colName : allExtras) {
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

                    for (String colName : allExtras) {
                        String value = step.getExtras().containsKey(colName)
                                ? step.getExtra(colName)
                                : "";
                        stepRow.createCell(colIndex++).setCellValue(value);
                    }
                }
            }
        }

        // Recurse into children
        for (TreeItem<TestNode> child : node.getChildren()) {
            saveScenariosRecursive(sheet, child, rowIndex);
        }
    }



    @FXML
    private void handleRun(ActionEvent event) {
        // End any active cell editing before running
        if (tableView.getEditingCell() != null) {
            tableView.edit(-1, null);
        }

        // Get the selected step, or default to the first one
        TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
        if (selectedStep == null && !tableView.getItems().isEmpty()) {
            tableView.getSelectionModel().selectFirst();
            selectedStep = tableView.getSelectionModel().getSelectedItem();
        }

        if (selectedStep == null) {
            updateExecutionPreview("⚠️ No step selected to run.");
            return;
        }

        // Show annotation inputs and extras for debugging
        String[] inputs = getInputsForAction(selectedStep.getAction());
        updateExecutionPreview("Annotation input names: " + Arrays.toString(inputs));
        updateExecutionPreview("Extras map: " + selectedStep.getExtras());

        // Run the test step
        Object resultObj = TestExecutor.runTest(selectedStep);

        // Build a safe log map: include result and extras, skipping nulls
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("result", resultObj);
        selectedStep.getExtras().forEach((k, v) -> {
            if (v != null) {
                logData.put(k, v.get() == null ? "" : v.get());
            }
        });

        // Update preview and log
        String message = "Ran step: " + selectedStep.getAction() +
                " on " + selectedStep.getObject() +
                " → Result: " + resultObj;
        updateExecutionPreview(message);

        logStepChange("Run", selectedStep, logData);
    }


    public static TestCase getTestByAction(Class<?> clazz, String actionName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = method.getAnnotation(ActionMeta.class);
                if (method.getName().equals(actionName)) {
                    // ✅ Pass inputs as a List<String> and include declaring class
                    return new TestCase(
                            meta.objectName(),
                            method.getName(),
                            meta.description(),
                            Arrays.asList(meta.inputs()),
                            clazz.getName()   // NEW fifth argument
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

        // Look up all discovered actions under the package root, keyed by action
        Map<String, TestCase> actionsByAction = TestExecutor.discoverActionsByAction("org.automonius.Actions");

        // Find the matching TestCase by action name
        TestCase tc = actionsByAction.get(step.getAction());
        if (tc != null) {
            // Use the annotation metadata
            List<String> inputs = tc.getInputs();
            TreeItem<TestNode> scenario = treeView.getSelectionModel().getSelectedItem();
            if (scenario != null && scenario.getValue().getType() == NodeType.TEST_SCENARIO) {
                for (String inputName : inputs) {
                    boolean exists = tableView.getColumns().stream()
                            .anyMatch(c -> c.getText().equals(inputName));
                    if (!exists) {
                        addColumnForScenario(scenario, inputName);
                    }
                }
            }
        }
    }


    private void rebuildArgumentColumns(List<String> persistedArgNames) {
        // Reset to static columns
        tableView.getColumns().setAll(
                itemColumn,
                objectColumn,
                actionColumn,
                typeColumn,        // keep if you want classification
                descriptionColumn,
                statusColumn       // new column for execution results
                // dynamic extras columns will be added later per scenario
        );


        List<String> argNames = (persistedArgNames != null && !persistedArgNames.isEmpty())
                ? persistedArgNames
                : List.of();

        if (argNames.isEmpty()) {
            log.info("Rebuilding dynamic columns skipped: no persisted headers for current scenario.");
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

                    // ✅ Always bind to property — no need to check containsKey

                    SimpleStringProperty newProp = step.getExtraProperty(key);
                    textField.textProperty().bindBidirectional(newProp);
                    currentProp = newProp;

                    setGraphic(stack);
                }

            });

            tableView.getColumns().add(col);
        }

        tableView.refresh();
    }


    private String[] getInputsForAction(String actionName) {
        // Scan all actions under the package root, keyed by action
        Map<String, TestCase> actionsByAction = TestExecutor.discoverActionsByAction("org.automonius.Actions");

        // Find the matching TestCase by action name
        TestCase tc = actionsByAction.get(actionName);
        if (tc != null) {
            // Preserve declared order
            return tc.getInputs().stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .toArray(new String[0]);
        }

        return new String[0]; // fallback if not found
    }




    private void updateExecutionPreview(String message) {
        if (executionPreviewFlow != null) {
            Text logLine = new Text("Preview: " + message + "\n");
            logLine.setFill(Color.GRAY); // default color
            executionPreviewFlow.getChildren().add(logLine);
        }
        log.info(() -> "Execution preview updated: " + message);
    }



    @FXML
    private void handleRunScenario(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().getType() != NodeType.TEST_SCENARIO) {
            updateExecutionPreview("⚠️ Please select a Test Scenario to run.");
            return;
        }

        TestScenario scenario = selected.getValue().getScenarioRef();
        if (scenario == null) {
            updateExecutionPreview("No scenario linked to this node.");
            return;
        }

        // Start message
        updateExecutionPreview("▶ Running scenario '" + scenario.getName() + "' in Test environment...");

        int rowIndex = 1;
        for (TestStep step : scenario.getSteps()) {
            try {
                Object resultObj = TestExecutor.runTest(step);

                // Append each step result into preview pane with numbering
                updateExecutionPreview("Row " + rowIndex + ": " + step.getAction() +
                        " on " + step.getObject() +
                        " → Result: " + (resultObj != null ? resultObj.toString() : "<null>"));

            } catch (Exception ex) {
                updateExecutionPreview("Row " + rowIndex + ": " + step.getAction() +
                        " on " + step.getObject() +
                        " → ERROR: " + ex.getMessage());
                log.log(Level.SEVERE, "Error running step " + rowIndex, ex);
            }
            rowIndex++;
        }

        // Finish message
        updateExecutionPreview("✔ Finished running scenario '" + scenario.getName() + "'");
    }


    private void commitTableEditsToScenario(TestScenario scenario) {
        scenario.getSteps().clear();

        for (TestStep edited : tableView.getItems()) {
            TestStep copy = new TestStep(
                    edited.getItem(),
                    edited.getAction(),
                    edited.getObject(),
                    edited.getInput()
            );
            copy.setDescription(edited.getDescription());

            // Always rebuild extras from global argsByAction
            List<String> args = argsByAction.getOrDefault(copy.getAction(), List.of());
            Map<String, SimpleStringProperty> extras = args.stream()
                    .collect(Collectors.toMap(
                            arg -> arg,
                            arg -> new SimpleStringProperty(
                                    edited.getExtras() != null && edited.getExtras().containsKey(arg)
                                            ? edited.getExtras().get(arg).get()
                                            : ""
                            ),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            copy.setExtras(extras);
            copy.setMaxArgs(args.size());

            scenario.getSteps().add(copy);
        }

        log.info(() -> "Committed edits back to scenario " + scenario.getId() +
                " with " + scenario.getSteps().size() + " steps.");
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
            List<String> extraNames = new ArrayList<>();
            for (int i = 5; i < colHeaderRow.getLastCellNum(); i++) {
                extraNames.add(colHeaderRow.getCell(i).getStringCellValue());
            }

            // Build scenario-level extras as Map<String, SimpleStringProperty>
            Map<String, SimpleStringProperty> extrasMap = extraNames.stream()
                    .collect(Collectors.toMap(
                            name -> name,
                            name -> new SimpleStringProperty(""),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            scenario.setExtras(extrasMap);

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

                // Fill extras for this step
                for (int i = 0; i < extraNames.size(); i++) {
                    String extraName = extraNames.get(i);
                    String extraVal = row.getCell(5 + i).getStringCellValue();
                    step.setExtra(extraName, extraVal); // assumes TestStep.setExtra(name, value) updates the property
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
                    List<String> extraNames = new ArrayList<>();
                    for (int i = 5; i < colHeaderRow.getLastCellNum(); i++) {
                        extraNames.add(colHeaderRow.getCell(i).getStringCellValue());
                    }

                    // Build scenario-level extras as Map<String, SimpleStringProperty>
                    Map<String, SimpleStringProperty> extrasMap = extraNames.stream()
                            .collect(Collectors.toMap(
                                    name -> name,
                                    name -> new SimpleStringProperty(""),
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ));
                    scenario.setExtras(extrasMap);

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

                        // Fill extras for this step
                        for (int i = 0; i < extraNames.size(); i++) {
                            String extraName = extraNames.get(i);
                            String extraVal = stepRow.getCell(5 + i).getStringCellValue();
                            step.setExtra(extraName, extraVal); // assumes TestStep.setExtra(name, value) updates the property
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
        String suiteName = (suite.getName() != null && !suite.getName().isBlank())
                ? suite.getName()
                : "Unnamed Suite";

        TestNode suiteNode = new TestNode(suiteName, NodeType.SUITE);
        suiteNode.setSuiteRef(suite);
        TreeItem<TestNode> suiteItem = new TreeItem<>(suiteNode);
        suiteItem.setExpanded(true);

        for (TestScenario scenario : suite.getScenarios()) {
            String scenarioName = (scenario.getName() != null && !scenario.getName().isBlank())
                    ? scenario.getName()
                    : "Unnamed Scenario";

            TestNode scenarioNode = new TestNode(scenarioName, NodeType.TEST_SCENARIO);
            scenarioNode.setScenarioRef(scenario);
            TreeItem<TestNode> scenarioItem = new TreeItem<>(scenarioNode);
            scenarioItem.setExpanded(true);

            // ✅ Toggle decides if steps are shown
            if (showStepsToggle != null && showStepsToggle.isSelected()) {
                int rowIndex = 1;
                for (TestStep step : scenario.getSteps()) {
                    String stepName = (step.getDescription() != null && !step.getDescription().isBlank())
                            ? step.getDescription()
                            : step.getAction();

                    // Add row numbering for clarity
                    String displayName = "Row " + rowIndex + ": " + (stepName != null ? stepName : "Unnamed Step");

                    TestNode stepNode = new TestNode(displayName, NodeType.TEST_STEP);
                    stepNode.setStepRef(step);
                    scenarioItem.getChildren().add(new TreeItem<>(stepNode));

                    rowIndex++;
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
        // Use the new constructor so the blank step is seeded with defaults
        TestScenario copy = new TestScenario(original.getName() + " Copy", argsByObject, actionsByObject);
        copy.getSteps().clear(); // remove the seeded blank
        for (TestStep step : original.getSteps()) {
            copy.addStep(new TestStep(step));
        }

        // Copy extras list
        Map<String, SimpleStringProperty> extrasCopy = new LinkedHashMap<>();
        original.getExtras().forEach((key, prop) -> {
            extrasCopy.put(key, new SimpleStringProperty(prop.get())); // deep copy each property
        });
        copy.setExtras(extrasCopy);


        return copy;
    }


    private TreeItem<TestNode> buildScenarioNode(TestScenario scenario) {
        // Create a TestNode wrapper for the scenario
        String scenarioName = (scenario.getName() != null && !scenario.getName().isBlank())
                ? scenario.getName()
                : "Unnamed Scenario";

        TestNode node = new TestNode(scenarioName, NodeType.TEST_SCENARIO);
        node.setScenarioRef(scenario);

        // Create the TreeItem with the correct icon
        TreeItem<TestNode> scenarioItem = new TreeItem<>(
                node,
                makeIcon("/icons/Scenario.png", 16, 16) // use a scenario-specific icon
        );
        scenarioItem.setExpanded(true);

        // Add step children if "showStepsToggle" is enabled
        if (showStepsToggle.isSelected()) {
            int rowIndex = 1;
            for (TestStep step : scenario.getSteps()) {
                String stepName;
                if (step.getDescription() != null && !step.getDescription().isBlank()) {
                    stepName = step.getDescription();
                } else if (step.getAction() != null && !step.getAction().isBlank()) {
                    stepName = step.getAction();
                } else {
                    stepName = "Unnamed Step";
                }

                // Add row numbering for clarity
                String displayName = "Row " + rowIndex + ": " + stepName;

                TestNode stepNode = new TestNode(displayName, NodeType.TEST_STEP);
                stepNode.setStepRef(step);

                TreeItem<TestNode> stepItem = new TreeItem<>(
                        stepNode,
                        makeIcon("/icons/Step.png", 14, 14)
                );
                scenarioItem.getChildren().add(stepItem);

                rowIndex++;
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
                    // Backing model with seeded extras
                    TestScenario scenarioModel = new TestScenario(trimmed, argsByObject, actionsByObject);

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
                    scenarioColumns.put(scenarioModel.getId(), new ArrayList<>(scenarioModel.getSteps().get(0).getExtras().keySet()));

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
        if (selected == null || selected.getValue().getType() != NodeType.TEST_SCENARIO) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Add Step");
            alert.setHeaderText(null);
            alert.setContentText("⚠️ Please select a Test Scenario before adding a step.");
            alert.show();
            return;
        }

        TestStep newStep = new TestStep();
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            // Copy from currently selected step
            TestStep currentStep = tableView.getItems().get(selectedIndex);
            newStep.setObject(currentStep.getObject());
            newStep.setAction(currentStep.getAction());
            newStep.setExtras(copyExtras(currentStep.getExtras()));
            newStep.setMaxArgs(currentStep.getMaxArgs());
        } else {
            // Context-aware defaults
            TestScenario scenario = selected.getValue().getScenarioRef();

            // Prefer the last step in this scenario as a template
            if (!scenario.getSteps().isEmpty()) {
                TestStep lastStep = scenario.getSteps().get(scenario.getSteps().size() - 1);
                newStep.setObject(lastStep.getObject());
                newStep.setAction(lastStep.getAction());
                newStep.setExtras(copyExtras(lastStep.getExtras()));
                newStep.setMaxArgs(lastStep.getMaxArgs());
            } else {
                // Fall back to global discovery maps
                String defaultObject = actionsByObject.keySet().stream().findFirst().orElse("");
                newStep.setObject(defaultObject);

                String defaultAction = actionsByObject.getOrDefault(defaultObject, List.of())
                        .stream().findFirst().orElse("");
                newStep.setAction(defaultAction);

                List<String> defaultArgs = argsByObject.getOrDefault(defaultObject, List.of());
                Map<String, SimpleStringProperty> extras = defaultArgs.stream()
                        .collect(Collectors.toMap(
                                arg -> arg,
                                arg -> new SimpleStringProperty(""),
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));
                newStep.setExtras(extras);
                newStep.setMaxArgs(defaultArgs.size());
            }
        }

        tableView.getItems().add(newStep);

        TestScenario scenario = selected.getValue().getScenarioRef();
        if (scenario != null) {
            scenario.getSteps().add(new TestStep(newStep)); // deep copy
            refreshScenarioUI(scenario);
        }

        tableDirty = true;
        log.info(() -> "Added new step to scenario " + scenario.getId() + ": " + newStep);
    }

    // Helper to copy extras
    private Map<String, SimpleStringProperty> copyExtras(Map<String, SimpleStringProperty> original) {
        return original.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new SimpleStringProperty(e.getValue().get()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
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

    private void adjustArgsColumnWidth() {
        for (TableColumn<TestStep, ?> col : tableView.getColumns()) {
            if ("Dynamic".equals(col.getUserData())) {
                // You can calculate width dynamically based on header text length:
                double width = Math.max(80, col.getText().length() * 10);
                col.setPrefWidth(width);
            }
        }
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

    @FXML
    private void handleRefresh() {
        log.info("[REFRESH] Starting full rescan...");

        argsByObject.clear();
        actionsByObject.clear();
        argsByAction.clear();

        Map<String, TestCase> discovered = TestExecutor.discoverActionsByAction("org.automonius.Actions");
        for (Map.Entry<String, TestCase> entry : discovered.entrySet()) {
            String actionName = entry.getKey();
            TestCase tc = entry.getValue();
            argsByAction.put(actionName, new ArrayList<>(tc.getInputs()));
            actionsByObject.computeIfAbsent(tc.getObjectName(), k -> new ArrayList<>()).add(actionName);
            argsByObject.put(tc.getObjectName(), new ArrayList<>(tc.getInputs()));
        }

        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
            TestScenario scenario = selected.getValue().getScenarioRef();
            if (scenario != null) {
                refreshScenarioUI(scenario); // 🔄 unified sync
                updateExecutionPreview("Refreshed scenario: " + scenario.getName() +
                        " (id=" + scenario.getId() + ")");
            }
        }

        log.info("[REFRESH] Completed rescan. TableView + ListView updated.");
    }


    private void safeguardScenario(TestScenario scenario) {
        // Detect instability: extras size mismatch vs maxArgs
        boolean unstable = scenario.getSteps().stream()
                .anyMatch(step -> step.getExtras().size() != step.getMaxArgs());

        if (unstable) {
            log.warning("[SAFEGUARD] Detected schema drift, pausing and autosaving...");

            // Pause editing while we stabilize
            tableView.setEditable(false);

            // Deep copy current steps into scenario
            ObservableList<TestStep> committedSteps = FXCollections.observableArrayList();
            for (TestStep step : tableView.getItems()) {
                committedSteps.add(new TestStep(step));
            }
            scenario.getSteps().setAll(committedSteps);
            scenarioSteps.put(scenario.getId(), committedSteps);

            // Snapshot current dynamic headers
            List<String> argNamesForScenario = tableView.getColumns().stream()
                    .filter(c -> "Dynamic".equals(c.getUserData()))
                    .map(TableColumn::getText)
                    .toList();
            scenarioColumns.put(scenario.getId(), argNamesForScenario);

            // Feedback to tester
            updateExecutionPreview("Autosave triggered for scenario: " + scenario.getName());

            // Resume editing
            tableView.setEditable(true);
            tableView.refresh();
        }
    }

    private String getCurrentContextKey() {
        String env = environmentChoice.getValue();
        String entity = entityChoice.getValue();
        return (env != null && entity != null) ? env + ":" + entity : "";
    }

    private String resolvePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }

        Map<String, String> vars = contextVariables.get(getCurrentContextKey());
        if (vars == null || vars.isEmpty()) {
            return payload;
        }

        StringBuilder resolved = new StringBuilder(payload);

        for (Map.Entry<String, String> e : vars.entrySet()) {
            String placeholder = "{" + e.getKey() + "}";
            String value = e.getValue() != null ? e.getValue() : "";
            int index;
            // Replace all occurrences safely
            while ((index = resolved.indexOf(placeholder)) != -1) {
                resolved.replace(index, index + placeholder.length(), value);
            }
        }

        return resolved.toString();
    }


    private void runStep(TestStep step) {
        String rawPayload = contextPayloads.getOrDefault(getCurrentContextKey(), "");
        String resolved = resolvePayload(rawPayload);

        lastPayloadArea.setText(resolved); // show payload

        // Running step log (blue)
        Text runLine = new Text("Running step: " + step.getAction() + "\n");
        runLine.setFill(Color.DODGERBLUE);
        runLine.setStyle("-fx-font-weight: bold;");
        executionPreviewFlow.getChildren().add(runLine);

        Object result = TestExecutor.runTest(step);

        // Result log (green for PASS, red for FAIL, gray otherwise)
        Text resultLine = new Text("Result: " + result + "\n\n");
        if ("PASS".equalsIgnoreCase(String.valueOf(result))) {
            resultLine.setFill(Color.GREEN);
            resultLine.setStyle("-fx-font-weight: bold;");
        } else if ("FAIL".equalsIgnoreCase(String.valueOf(result))) {
            resultLine.setFill(Color.RED);
            resultLine.setStyle("-fx-font-weight: bold;");
        } else {
            resultLine.setFill(Color.GRAY);
        }
        executionPreviewFlow.getChildren().add(resultLine);
    }


    @FXML
    private void handleRun() {
        TreeItem<TestNode> selectedNode = treeView.getSelectionModel().getSelectedItem();
        if (selectedNode == null || selectedNode.getValue().getType() != NodeType.TEST_SCENARIO) {
            updateExecutionPreview("⚠️ Please select a Test Scenario to run.");
            return;
        }

        TestScenario scenario = selectedNode.getValue().getScenarioRef();
        if (scenario == null) {
            updateExecutionPreview("No scenario linked to this node.");
            return;
        }

        runScenario(scenario); // always run the whole scenario
    }



    private void runScenario(TestScenario scenario) {
        updateExecutionPreview("▶ Running scenario '" + scenario.getName() + "'...");

        int rowIndex = 1;
        for (TestStep step : scenario.getSteps()) {
            runStepWithLogging(step, rowIndex);
            rowIndex++;
        }

        updateExecutionPreview("✔ Finished running scenario '" + scenario.getName() + "'");
    }

    private void runStepWithLogging(TestStep step, int rowIndex) {
        try {
            Object resultObj = TestExecutor.runTest(step);

            updateExecutionPreview("Row " + rowIndex + ": " + step.getAction() +
                    " on " + step.getObject() +
                    " → Result: " + (resultObj != null ? resultObj.toString() : "<null>"));

            // 🔄 Sync updated extras back into context variables
            step.getExtras().forEach((key, prop) -> {
                contextVariables
                        .computeIfAbsent(getCurrentContextKey(), k -> new LinkedHashMap<>())
                        .put(key, prop.get());
            });

        } catch (Exception ex) {
            updateExecutionPreview("Row " + rowIndex + ": " + step.getAction() +
                    " on " + step.getObject() +
                    " → ERROR: " + ex.getMessage());
            log.log(Level.SEVERE, "Error running step " + rowIndex, ex);
        }
    }



    @FXML
    private void handleEditPayload() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Payload");
        dialog.setHeaderText("Edit JSON/XML Payload for current context");

        TextArea editor = new TextArea();
        editor.setWrapText(true);
        editor.setPrefRowCount(20);

        // Load existing payload if available
        String currentPayload = contextPayloads.getOrDefault(getCurrentContextKey(), "");
        editor.setText(currentPayload);

        // Autocomplete on Ctrl+Space
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
                List<String> vars = getAvailableVariables();
                if (!vars.isEmpty()) {
                    ContextMenu menu = new ContextMenu();
                    for (String var : vars) {
                        MenuItem item = new MenuItem(var);
                        item.setOnAction(e -> {
                            int caret = editor.getCaretPosition();
                            editor.insertText(caret, var);
                        });
                        menu.getItems().add(item);
                    }
                    menu.show(editor, Side.BOTTOM, 0, 0);
                }
                event.consume();
            }
        });

        dialog.getDialogPane().setContent(editor);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // ✅ Make the dialog resizable
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefSize(800, 600);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return editor.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(payload -> {
            contextPayloads.put(getCurrentContextKey(), payload);
            resolvedPayloadArea.setText(payload);
        });
    }


    // Helper: list available variables for autocomplete
    private List<String> getAvailableVariables() {
        Map<String, String> vars = contextVariables.get(getCurrentContextKey());
        return vars != null
                ? vars.keySet().stream().map(name -> "{" + name + "}").toList()
                : List.of();
    }


    private void syncScenarioToContext(TestScenario scenario) {
        String contextKey = getCurrentContextKey();

        List<String> groupedItems = new ArrayList<>();
        int rowIndex = 1;

        // 🔄 Use live TableView items
        for (TestStep step : tableView.getItems()) {
            String action = step.getAction();
            if (action == null || action.isBlank()) {
                rowIndex++;
                continue;
            }

            // Add header with row numbering
            groupedItems.add("Row " + rowIndex + ": " + action);

            // Ensure extras are rebuilt from global catalog
            List<String> args = argsByAction.getOrDefault(action, List.of());
            Map<String, SimpleStringProperty> existingExtras = step.getExtras() != null ? step.getExtras() : Map.of();
            Map<String, SimpleStringProperty> extras = args.stream()
                    .collect(Collectors.toMap(
                            arg -> arg,
                            arg -> existingExtras.containsKey(arg)
                                    ? existingExtras.get(arg)
                                    : new SimpleStringProperty(""),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            step.setExtras(extras);
            step.setMaxArgs(args.size());

            // Add extras under this action
            extras.forEach((key, prop) -> groupedItems.add("--" + key + "=" + prop.get()));

            rowIndex++;
        }

        // Snapshot for payload resolution
        Map<String, String> vars = tableView.getItems().stream()
                .flatMap(s -> s.getExtras().entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
        contextVariables.put(contextKey, vars);

        // Show grouped list
        resolvedVariableList.setItems(FXCollections.observableArrayList(groupedItems));
        resolvedVariableList.setPlaceholder(new Label("No arguments discovered"));

        // Log load with row numbering
        vars.forEach((name, value) -> log.info(() -> String.format(
                "[SYNC LOAD] scenario=%s, var=%s, value=%s",
                scenario.getId(), name, value
        )));

        // Cell factory: headers bold, args editable
        resolvedVariableList.setCellFactory(list -> new TextFieldListCell<>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setEditable(false);
                    return;
                }
                if (!item.trim().startsWith("--")) {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2a2a2a;");
                    setEditable(false);
                } else {
                    setText(item);
                    setStyle("");
                    setEditable(true);
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                if (getGraphic() instanceof TextField tf) {
                    // Commit when focus leaves the text field
                    tf.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            commitEdit(tf.getText());
                        }
                    });
                }
            }
        });


        // Handle edits only for args
        resolvedVariableList.setOnEditCommit(event -> {
            String newValue = event.getNewValue();
            if (!newValue.trim().startsWith("--")) return;

            String[] parts = newValue.trim().substring(2).split("=", 2);
            if (parts.length == 2) {
                String varName = parts[0];
                String varValue = parts[1];

                Map<String, String> ctxVars = contextVariables.get(contextKey);
                if (ctxVars != null) ctxVars.put(varName, varValue);

                TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
                if (selectedStep != null) {
                    selectedStep.getExtraProperty(varName).set(varValue);
                }

                resolvedVariableList.getItems().set(event.getIndex(), newValue);

                // 🔥 Color‑coded log line
                logExecutionEvent("ARG", "step=" +
                        (selectedStep != null ? selectedStep.getId() : "unknown") +
                        ", var=" + varName + ", value=" + varValue);

                // Refresh payload preview
                StringBuilder payloadBuilder = new StringBuilder();
                if (selectedStep != null) {
                    selectedStep.getExtras().forEach((k, v) -> {
                        payloadBuilder.append("--").append(k).append("=")
                                .append(v.get()).append("\n");
                    });
                }
                resolvedPayloadArea.setText(payloadBuilder.toString().trim());
            }
        });





    }


    private void refreshScenarioUI(TestScenario scenario) {
        // Rebuild extras for each step in the live table
        for (TestStep step : tableView.getItems()) {
            List<String> args = argsByAction.getOrDefault(step.getAction(), List.of());
            Map<String, SimpleStringProperty> existingExtras = step.getExtras() != null ? step.getExtras() : Map.of();
            Map<String, SimpleStringProperty> extras = args.stream()
                    .collect(Collectors.toMap(
                            arg -> arg,
                            arg -> existingExtras.containsKey(arg)
                                    ? existingExtras.get(arg)
                                    : new SimpleStringProperty(""),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            step.setExtras(extras);
            step.setMaxArgs(args.size());
        }

        // Safeguard + sync ListView
        safeguardScenario(scenario);
        syncScenarioToContext(scenario);
    }
    private TableCell<TestStep, String> buildObjectComboCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                TestStep step = getTableRow().getItem();
                List<String> availableObjects = new ArrayList<>(argsByObject.keySet());

                ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(availableObjects));
                combo.setDisable(availableObjects.isEmpty());

                combo.setValue(step.getObject() != null && !step.getObject().isBlank()
                        ? step.getObject()
                        : (!availableObjects.isEmpty() ? availableObjects.get(0) : null));

                combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        log.info("[OBJECT CHANGE] Row=" + getIndex() + " → " + newVal);
                        step.setObject(newVal);

                        List<String> methods = actionsByObject.getOrDefault(newVal, List.of());
                        step.setAction(!methods.isEmpty() ? methods.get(0) : null);

                        tableDirty = true;
                        tableView.refresh();

                        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
                            TestScenario scenario = selected.getValue().getScenarioRef();
                            if (scenario != null) {
                                refreshScenarioUI(scenario);
                            }
                        }
                    }
                });

                setGraphic(combo);
            }
        };
    }
    private TableCell<TestStep, String> buildActionComboCell() {
        return new TableCell<>() {
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

                combo.setValue(step.getAction() != null && !step.getAction().isBlank()
                        ? step.getAction()
                        : (!availableActions.isEmpty() ? availableActions.get(0) : null));

                combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        log.info("[ACTION CHANGE] Row=" + getIndex() + " → " + newVal);
                        step.setAction(newVal);

                        tableDirty = true;
                        tableView.refresh();

                        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
                        if (selected != null && selected.getValue().getType() == NodeType.TEST_SCENARIO) {
                            TestScenario scenario = selected.getValue().getScenarioRef();
                            if (scenario != null) {
                                refreshScenarioUI(scenario);
                            }
                        }
                    }
                });

                setGraphic(combo);
            }
        };
    }

// Replace executionPreviewArea (TextArea) with a TextFlow in your FXML:
// <TextFlow fx:id="executionPreviewFlow" />

    private void logExecutionEvent(String type, String message) {
        Text logLine = new Text();

        switch (type.toUpperCase()) {
            case "PASS":
                logLine.setFill(Color.GREEN);
                logLine.setStyle("-fx-font-weight: bold;");
                logLine.setText("✔ PASS → " + message + "\n");
                break;
            case "FAIL":
                logLine.setFill(Color.RED);
                logLine.setStyle("-fx-font-weight: bold;");
                logLine.setText("✘ FAIL → " + message + "\n");
                break;
            case "ARG":
                logLine.setFill(Color.DODGERBLUE);
                logLine.setStyle("-fx-font-weight: bold;");
                logLine.setText("✎ Arg updated → " + message + "\n");
                break;
            default:
                logLine.setFill(Color.GRAY);
                logLine.setText(message + "\n");
        }

        executionPreviewFlow.getChildren().add(logLine);
    }



}