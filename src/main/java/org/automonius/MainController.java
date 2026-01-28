package org.automonius;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

import com.google.gson.*;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.automonius.Annotations.ActionMeta;
import org.automonius.Model.VariableTreeController;
import org.automonius.exec.TestCase;
import org.automonius.exec.TestExecutor;

import java.lang.reflect.Method;

import javafx.scene.control.TextArea;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

import static org.automonius.TestStep.toStringMap;

import org.apache.poi.ss.usermodel.Cell;

import java.util.prefs.Preferences;


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
    @FXML
    private TextFlow executionPreviewFlow;
    private static TestStep copiedStep;
    // Track the active project file globally
    private File currentProjectFile;
    @FXML
        private TableColumn<TestStep, Boolean> selectColumn;
    // --- Global args storage ---
    private final Map<String, StringProperty> globalArgs = new LinkedHashMap<>();





    private Stage primaryStage; // Setter called from Main.start()

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }


    @FXML
    public void initialize() {
        log.info("MainController.initialize() called — setting up UI");

        // --- 1. Discover and prepare data ---
        discoverActions();       // find available actions
        initializeDefaults();    // set default object/action/args

        // --- 2. Build core UI structure ---
        setupTreeView();         // TreeView root + structure
        loadTree(treeView);      // load saved project tree
        setupTableView();        // TableView columns + row factory
        setupListView();         // ListView for extras/args

        // --- 3. Wire up interactions ---
        setupClipboard();        // copy/paste shortcuts
        setupTreeContextMenu();  // right-click menu for suites/scenarios
        setupTableContextMenu(); // right-click menu for steps
        enableDragAndDrop(treeView); // drag/drop in tree

        // --- 4. Add listeners/toggles ---
        setupToggleListener();   // show/hide steps toggle
        setupSelectionListener();// sync TreeView ↔ TableView ↔ ListView

        log.info("MainController.initialize() finished — TreeView root=" +
                (treeView.getRoot() != null ? treeView.getRoot().getValue().getName() : "null"));
    }



    // --- Discover actions ---
    private void discoverActions() {
        Map<String, TestCase> discovered = TestExecutor.discoverActionsByAction("org.automonius.Actions");
        log.info("Discovered actions: " + discovered.keySet());

        argsByObject.clear();
        actionsByObject.clear();
        argsByAction.clear();

        for (Map.Entry<String, TestCase> entry : discovered.entrySet()) {
            String actionName = entry.getKey();
            TestCase tc = entry.getValue();
            String objectName = tc.getObjectName();
            List<String> inputs = tc.getInputs();

            argsByAction.put(actionName, new ArrayList<>(inputs));
            actionsByObject.computeIfAbsent(objectName, k -> new ArrayList<>()).add(actionName);
            argsByObject.computeIfAbsent(objectName, k -> new ArrayList<>()).addAll(inputs);
        }
    }

    // --- Initialize first step defaults ---
    private void initializeDefaults() {
        if (!steps.isEmpty()) {
            // ✅ Ensure the very first step has a UUID
            TestStep blank = steps.get(0);
            if (blank.getId() == null || blank.getId().isBlank()) {
                TestStep copyWithId = TestStep.deepCopy(blank);
                steps.set(0, copyWithId);
                blank = copyWithId;
            }

            String defaultObject = argsByObject.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (defaultObject != null) {
                String defaultAction = actionsByObject.getOrDefault(defaultObject, List.of())
                        .stream().findFirst().orElse(null);

                List<String> defaultArgs = argsByObject.getOrDefault(defaultObject, List.of());

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
                        ", args=" + defaultArgs +
                        ", id=" + blank.getId()); // ✅ log ID for confirmation
            }
        }
    }


    // --- TreeView setup ---
    private void setupTreeView() {
        TreeItem<TestNode> root = new TreeItem<>(new TestNode("Directory Structure", NodeType.ROOT));
        root.setExpanded(true);
        treeView.setRoot(root);

        // 🔥 Ensure root is visible
        treeView.setShowRoot(true);

        log.info("TreeView initialized with root: " + root.getValue().getName());
    }


    // --- Clipboard setup ---
    private void setupClipboard() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent clipboardContent = new ClipboardContent();

        // Keyboard shortcuts for TreeView copy/paste
        treeView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // 🚫 Block Enter from triggering default TreeView behavior
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                return;
            }

            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                handleCopy(selected, clipboardContent);
                saveTree(treeView.getRoot());
                event.consume();
            }

            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                String data = Clipboard.getSystemClipboard().getString();
                if (data != null) {
                    handlePaste(selected, data);
                    saveTree(treeView.getRoot());
                    event.consume();
                }
            }
        });

        // Keyboard shortcuts for TableView copy/paste
        tableView.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.C) {
                handleCopyStep(new ActionEvent());
                event.consume();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                handlePasteStep(new ActionEvent());
                event.consume();
            }
        });
    }



    public void setupInitialProject() {
        Preferences prefs = Preferences.userNodeForPackage(MainController.class);
        String lastPath = prefs.get("lastProjectPath", null);

        if (lastPath != null) {
            File lastFile = new File(lastPath);
            if (lastFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Reopen last project: " + lastFile.getName() + "?",
                        ButtonType.YES, ButtonType.NO);
                alert.setHeaderText(null);
                alert.initOwner(primaryStage);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    loadTreeFromFile(lastFile);
                    log.info("Restored last project from " + lastFile.getAbsolutePath());
                    return;
                }
                // If NO → fall through to FileChooser
            }
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Project File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showOpenDialog(primaryStage);

        if (file != null) {
            loadTreeFromFile(file);
            prefs.put("lastProjectPath", file.getAbsolutePath());
            log.info("User selected project: " + file.getAbsolutePath());
        } else {
            // fallback: build default suite
            if (treeView.getRoot() != null && treeView.getRoot().getChildren().isEmpty()) {
                TestSuite suiteModel = new TestSuite("Suite");
                TestNode node = new TestNode("Suite", NodeType.SUITE);
                node.setSuiteRef(suiteModel);

                TreeItem<TestNode> suiteItem = new TreeItem<>(node);
                suiteItem.setExpanded(true);
                treeView.getRoot().getChildren().add(suiteItem);

                saveTree(treeView.getRoot());
                log.info("Initialized with Default Suite under Directory Structure");
            }
        }
    }



    // --- TreeView context menu ---
    private void setupTreeContextMenu() {
        ContextMenu treeMenu = new ContextMenu();

        // --- Add Suite under root ---
        MenuItem addSuiteItem = new MenuItem("Add Suite");
        addSuiteItem.setOnAction(this::handleNewSuite);

        // --- Add Sub-Suite under Suite/Sub-Suite ---
        MenuItem addSubSuiteItem = new MenuItem("Add Sub-Suite");
        addSubSuiteItem.setOnAction(this::handleNewSubSuite);

        // --- Add Scenario under Suite/Sub-Suite ---
        MenuItem addScenarioItem = new MenuItem("Add Scenario");
        addScenarioItem.setOnAction(this::handleNewTestScenario);

        // --- Paste (Suite/Scenario copy-paste logic) ---
        MenuItem pasteMenuItem = new MenuItem("Paste");
        pasteMenuItem.setOnAction(e -> {
            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            String data = Clipboard.getSystemClipboard().getString();
            if (data != null && !data.isBlank()) {
                handlePaste(selected, data);
                saveTree(treeView.getRoot());
            }
        });

        // --- Rename (Suite, SubSuite, Scenario) ---

        MenuItem renameMenuItem = new MenuItem("Rename");
        renameMenuItem.setOnAction(event -> {
            TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // ✅ This will show the TextInputDialog and handle renaming
                handleRenameNode(selected);
            }
        });
// Disable rename only for Root if needed
        renameMenuItem.setDisable(false);


        // --- Delete node ---
        MenuItem deleteNodeItem = new MenuItem("Delete");
        deleteNodeItem.setOnAction(this::handleDelete);

        // --- Attach all items ---
        treeMenu.getItems().addAll(
                addSuiteItem,
                addSubSuiteItem,
                addScenarioItem,
                pasteMenuItem,
                renameMenuItem,
                deleteNodeItem
        );

        treeView.setContextMenu(treeMenu);
    }


    // --- TableView context menu ---
    private void setupTableContextMenu() {
        ContextMenu stepMenu = new ContextMenu();

        MenuItem copyStepItem = new MenuItem("Copy Step");
        copyStepItem.setOnAction(e -> handleCopyStep(new ActionEvent()));

        MenuItem pasteStepItem = new MenuItem("Paste Step");
        pasteStepItem.setOnAction(e -> handlePasteStep(new ActionEvent()));

        stepMenu.getItems().addAll(copyStepItem, pasteStepItem);
        tableView.setContextMenu(stepMenu);
    }

    // --- Global toggle listener ---
    private void setupToggleListener() {
        showStepsToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (treeView.getRoot() == null) return;

            // Iterate through all top-level suites
            for (TreeItem<TestNode> suiteItem : treeView.getRoot().getChildren()) {
                // Iterate through all scenarios inside each suite
                for (TreeItem<TestNode> scenarioItem : suiteItem.getChildren()) {
                    if (scenarioItem.getValue().getType() == NodeType.TEST_SCENARIO) {
                        TestScenario scenario = scenarioItem.getValue().getScenarioRef();
                        if (scenario != null) {
                            applyStepToggle(scenarioItem, scenario);
                        }
                    }
                }
            }
        });
    }

    // --- Selection listener ---
    private void setupSelectionListener() {
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            // --- Commit edits when leaving a scenario ---
            if (oldItem != null
                    && oldItem.getValue().getType() == NodeType.TEST_SCENARIO
                    && MainController.isTableDirty()) {

                commitActiveEdits(); // flush edits before snapshot

                TestScenario oldScenario = oldItem.getValue().getScenarioRef();
                if (oldScenario != null) {
                    // 🔥 Push global args into all steps before snapshot
                    applyGlobalArgsToSteps(tableView.getItems());

                    ObservableList<TestStep> committedSteps = FXCollections.observableArrayList();
                    for (TestStep step : tableView.getItems()) {
                        // ✅ use deepCopy to preserve or assign UUID
                        committedSteps.add(TestStep.deepCopy(step));
                    }
                    oldScenario.getSteps().setAll(committedSteps);
                    scenarioSteps.put(oldScenario.getId(), committedSteps);

                    String timestamp = java.time.LocalDateTime.now().toString();
                    logScenarioSnapshot("Commit", oldScenario, committedSteps, timestamp);
                    writeScenarioSnapshotToFile(oldScenario, committedSteps, "Commit", timestamp);
                }
                MainController.resetTableDirty();
            }

            // --- Load steps when entering a new scenario ---
            if (newItem != null && newItem.getValue().getType() == NodeType.TEST_SCENARIO) {
                TestScenario newScenario = newItem.getValue().getScenarioRef();
                if (newScenario != null) {
                    ObservableList<TestStep> stepsCopy = FXCollections.observableArrayList(
                            newScenario.getSteps().stream()
                                    .map(s -> (s.getId() == null || s.getId().isBlank())
                                            ? TestStep.deepCopy(s) // ✅ assign UUID if missing
                                            : s)
                                    .toList()
                    );
                    scenarioSteps.put(newScenario.getId(), stepsCopy);
                    tableView.setItems(stepsCopy);

                    // ✅ Apply global args immediately after load
                    applyGlobalArgsToSteps(stepsCopy);

                    // ✅ Force row rendering and selection so UUIDs exist immediately
                    tableView.refresh();
                    if (!stepsCopy.isEmpty()) {
                        tableView.getSelectionModel().selectFirst();
                    }

                    adjustArgsColumnWidth();
                    refreshScenarioUI(newScenario);

                    String timestamp = java.time.LocalDateTime.now().toString();
                    logScenarioSnapshot("Load", newScenario, stepsCopy, timestamp);
                    writeScenarioSnapshotToFile(newScenario, stepsCopy, "Load", timestamp);

                    // 🔥 Apply toggle state to tree children
                    applyStepToggle(newItem, newScenario);
                }
            }
        });
    }


    // --- TableView setup (structural only) ---
    private void setupTableView() {
        tableView.setEditable(true);

        // --- Define column order ---
        tableView.getColumns().setAll(
                selectColumn,     // ✅ new checkbox column for multi-run
                itemColumn,       // step numbering
                objectColumn,     // object reference
                actionColumn,     // action reference
                typeColumn,       // classification
                descriptionColumn,// editable description
                statusColumn      // execution status
        );

        // --- Initialize each column ---
        setupSelectColumn();     // checkbox column
        setupItemColumn();       // numbering
        setupObjectColumn();     // object ComboBox
        setupActionColumn();     // action ComboBox
        setupTypeColumn();       // type styling
        setupDescriptionColumn();// editable description
        setupStatusColumn();     // PASS/FAIL styling
        setupRowFactory();       // row styling + context menu
    }


    // --- Item column → centered numbering ---
    private void setupItemColumn() {
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
    }

    // --- Object column → ComboBox with auto‑assign + refresh ---
    private void setupObjectColumn() {
        objectColumn.setCellValueFactory(new PropertyValueFactory<>("object"));
        objectColumn.setEditable(true);
        objectColumn.setCellFactory(col -> buildObjectComboCell()); // your helper
    }

    // --- Action column → ComboBox with auto‑assign + refresh ---
    private void setupActionColumn() {
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionColumn.setEditable(true);
        actionColumn.setCellFactory(col -> buildActionComboCell()); // your helper
    }

    // --- Type column → classification, read‑only ---
    private void setupTypeColumn() {
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
    }

    // --- Description column → editable text field with auto‑commit ---
    private void setupDescriptionColumn() {
        descriptionColumn.setCellFactory(col -> new TableCell<TestStep, String>() {
            private final TextField textField = new TextField();

            @Override
            protected void updateItem(String desc, boolean empty) {
                super.updateItem(desc, empty);

                if (empty || desc == null) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    textField.setText(desc);
                    setGraphic(textField);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(desc);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();
                textField.setText(getItem());
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                getTableView().getItems().get(getIndex()).setDescription(newValue);
                tableDirty = true;
                log.info(() -> "Edited description for step: " + getTableView().getItems().get(getIndex()));
            }
        });
    }

    // --- Status column → execution results, styled ---
    private void setupStatusColumn() {
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
    }

    // --- TableView row factory: selection + status styling ---
    private void setupRowFactory() {
        tableView.setRowFactory(tv -> {
            TableRow<TestStep> row = new TableRow<>() {
                @Override
                protected void updateItem(TestStep step, boolean empty) {
                    super.updateItem(step, empty);

                    if (empty || step == null) {
                        setStyle("");
                        setGraphic(null);
                        setContextMenu(null);
                    } else {
                        // ✅ Ensure UUID is assigned immediately
                        TestStep effectiveStep = step;
                        if (effectiveStep.getId() == null || effectiveStep.getId().isBlank()) {
                            effectiveStep = TestStep.deepCopy(effectiveStep);
                            tableView.getItems().set(getIndex(), effectiveStep);
                        }

                        // --- Existing status styling ---
                        if (effectiveStep.isNew()) {
                            setStyle("-fx-background-color: #d6f5ff; -fx-font-weight: bold;");
                        } else {
                            String status = effectiveStep.getStatus();
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

                        // --- Context menu for right‑click ---
                        ContextMenu menu = new ContextMenu();
                        MenuItem runItem = new MenuItem("Run Step");
                        TestStep finalStep = effectiveStep; // ✅ final for lambda
                        runItem.setOnAction(e -> {
                            tableView.getSelectionModel().select(finalStep); // ensure selection
                            handleRun(new ActionEvent());                     // reuse your existing run logic
                        });
                        menu.getItems().add(runItem);

                        setContextMenu(menu);
                    }
                }
            };

            // Left‑click still selects row
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getItem());
                }
            });

            return row;
        });
    }



    private void setupListView() {
        resolvedVariableList.setEditable(true);
        resolvedVariableList.setPlaceholder(new Label("No arguments discovered"));

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
                    tf.textProperty().addListener((obs, oldVal, newVal) -> {
                        if (getItem() != null && getItem().startsWith("--")) {
                            String key = getItem().substring(2);

                            // 🔥 Update globalArgs
                            StringProperty prop = globalArgs.computeIfAbsent(key, k -> new SimpleStringProperty(""));
                            prop.set(newVal);

                            // ✅ Ensure a TableView row is selected so UUID assignment runs
                            TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
                            if (selectedStep == null && !tableView.getItems().isEmpty()) {
                                tableView.getSelectionModel().selectFirst();
                                selectedStep = tableView.getSelectionModel().getSelectedItem();
                            }

                            // ✅ Guarantee an ID
                            String stepId = (selectedStep != null && selectedStep.getId() != null && !selectedStep.getId().isBlank())
                                    ? selectedStep.getId()
                                    : "global";

                            // Log and preview updates
                            log.info(() -> "Edited global arg=" + key + ", newValue=" + newVal);
                            updateExecutionPreview("Arg updated → step=" + stepId + ", var=" + key + ", value=" + newVal);

                            // 🔥 Show full globalArgs state
                            String dump = globalArgs.entrySet().stream()
                                    .map(e -> e.getKey() + "=" + e.getValue().get())
                                    .collect(Collectors.joining(", "));
                            updateExecutionPreview("Global args state → [" + dump + "]");
                            log.info(() -> "Global args snapshot: [" + dump + "]");

                            // 🎨 Visual feedback
                            setStyle("-fx-background-color: #d6f5ff; -fx-font-weight: bold;");
                            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                            pause.setOnFinished(e -> setStyle(""));
                            pause.play();
                        }
                    });
                }
            }
        });
    }


    @FXML
    private void handleDelete(ActionEvent event) {
        TreeItem<TestNode> selectedNode = treeView.getSelectionModel().getSelectedItem();

        // --- Case 1: Delete a step from the TableView ---
        if (tableView.isFocused()) {
            TestStep selectedStep = tableView.getSelectionModel().getSelectedItem();
            if (selectedStep != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Delete");
                confirm.setHeaderText("Delete Step");
                confirm.setContentText("Are you sure you want to delete this step?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        tableView.getItems().remove(selectedStep);

                        TestScenario scenario = selectedNode != null ? selectedNode.getValue().getScenarioRef() : null;
                        if (scenario != null) {
                            // Deep copy back into scenario
                            ObservableList<TestStep> committedSteps = FXCollections.observableArrayList();
                            for (TestStep step : tableView.getItems()) {
                                committedSteps.add(new TestStep(step));
                            }
                            scenario.getSteps().setAll(committedSteps);
                            scenarioSteps.put(scenario.getId(), committedSteps);

                            log.info(() -> "Deleted step: " + selectedStep);
                            updateExecutionPreview("Deleted step from scenario: " + scenario.getName());

                            // ✅ Persist immediately
                            saveTree(treeView.getRoot());
                        }
                    }
                });
            }
            return;
        }

        // --- Case 2: Delete a node from the TreeView ---
        if (selectedNode == null) {
            showError("⚠️ Please select a Suite, SubSuite, or Scenario to delete.");
            return;
        }

        NodeType type = selectedNode.getValue().getType();
        String name = selectedNode.getValue().getName();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + type);
        confirm.setContentText("Are you sure you want to delete: " + name + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                switch (type) {
                    case SUITE -> {
                        treeView.getRoot().getChildren().remove(selectedNode);
                        TestSuite suite = selectedNode.getValue().getSuiteRef();
                        if (suite != null) {
                            for (TestScenario scenario : suite.getScenarios()) {
                                scenarioSteps.remove(scenario.getId());
                                scenarioColumns.remove(scenario.getId());
                            }
                        }
                        updateExecutionPreview("Deleted Suite: " + name);
                        log.info(() -> "Deleted Suite: " + name);
                    }
                    case SUB_SUITE -> {
                        TreeItem<TestNode> parent = selectedNode.getParent();
                        if (parent != null) {
                            parent.getChildren().remove(selectedNode);
                            TestSuite suite = selectedNode.getValue().getSuiteRef();
                            if (suite != null) {
                                for (TestScenario scenario : suite.getScenarios()) {
                                    scenarioSteps.remove(scenario.getId());
                                    scenarioColumns.remove(scenario.getId());
                                }
                            }
                            updateExecutionPreview("Deleted SubSuite: " + name);
                            log.info(() -> "Deleted SubSuite: " + name);
                        }
                    }
                    case TEST_SCENARIO -> {
                        TreeItem<TestNode> parent = selectedNode.getParent();
                        if (parent != null) {
                            parent.getChildren().remove(selectedNode);
                        }
                        TestScenario scenario = selectedNode.getValue().getScenarioRef();
                        if (scenario != null) {
                            scenarioSteps.remove(scenario.getId());
                            scenarioColumns.remove(scenario.getId());
                        }
                        tableView.getItems().clear();
                        updateExecutionPreview("Deleted Scenario: " + name);
                        log.info(() -> "Deleted Scenario: " + name);
                    }
                    default -> showError("⚠️ Unsupported node type for deletion.");
                }

                // ✅ Persist tree changes
                saveTree(treeView.getRoot());
            }
        });
    }


    private String makeKey(TreeItem<TestNode> scenario) {
        return scenario.getValue().getId(); // use UUID, not name path
    }


    private void saveProject() {
        File projectDir = new File(System.getProperty("user.home"), "AutomoniusProject");
        if (!projectDir.exists() && !projectDir.mkdirs()) {
            showError("Failed to create project directory: " + projectDir.getAbsolutePath());
            return;
        }

        for (TreeItem<TestNode> suiteItem : treeView.getRoot().getChildren()) {
            TestNode suiteNode = suiteItem.getValue();
            if (suiteNode.getType() == NodeType.SUITE && suiteNode.getSuiteRef() != null) {
                try (Workbook workbook = new XSSFWorkbook()) {
                    // 🔥 Use node name for sheet name
                    String safeSheetName = suiteNode.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (safeSheetName.length() > 31) {
                        safeSheetName = safeSheetName.substring(0, 31);
                    }
                    Sheet sheet = workbook.createSheet(safeSheetName);

                    AtomicInteger rowIndex = new AtomicInteger(0);

                    // Export scenarios + steps with polished styling
                    saveScenariosRecursive(sheet, suiteItem, rowIndex, workbook);

                    // Auto-size all columns
                    if (sheet.getRow(0) != null) {
                        int lastCol = sheet.getRow(0).getLastCellNum();
                        for (int i = 0; i < lastCol; i++) {
                            sheet.autoSizeColumn(i);
                        }
                    }

                    // Freeze header row for readability
                    sheet.createFreezePane(0, 1);

                    // 🔥 Use node name for file name
                    String safeFileName = suiteNode.getName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".xlsx";
                    File outFile = new File(projectDir, safeFileName);

                    try (FileOutputStream out = new FileOutputStream(outFile)) {
                        workbook.write(out);
                        updateExecutionPreview("Saved suite: " + suiteNode.getName() +
                                " (id=" + suiteNode.getId() + ") → " + outFile.getName());
                    }
                } catch (IOException e) {
                    showError("Failed to save suite " + suiteNode.getName() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Project saved to " + projectDir.getAbsolutePath());
    }

    // 🔎 Helper: Bold, colored style for scenario headers
    private CellStyle createScenarioHeaderStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.LEFT);

        return headerStyle;
    }

    // 🔎 Helper: Zebra striping for step rows
    private CellStyle createStepRowStyle(Workbook workbook, boolean evenRow) {
        CellStyle style = workbook.createCellStyle();
        if (evenRow) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        } else {
            style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // 🔎 Recursive scenario export with styling
    private void saveScenariosRecursive(Sheet sheet, TreeItem<TestNode> suiteItem,
                                        AtomicInteger rowIndex, Workbook workbook) {
        CellStyle headerStyle = createScenarioHeaderStyle(workbook);

        for (TreeItem<TestNode> scenarioItem : suiteItem.getChildren()) {
            TestNode scenarioNode = scenarioItem.getValue();
            if (scenarioNode.getType() == NodeType.TEST_SCENARIO && scenarioNode.getScenarioRef() != null) {
                TestScenario scenario = scenarioNode.getScenarioRef();

                // Scenario header row
                Row headerRow = sheet.createRow(rowIndex.getAndIncrement());
                Cell headerCell = headerRow.createCell(0);
                headerCell.setCellValue("Scenario: " + scenario.getName());
                headerCell.setCellStyle(headerStyle);

                // Step rows
                for (TestStep step : scenario.getSteps()) {
                    Row stepRow = sheet.createRow(rowIndex.getAndIncrement());
                    int colIndex = 0;
                    stepRow.createCell(colIndex++).setCellValue(step.getItem());
                    stepRow.createCell(colIndex++).setCellValue(step.getObject());
                    stepRow.createCell(colIndex++).setCellValue(step.getAction());
                    stepRow.createCell(colIndex++).setCellValue(step.getDescription());
                    stepRow.createCell(colIndex++).setCellValue(step.getInput());

                    // Extras
                    for (String colName : step.getExtras().keySet()) {
                        String value = step.getExtra(colName);
                        stepRow.createCell(colIndex++).setCellValue(value);
                    }

                    // Apply zebra striping
                    CellStyle stepStyle = createStepRowStyle(workbook, rowIndex.get() % 2 == 0);
                    for (int i = 0; i < colIndex; i++) {
                        stepRow.getCell(i).setCellStyle(stepStyle);
                    }
                }
            }

            // Recurse into child suites if any
            if (!scenarioItem.getChildren().isEmpty()) {
                saveScenariosRecursive(sheet, scenarioItem, rowIndex, workbook);
            }
        }
    }


    private void saveScenariosRecursive(Sheet sheet, TreeItem<TestNode> node, AtomicInteger rowIndex) {
        NodeType type = node.getValue().getType();

        if (type == NodeType.SUB_SUITE) {
            Row row = sheet.createRow(rowIndex.getAndIncrement());
            row.createCell(0).setCellValue("Sub-Suite: " + node.getValue().getName());
        }

        if (type == NodeType.TEST_SCENARIO) {
            TestScenario scenario = node.getValue().getScenarioRef();
            if (scenario != null) {
                // Scenario header row — use node name, not scenario model
                Row row = sheet.createRow(rowIndex.getAndIncrement());
                row.createCell(0).setCellValue("Scenario: " + node.getValue().getName());

                // Collect all extras across steps
                Set<String> allExtras = scenario.getSteps().stream()
                        .flatMap(s -> s.getExtras().keySet().stream())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                // Header row
                Row header = sheet.createRow(rowIndex.getAndIncrement());
                int colIndex = 0;
                header.createCell(colIndex++).setCellValue("Item");
                header.createCell(colIndex++).setCellValue("Object");
                header.createCell(colIndex++).setCellValue("Action");
                header.createCell(colIndex++).setCellValue("Description");
                header.createCell(colIndex++).setCellValue("Input");
                for (String colName : allExtras) {
                    header.createCell(colIndex++).setCellValue(colName);
                }

                // Step rows
                for (TestStep step : scenario.getSteps()) {
                    Row stepRow = sheet.createRow(rowIndex.getAndIncrement());
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
        commitActiveEdits();

        // ✅ Wrap filtered steps into an ObservableList
        ObservableList<TestStep> checkedSteps = FXCollections.observableArrayList(
                tableView.getItems().stream()
                        .filter(TestStep::isSelected)
                        .toList()
        );

        if (!checkedSteps.isEmpty()) {
            applyGlobalArgsToSteps(checkedSteps); // 🔥 propagate global args
            updateExecutionPreview("▶ Running " + checkedSteps.size() + " selected steps...");
            for (TestStep step : checkedSteps) {
                runSingleStep(step);
            }
            return;
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

        // ✅ Wrap single step into an ObservableList
        applyGlobalArgsToSteps(FXCollections.observableArrayList(selectedStep));
        runSingleStep(selectedStep);
    }



    private void applyGlobalArgsToSteps(List<TestStep> steps) {
        for (TestStep step : steps) {
            // Get the expected args for this action
            List<String> args = argsByAction.getOrDefault(step.getAction(), List.of());

            // Start fresh extras map
            Map<String, StringProperty> newExtras = new LinkedHashMap<>();

            for (String arg : args) {
                StringProperty value;

                // Priority 1: keep existing step value if present
                if (step.getExtras() != null && step.getExtras().containsKey(arg)) {
                    value = new SimpleStringProperty(step.getExtras().get(arg).get());
                }
                // Priority 2: apply global arg if available
                else if (globalArgs.containsKey(arg)) {
                    value = new SimpleStringProperty(globalArgs.get(arg).get());
                }
                // Priority 3: default empty
                else {
                    value = new SimpleStringProperty("");
                }

                newExtras.put(arg, value);
            }

            step.setExtras(newExtras);
            step.setMaxArgs(args.size());
        }

        log.info("Applied global args to " + steps.size() + " steps: ");
    }



    private void runSingleStep(TestStep step) {
        if (step == null) return;
        step.setNew(false);

        // ✅ Apply global args to all steps before execution
        applyGlobalArgsToSteps(tableView.getItems());

        // ✅ Pre-execution extras dump
        String extrasDump = step.getExtras().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().get())
                .collect(Collectors.joining(", "));
        log.info(() -> "Running step " + step.getId() + " (" + step.getAction() + ") "
                + "with extras: [" + extrasDump + "]");

        String[] inputs = getInputsForAction(step.getAction());
        updateExecutionPreview("Annotation input names: " + Arrays.toString(inputs));
        updateExecutionPreview("Extras map: " + step.getExtras());

        Object resultObj = TestExecutor.runTest(step);

        if (resultObj instanceof Boolean) {
            step.setStatus((Boolean) resultObj ? "PASS" : "FAIL");
        } else if (resultObj == null) {
            step.setStatus("SKIPPED");
        } else {
            step.setStatus("ERROR");
        }

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("result", resultObj);
        step.getExtras().forEach((k, v) -> {
            if (v != null) {
                logData.put(k, v.get() == null ? "" : v.get());
            }
        });

        String message = "Ran step: " + step.getAction() +
                " on " + step.getObject() +
                " → Result: " + resultObj;
        updateExecutionPreview(message);

        logStepChange("Run", step, logData);
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
                String trimmed = newName.trim();
                TestNode testNode = node.getValue();
                String oldName = testNode.getName();

                // Update UI label
                testNode.setName(trimmed);

                // Sync underlying model
                if (testNode.getType() == NodeType.SUITE || testNode.getType() == NodeType.SUB_SUITE) {
                    if (testNode.getSuiteRef() != null) {
                        testNode.getSuiteRef().setName(trimmed);
                    }
                } else if (testNode.getType() == NodeType.TEST_SCENARIO) {
                    if (testNode.getScenarioRef() != null) {
                        testNode.getScenarioRef().setName(trimmed);
                    }
                    // ✅ No need to remap scenarioSteps/scenarioColumns — UUID key is stable
                }

                log.info(() -> "Renamed " + testNode.getType() + " " + oldName + " → " + trimmed);
                treeView.refresh();
                saveTree(treeView.getRoot());
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
            Map<String, StringProperty> extras = args.stream()
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


    private TreeItem<TestNode> buildSuiteNode(TestSuite suite, NodeType type) {
        String suiteName = (suite.getName() != null && !suite.getName().isBlank())
                ? suite.getName()
                : "Unnamed Suite";

        TestNode suiteNode = new TestNode(suiteName, type);
        suiteNode.setSuiteRef(suite);
        TreeItem<TestNode> suiteItem = new TreeItem<>(suiteNode);
        suiteItem.setExpanded(true);

        // ✅ Add one icon for Suite/SubSuite/TestSuite
        if (type == NodeType.SUITE || type == NodeType.SUB_SUITE) {
            FontIcon icon = new FontIcon(MaterialDesign.MDI_EMOTICON); // smiley icon
            icon.setIconSize(16);
            suiteItem.setGraphic(icon);
        }

        // Add scenarios
        for (TestScenario scenario : suite.getScenarios()) {
            String scenarioName = (scenario.getName() != null && !scenario.getName().isBlank())
                    ? scenario.getName()
                    : "Unnamed Scenario";

            TestNode scenarioNode = new TestNode(scenarioName, NodeType.TEST_SCENARIO);
            scenarioNode.setScenarioRef(scenario);
            TreeItem<TestNode> scenarioItem = new TreeItem<>(
                    scenarioNode,
                    makeIcon("/icons/Scenario.png", 16, 16)
            );
            scenarioItem.setExpanded(true);

            // 🔥 No toggle logic here anymore
            suiteItem.getChildren().add(scenarioItem);
        }

        // Recursive sub-suites
        for (TestSuite subSuite : suite.getSubSuites()) {
            suiteItem.getChildren().add(buildSuiteNode(subSuite, NodeType.SUB_SUITE));
        }

        return suiteItem;
    }


    private TestSuite cloneSuite(TestSuite original) {
        // keep the same name, no " Copy"
        TestSuite copy = new TestSuite(UUID.randomUUID().toString(), original.getName());

        for (TestScenario scenario : original.getScenarios()) {
            copy.addScenario(cloneScenario(scenario));
        }
        for (TestSuite subSuite : original.getSubSuites()) {
            copy.addSubSuite(cloneSuite(subSuite));
        }

        Map<String, String> originalVars = contextVariables.getOrDefault(original.getName(), Map.of());
        contextVariables.put(copy.getName(), new HashMap<>(originalVars));

        log.info(() -> "Cloned suite " + original.getName() + " → " + copy.getName());
        return copy;
    }


    private void handleCopy(TreeItem<TestNode> selected, ClipboardContent clipboardContent) {
        NodeType type = selected.getValue().getType();
        String message = null;

        if (type == NodeType.SUITE) {
            TestSuite suite = selected.getValue().getSuiteRef();
            clipboardContent.putString("SUITE:" + suite.getId());
            message = "Suite " + suite.getName() + " copied";

        } else if (type == NodeType.SUB_SUITE) {
            TestSuite subSuite = selected.getValue().getSuiteRef();
            clipboardContent.putString("SUBSUITE:" + subSuite.getId());
            message = "SubSuite " + subSuite.getName() + " copied";

        } else if (type == NodeType.TEST_SCENARIO) {
            TestScenario scenario = selected.getValue().getScenarioRef();
            clipboardContent.putString("SCENARIO:" + scenario.getId());
            message = "TestScenario " + scenario.getName() + " copied";

        } else if (type == NodeType.TEST_STEP) {
            TestStep step = selected.getValue().getStepRef();
            if (step != null) {
                clipboardContent.putString("STEP:" + step.getId());

                String displayName;
                if (step.getDescription() != null && !step.getDescription().isBlank()) {
                    displayName = step.getDescription();
                } else if (step.getAction() != null && !step.getAction().isBlank()) {
                    displayName = step.getAction() + " (" + step.getObject() + ")";
                } else {
                    displayName = "Step";
                }
                message = "TestStep " + displayName + " copied";
            }
        }

        if (message != null) {
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            log.info(message);
        }
    }


    private TestScenario cloneScenario(TestScenario original) {
        // keep the same name, no " Copy"
        TestScenario copy = new TestScenario(UUID.randomUUID().toString(), original.getName());

        // use the TestStep copy constructor
        for (TestStep step : original.getSteps()) {
            copy.addStep(new TestStep(step));
        }

        log.info(() -> "Cloned scenario " + original.getName() + " → " + copy.getName() +
                " with " + copy.getSteps().size() + " steps");
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
                makeIcon("/icons/Scenario.png", 16, 16)
        );
        scenarioItem.setExpanded(true);

        // 🔥 No toggle logic here anymore
        return scenarioItem;
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

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Paste");
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
        if (root == null) return;

        TextInputDialog dialog = new TextInputDialog("Suite");
        dialog.setTitle("New Suite");
        dialog.setHeaderText("Create a new Test Suite");
        dialog.setContentText("Enter Suite name:");

        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                TestSuite suiteModel = new TestSuite(trimmed);

                TestNode node = new TestNode(trimmed, NodeType.SUITE);
                node.setSuiteRef(suiteModel);

                TreeItem<TestNode> suiteItem = new TreeItem<>(node);
                suiteItem.setExpanded(true);

                root.getChildren().add(suiteItem);
                treeView.getSelectionModel().select(suiteItem);

                saveTree(treeView.getRoot()); // 🔥 persist immediately
                updateExecutionPreview("Created new suite: " + suiteModel.getName() +
                        " (id=" + suiteModel.getId() + ")");
            } else {
                showError("Suite name cannot be empty.");
            }
        });
    }


    @FXML
    private void handleNewSubSuite(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected != null &&
                (selected.getValue().getType() == NodeType.SUITE ||
                        selected.getValue().getType() == NodeType.SUB_SUITE)) {

            TextInputDialog dialog = new TextInputDialog("Sub-Suite");
            dialog.setTitle("New Sub-Suite");
            dialog.setHeaderText("Create a new Sub-Suite");
            dialog.setContentText("Enter Sub-Suite name:");

            dialog.showAndWait().ifPresent(name -> {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    TestSuite subSuiteModel = new TestSuite(trimmed);

                    TestNode node = new TestNode(trimmed, NodeType.SUB_SUITE);
                    node.setSuiteRef(subSuiteModel);

                    TreeItem<TestNode> subSuiteItem = new TreeItem<>(node);
                    subSuiteItem.setExpanded(true);

                    selected.getChildren().add(subSuiteItem);
                    treeView.getSelectionModel().select(subSuiteItem);

                    saveTree(treeView.getRoot()); // 🔥 persist immediately
                    updateExecutionPreview("Created new sub-suite: " + subSuiteModel.getName() +
                            " (id=" + subSuiteModel.getId() + ")");
                } else {
                    showError("Sub-Suite name cannot be empty.");
                }
            });

        } else {
            showError("Sub-Suite can only be added inside a Suite or Sub-Suite.");
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
                    TestScenario scenarioModel = new TestScenario(trimmed, argsByObject, actionsByObject);

                    TestNode node = new TestNode(trimmed, NodeType.TEST_SCENARIO);
                    node.setScenarioRef(scenarioModel);

                    TreeItem<TestNode> scenarioItem = new TreeItem<>(node);
                    scenarioItem.setExpanded(true);

                    selected.getChildren().add(scenarioItem);
                    treeView.getSelectionModel().select(scenarioItem);

                    // Safe guard: only populate extras if steps exist
                    if (!scenarioModel.getSteps().isEmpty()) {
                        scenarioSteps.put(scenarioModel.getId(),
                                FXCollections.observableArrayList(scenarioModel.getSteps().stream()
                                        .map(TestStep::new)
                                        .toList()));
                        scenarioColumns.put(scenarioModel.getId(),
                                new ArrayList<>(scenarioModel.getSteps().get(0).getExtras().keySet()));

                        if (treeView.getSelectionModel().getSelectedItem() == scenarioItem) {
                            tableView.setItems(scenarioSteps.get(scenarioModel.getId()));
                        }
                    }

                    saveTree(treeView.getRoot()); // 🔥 persist immediately
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
    private void handleSave(ActionEvent event) {
        TreeItem<TestNode> root = treeView.getRoot();
        if (root == null) {
            showError("No project tree to save.");
            return;
        }

        // 🔥 Prompt user for save choice
        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Save Project");
        choice.setHeaderText("Choose how to save the project");
        choice.setContentText("Do you want to overwrite the current file or save as a new one?");

        ButtonType saveAsIs = new ButtonType("Save As Is");
        ButtonType saveAsNew = new ButtonType("Save As New File");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        choice.getButtonTypes().setAll(saveAsIs, saveAsNew, cancel);

        Optional<ButtonType> result = choice.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveAsIs) {
                saveTree(root); // overwrites currentProjectFile
                updateExecutionPreview("Project saved successfully to " + currentProjectFile.getName());
            } else if (result.get() == saveAsNew) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Save Project As");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                File newFile = chooser.showSaveDialog(treeView.getScene().getWindow());
                if (newFile != null) {
                    currentProjectFile = newFile; // update active file
                    saveTree(root);               // save to new file
                    updateExecutionPreview("Project saved successfully as " + newFile.getName());
                }
            }
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

        TestStep newStep;
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            // Copy selected step’s metadata, blank extras
            TestStep currentStep = tableView.getItems().get(selectedIndex);
            newStep = TestStep.copyTemplate(currentStep);
        } else {
            TestScenario scenario = selected.getValue().getScenarioRef();
            if (scenario != null && !scenario.getSteps().isEmpty()) {
                TestStep lastStep = scenario.getSteps().get(scenario.getSteps().size() - 1);
                newStep = TestStep.copyTemplate(lastStep);
            } else {
                // Fall back to global defaults
                newStep = new TestStep();
                String defaultObject = actionsByObject.keySet().stream().findFirst().orElse("");
                newStep.setObject(defaultObject);

                String defaultAction = actionsByObject.getOrDefault(defaultObject, List.of())
                        .stream().findFirst().orElse("");
                newStep.setAction(defaultAction);

                List<String> defaultArgs = argsByObject.getOrDefault(defaultObject, List.of());
                Map<String, StringProperty> extras = defaultArgs.stream()
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

        newStep.setNew(true);

        TestScenario scenario = selected.getValue().getScenarioRef();
        if (scenario != null) {
            TestStep persistedStep = TestStep.deepCopy(newStep);
            scenario.getSteps().add(persistedStep);

            ObservableList<TestStep> boundList = scenarioSteps.get(scenario.getId());
            if (boundList != null) {
                boundList.add(persistedStep);
                tableView.getSelectionModel().select(persistedStep);
            } else {
                ObservableList<TestStep> stepsCopy = FXCollections.observableArrayList(
                        scenario.getSteps().stream().map(TestStep::deepCopy).toList()
                );
                scenarioSteps.put(scenario.getId(), stepsCopy);
                tableView.setItems(stepsCopy);
                tableView.getSelectionModel().select(persistedStep);
            }

            refreshScenarioUI(scenario);
            saveTree(treeView.getRoot());
        }

        log.info(() -> "Added new step to scenario " + scenario.getId() + ": " + newStep);
    }



    // Helper to copy extras
    // Helper to copy extras safely
    private static Map<String, SimpleStringProperty> copyExtras(Map<String, SimpleStringProperty> original) {
        return original.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new SimpleStringProperty(e.getValue().get()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }


    public static TestStep deepCopy(TestStep original) {
        if (original == null) return null;

        // Use the explicit constructor with ID to preserve the original ID if needed
        return new TestStep(
                original.getId(),                  // preserve ID
                original.getItem(),
                original.getAction(),
                original.getObject(),
                original.getInput(),
                original.getDescription(),
                original.getType(),
                original.getStatus(),
                toStringMap(original.getExtras()), // convert extras to plain map
                original.getMaxArgs(),
                original.isNew()
        );
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

        for (TestStep step : tableView.getItems()) {
            String action = step.getAction();
            if (action == null || action.isBlank()) {
                rowIndex++;
                continue;
            }

            groupedItems.add("Row " + rowIndex + ": " + action);

            List<String> args = argsByAction.getOrDefault(action, List.of());
            Map<String, StringProperty> existingExtras = step.getExtras() != null ? step.getExtras() : Map.of();
            Map<String, StringProperty> extras = args.stream()
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

            // ✅ Only once
            extras.forEach((key, prop) -> groupedItems.add("--" + key + "=" + prop.get()));

            rowIndex++;
        }

        Map<String, String> vars = tableView.getItems().stream()
                .flatMap(s -> s.getExtras().entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
        contextVariables.put(contextKey, vars);

        resolvedVariableList.setItems(FXCollections.observableArrayList(groupedItems));
        resolvedVariableList.setPlaceholder(new Label("No arguments discovered"));

        vars.forEach((name, value) -> log.info(() -> String.format(
                "[SYNC LOAD] scenario=%s, var=%s, value=%s",
                scenario.getId(), name, value
        )));

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
                    tf.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            commitEdit(tf.getText());
                        }
                    });
                }
            }
        });

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

                logExecutionEvent("ARG", "step=" +
                        (selectedStep != null ? selectedStep.getId() : "unknown") +
                        ", var=" + varName + ", value=" + varValue);

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
        for (TestStep step : tableView.getItems()) {
            List<String> args = argsByAction.getOrDefault(step.getAction(), List.of());
            Map<String, StringProperty> existingExtras = step.getExtras() != null ? step.getExtras() : Map.of();
            Map<String, StringProperty> extras = args.stream()
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
            step.setNew(false);
        }

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

    private void commitActiveEdits() {
        // End any active TableView edit
        if (tableView.getEditingCell() != null) {
            tableView.edit(-1, null);
        }

        // End any active ListView edit (optional safeguard)
        if (resolvedVariableList.getEditingIndex() >= 0) {
            resolvedVariableList.edit(-1);
        }
    }




    // --- Suite lookup by ID ---
    private TestSuite findSuiteById(String id) {
        return findSuiteRecursive(treeView.getRoot(), id);
    }

    private TestSuite findSuiteRecursive(TreeItem<TestNode> node, String id) {
        if (node.getValue().getSuiteRef() != null &&
                node.getValue().getSuiteRef().getId().equals(id)) {
            return node.getValue().getSuiteRef();
        }
        for (TreeItem<TestNode> child : node.getChildren()) {
            TestSuite result = findSuiteRecursive(child, id);
            if (result != null) return result;
        }
        return null;
    }

    // --- Scenario lookup by ID ---
    private TestScenario findScenarioById(String id) {
        return findScenarioRecursive(treeView.getRoot(), id);
    }

    private TestScenario findScenarioRecursive(TreeItem<TestNode> node, String id) {
        if (node.getValue().getScenarioRef() != null &&
                node.getValue().getScenarioRef().getId().equals(id)) {
            return node.getValue().getScenarioRef();
        }
        for (TreeItem<TestNode> child : node.getChildren()) {
            TestScenario result = findScenarioRecursive(child, id);
            if (result != null) return result;
        }
        return null;
    }

    // --- Paste helper ---
    private void handlePaste(TreeItem<TestNode> selected, String data) {
        NodeType parentType = selected.getValue().getType();

        // --- Paste Suite ---
        if (data.startsWith("SUITE:")) {
            if (parentType != NodeType.ROOT) {
                showWarning("Suites can only be pasted under Root.");
                return;
            }
            String suiteId = data.substring("SUITE:".length());
            TestSuite original = findSuiteById(suiteId);
            if (original == null) return;

            TestSuite copy = cloneSuite(original);
            TreeItem<TestNode> copiedNode = buildSuiteNode(copy, NodeType.SUITE);
            selected.getChildren().add(copiedNode);
            treeView.getSelectionModel().select(copiedNode);
        }

        // --- Paste SubSuite ---
        if (data.startsWith("SUBSUITE:")) {
            if (parentType != NodeType.SUITE) {
                showWarning("SubSuites can only be pasted under a Suite.");
                return;
            }
            String suiteId = data.substring("SUBSUITE:".length());
            TestSuite original = findSuiteById(suiteId);
            if (original == null) return;

            TestSuite copy = cloneSuite(original);
            TreeItem<TestNode> copiedNode = buildSuiteNode(copy, NodeType.SUB_SUITE);
            selected.getChildren().add(copiedNode);
            treeView.getSelectionModel().select(copiedNode);
        }

        // --- Paste Scenario ---
        if (data.startsWith("SCENARIO:")) {
            if (parentType != NodeType.SUITE && parentType != NodeType.SUB_SUITE) {
                showWarning("Scenarios can only be pasted under a Suite or SubSuite.");
                return;
            }
            String scenarioId = data.substring("SCENARIO:".length());
            TestScenario original = findScenarioById(scenarioId);
            if (original == null) return;

            TestScenario copy = cloneScenario(original);
            TreeItem<TestNode> copiedNode = buildScenarioNode(copy);
            selected.getChildren().add(copiedNode);
            treeView.getSelectionModel().select(copiedNode);
        }

        // --- Paste Step ---
        if (data.startsWith("STEP:")) {
            if (parentType != NodeType.TEST_SCENARIO) {
                showWarning("Steps can only be pasted under a Scenario.");
                return;
            }
            String stepId = data.substring("STEP:".length());
            TestStep original = findStepById(stepId);
            if (original == null) return;

            // ✅ Use copy constructor (deep copy with new UUID)
            TestStep copy = new TestStep(original);

            // Build node for the copied step
            TestNode stepNode = new TestNode(copy.getId(), copy.getDescription(), NodeType.TEST_STEP);
            stepNode.setStepRef(copy);

            TreeItem<TestNode> copiedNode = buildTreeItem(stepNode); // ensures name + icon
            selected.getChildren().add(copiedNode);
            treeView.getSelectionModel().select(copiedNode);
        }

    }


    // --- Rename helper ---
    private void handleRename(TreeItem<TestNode> selected) {
        NodeType type = selected.getValue().getType();

        if (type == NodeType.SUITE || type == NodeType.SUB_SUITE) {
            TestSuite suite = selected.getValue().getSuiteRef();
            if (suite != null) {
                String oldName = suite.getName();
                TextInputDialog dialog = new TextInputDialog(oldName);
                dialog.setTitle("Rename " + type);
                dialog.setHeaderText("Rename " + type);
                dialog.setContentText("New name:");
                dialog.showAndWait().ifPresent(newName -> {
                    suite.setName(newName);
                    Map<String, String> vars = contextVariables.remove(oldName);
                    if (vars != null) {
                        contextVariables.put(newName, vars);
                    }
                    log.info(() -> "Renamed " + type + " " + oldName + " → " + newName);
                    treeView.refresh();
                });
            }

        } else if (type == NodeType.TEST_SCENARIO) {
            TestScenario scenario = selected.getValue().getScenarioRef();
            if (scenario != null) {
                String oldName = scenario.getName();
                TextInputDialog dialog = new TextInputDialog(oldName);
                dialog.setTitle("Rename Scenario");
                dialog.setHeaderText("Rename Scenario");
                dialog.setContentText("New name:");
                dialog.showAndWait().ifPresent(newName -> {
                    scenario.setName(newName);
                    log.info(() -> "Renamed Scenario " + oldName + " → " + newName);
                    treeView.refresh();
                });
            }

        } else if (type == NodeType.TEST_STEP) {
            TestStep step = selected.getValue().getStepRef();
            if (step != null) {
                // Use description as the editable field
                String oldName = step.getDescription() != null ? step.getDescription() : "";
                TextInputDialog dialog = new TextInputDialog(oldName);
                dialog.setTitle("Rename Step");
                dialog.setHeaderText("Rename Step");
                dialog.setContentText("New description:");
                dialog.showAndWait().ifPresent(newName -> {
                    step.setDescription(newName);
                    selected.getValue().setName(newName); // update TestNode name
                    log.info(() -> "Renamed Step " + oldName + " → " + newName);
                    treeView.refresh();
                });
            }
        }
    }


    private void enableDragAndDrop(TreeView<TestNode> treeView) {
        treeView.setCellFactory(tv -> {
            TreeCell<TestNode> cell = new TreeCell<>() {
                @Override
                protected void updateItem(TestNode item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        textProperty().unbind();
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                        setContextMenu(null);
                    } else {
                        textProperty().unbind();
                        setStyle("");

                        switch (item.getType()) {
                            case ROOT -> {
                                setText(item.getName());
                                setGraphic(makeIcon("/icons/bank.png", 16, 16));
                            }
                            case SUITE -> {
                                TestSuite suite = item.getSuiteRef();
                                if (suite != null) textProperty().bind(suite.nameProperty());
                                setGraphic(makeIcon("/icons/MainSuite.png", 16, 16));
                            }
                            case SUB_SUITE -> {
                                TestSuite subSuite = item.getSuiteRef();
                                if (subSuite != null) textProperty().bind(subSuite.nameProperty());
                                setGraphic(makeIcon("/icons/SubSuite.png", 16, 16));
                            }
                            case TEST_SCENARIO -> {
                                TestScenario scenario = item.getScenarioRef();
                                if (scenario != null) textProperty().bind(scenario.nameProperty());
                                setGraphic(makeIcon("/icons/TestSuite.png", 16, 16));
                            }
                            case TEST_STEP -> {
                                TestStep step = item.getStepRef();
                                String stepName = resolveStepName(step, item); // ✅ helper for naming
                                String status = step != null ? step.getStatus() : "";

                                setGraphic(makeIcon("/icons/Step.png", 14, 14));

                                if (status == null || status.isBlank()) {
                                    setText(stepName);
                                    setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                                } else if ("PASS".equalsIgnoreCase(status)) {
                                    setText(stepName);
                                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                } else if ("FAIL".equalsIgnoreCase(status)) {
                                    setText(stepName);
                                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                } else {
                                    setText(stepName);
                                    setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                                }
                            }
                        }

                        // ✅ Attach context menu per row
                        setContextMenu(buildContextMenu(getTreeItem()));
                    }
                }
            };

            // --- Drag detected ---
            cell.setOnDragDetected(event -> {
                if (cell.getItem() == null) return;
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(cell.getItem().getId().toString());
                db.setContent(content);
                event.consume();
            });

            // --- Drag over ---
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    TreeItem<TestNode> draggedItem = findTreeItemById(treeView.getRoot(), event.getDragboard().getString());
                    if (draggedItem != null && isValidDrop(draggedItem, cell.getTreeItem())) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        cell.setStyle("-fx-background-color: lightblue;");
                    }
                }
                event.consume();
            });

            // --- Drag exit ---
            cell.setOnDragExited(event -> cell.setStyle(""));

            // --- Drag dropped ---
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasString()) {
                    TreeItem<TestNode> draggedItem = findTreeItemById(treeView.getRoot(), db.getString());
                    TreeItem<TestNode> dropTarget = cell.getTreeItem();

                    if (draggedItem != null && dropTarget != null && draggedItem != dropTarget) {
                        if (isValidDrop(draggedItem, dropTarget)) {
                            draggedItem.getParent().getChildren().remove(draggedItem);
                            dropTarget.getChildren().add(draggedItem);
                            success = true;

                            log.info("✔ Dropped " + draggedItem.getValue().getType() +
                                    " into " + dropTarget.getValue().getType());

                            saveTree(treeView.getRoot());
                            treeView.refresh(); // ✅ refresh after drop
                        } else {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Invalid Drop");
                            alert.setHeaderText("Drop not allowed");
                            alert.setContentText("❌ Invalid drop: " +
                                    draggedItem.getValue().getType() + " → " +
                                    dropTarget.getValue().getType());
                            alert.showAndWait();

                            log.info("❌ Invalid drop: " + draggedItem.getValue().getType() +
                                    " → " + dropTarget.getValue().getType());
                        }
                    }
                }

                event.setDropCompleted(success);
                cell.setStyle("");
                event.consume();
            });

            return cell;
        });
    }

    private String resolveStepName(TestStep step, TestNode node) {
        if (step != null) {
            if (step.getDescription() != null && !step.getDescription().isBlank()) {
                return step.getDescription();
            } else if (step.getAction() != null && !step.getAction().isBlank()) {
                return step.getAction() + " (" + step.getObject() + ")";
            }
        }
        return node.getName() != null ? node.getName() : "Step";
    }


    private TreeItem<TestNode> findTreeItemById(TreeItem<TestNode> root, String id) {
        if (root == null || root.getValue() == null) return null;

        // Compare UUIDs as strings
        if (root.getValue().getId().toString().equals(id)) {
            return root;
        }

        // Recursively search children
        for (TreeItem<TestNode> child : root.getChildren()) {
            TreeItem<TestNode> result = findTreeItemById(child, id);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean isValidDrop(TreeItem<TestNode> draggedItem, TreeItem<TestNode> dropTarget) {
        NodeType draggedType = draggedItem.getValue().getType();
        NodeType targetType = dropTarget.getValue().getType();

        return switch (draggedType) {
            case SUITE -> targetType == NodeType.ROOT; // Suites only under Root
            case SUB_SUITE -> targetType == NodeType.SUITE; // SubSuites only under Suites
            case TEST_SCENARIO ->
                    targetType == NodeType.SUITE || targetType == NodeType.SUB_SUITE; // Scenarios under Suite/SubSuite
            case TEST_STEP -> targetType == NodeType.TEST_SCENARIO; // Steps only under Scenarios
            default -> false;
        };
    }

    private NodeDTO toDTO(TreeItem<TestNode> item) {
        TestNode node = item.getValue();
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setType(node.getType());

        // 🔥 Always persist the UI-bound name
        dto.setName(node.getName());

        switch (node.getType()) {
            case ROOT, SUITE, SUB_SUITE -> {
                for (TreeItem<TestNode> child : item.getChildren()) {
                    dto.getChildren().add(toDTO(child));
                }
            }
            case TEST_SCENARIO -> {
                TestScenario scenario = node.getScenarioRef();
                if (scenario != null) {
                    // ✅ Keep dto.name from node.getName(), do not overwrite
                    dto.setScenarioStatus(scenario.getStatus());
                    dto.setScenarioExtras(
                            scenario.getExtras() != null ? TestStep.toStringMap(scenario.getExtras()) : null
                    );

                    ObservableList<TestStep> stepsToSave =
                            scenarioSteps.getOrDefault(scenario.getId(), scenario.getSteps());

                    for (TestStep step : stepsToSave) {
                        NodeDTO stepDTO = new NodeDTO();
                        stepDTO.setId(step.getId());
                        stepDTO.setType(NodeType.TEST_STEP);
                        stepDTO.setItem(step.getItem());
                        stepDTO.setAction(step.getAction());
                        stepDTO.setObject(step.getObject());
                        stepDTO.setInput(step.getInput());
                        stepDTO.setDescription(step.getDescription());
                        stepDTO.setStepType(step.getType());
                        stepDTO.setStepStatus(step.getStatus());
                        stepDTO.setStepExtras(
                                step.getExtras() != null ? TestStep.toStringMap(step.getExtras()) : null
                        );

                        dto.getChildren().add(stepDTO);
                    }
                }
            }
            case TEST_STEP -> {
                TestStep step = node.getStepRef();
                if (step != null) {
                    dto.setId(step.getId());
                    dto.setItem(step.getItem());
                    dto.setAction(step.getAction());
                    dto.setObject(step.getObject());
                    dto.setInput(step.getInput());
                    dto.setDescription(step.getDescription());
                    dto.setStepType(step.getType());
                    dto.setStepStatus(step.getStatus());
                    dto.setStepExtras(
                            step.getExtras() != null ? TestStep.toStringMap(step.getExtras()) : null
                    );
                }
            }
        }

        return dto;
    }



    private TreeItem<TestNode> fromDTO(NodeDTO dto) {
        // 🔥 Ensure TestNode is created with the persisted name
        TestNode node = new TestNode(dto.getId(), dto.getName(), dto.getType());
        node.setName(dto.getName()); // make sure bound property is updated

        if (dto.getType() == NodeType.SUITE || dto.getType() == NodeType.SUB_SUITE) {
            TestSuite suite = new TestSuite(dto.getId(), dto.getName());
            suite.setName(dto.getName());   // restore suite name
            node.setSuiteRef(suite);
        }

        if (dto.getType() == NodeType.TEST_SCENARIO) {
            TestScenario scenario = new TestScenario(dto.getId(), dto.getName());
            scenario.setName(dto.getName());   // restore scenario name
            scenario.setStatus(dto.getScenarioStatus());

            if (dto.getScenarioExtras() != null) {
                Map<String, SimpleStringProperty> extrasMap = new LinkedHashMap<>();
                dto.getScenarioExtras().forEach((k, v) -> extrasMap.put(k, new SimpleStringProperty(v)));
                scenario.setExtras(extrasMap);
            }

            ObservableList<TestStep> restoredSteps = FXCollections.observableArrayList();
            if (dto.getChildren() != null) {
                for (NodeDTO stepDTO : dto.getChildren()) {
                    TestStep step = new TestStep(
                            stepDTO.getId(),
                            stepDTO.getItem(),
                            stepDTO.getAction(),
                            stepDTO.getObject(),
                            stepDTO.getInput(),
                            stepDTO.getDescription(),
                            stepDTO.getStepType(),
                            stepDTO.getStepStatus(),
                            stepDTO.getStepExtras() != null ? stepDTO.getStepExtras() : new HashMap<>(),
                            stepDTO.getStepExtras() != null ? stepDTO.getStepExtras().size() : 0,
                            false
                    );
                    restoredSteps.add(step);
                }
            }

            scenario.getSteps().setAll(restoredSteps);
            scenarioSteps.put(scenario.getId(), restoredSteps);
            node.setScenarioRef(scenario);
        }

        if (dto.getType() == NodeType.TEST_STEP) {
            TestStep step = new TestStep(
                    dto.getId(),
                    dto.getItem(),
                    dto.getAction(),
                    dto.getObject(),
                    dto.getInput(),
                    dto.getDescription(),
                    dto.getStepType(),
                    dto.getStepStatus(),
                    dto.getStepExtras() != null ? dto.getStepExtras() : new HashMap<>(),
                    dto.getStepExtras() != null ? dto.getStepExtras().size() : 0,
                    false
            );
            node.setStepRef(step);
        }


        TreeItem<TestNode> item = buildTreeItem(node);

        if (dto.getChildren() != null &&
                (dto.getType() == NodeType.ROOT ||
                        dto.getType() == NodeType.SUITE ||
                        dto.getType() == NodeType.SUB_SUITE)) {
            for (NodeDTO child : dto.getChildren()) {
                item.getChildren().add(fromDTO(child));
            }
        }

        return item;
    }



    // --- Save helper ---

    private void saveTree(TreeItem<TestNode> root) {
        if (root == null) {
            log.warning("saveTree called with null root");
            return;
        }

        try {
            // 🔥 If no file has been chosen yet, default to tree.json in AutomoniusProject
            if (currentProjectFile == null) {
                File projectDir = new File(System.getProperty("user.home"), "AutomoniusProject");
                if (!projectDir.exists()) projectDir.mkdirs();
                currentProjectFile = new File(projectDir, "tree.json");
            }

            try (FileWriter writer = new FileWriter(currentProjectFile)) {
                NodeDTO dto = toDTO(root); // convert the full hierarchy
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(dto, writer);

                log.info("Tree persisted successfully to " + currentProjectFile.getAbsolutePath());
            }
        } catch (IOException e) {
            showError("Failed to save tree: " + e.getMessage());
            log.severe("Failed to save tree: " + e.getMessage());
        }
    }


    public void stop() {
        saveTree(treeView.getRoot());
    }


    private void loadTree(TreeView<TestNode> treeView) {
        File projectDir = new File(System.getProperty("user.home"), "AutomoniusProject");
        if (!projectDir.exists()) projectDir.mkdirs();

        File[] jsonFiles = projectDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            log.warning("No JSON project found, starting with empty tree");

            TreeItem<TestNode> root = treeView.getRoot();
            if (root != null && root.getChildren().isEmpty()) {
                TestSuite suiteModel = new TestSuite("Default Suite");
                TestNode node = new TestNode("Default Suite", NodeType.SUITE);
                node.setSuiteRef(suiteModel);

                TreeItem<TestNode> suiteItem = new TreeItem<>(node);
                suiteItem.setExpanded(true);
                root.getChildren().add(suiteItem);

                currentProjectFile = new File(projectDir, "tree.json");
                saveTree(treeView.getRoot());
                log.info("Initialized with Default Suite under Directory Structure");
            }
            return;
        }

        if (jsonFiles.length == 1) {
            currentProjectFile = jsonFiles[0];
            loadTreeFromFile(currentProjectFile);
            return;
        }

        // Multiple files → let user choose
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Project File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File selected = chooser.showOpenDialog(treeView.getScene().getWindow());
        if (selected != null) {
            currentProjectFile = selected;
            loadTreeFromFile(selected);
        }
    }

    private void loadTreeFromFile(File file) {
        if (file == null || !file.exists()) {
            showError("Project file not found: " + (file != null ? file.getAbsolutePath() : "null"));
            log.warning("No project file provided or file does not exist.");
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            NodeDTO dto = gson.fromJson(reader, NodeDTO.class);

            // 🔥 Restore hierarchy from DTO
            TreeItem<TestNode> restoredRoot = fromDTO(dto);
            restoredRoot.setExpanded(true);
            treeView.setRoot(restoredRoot);

            // Track which file is active for saving later
            currentProjectFile = file;

            log.info("Tree restored successfully from " + file.getAbsolutePath() +
                    ", children=" + restoredRoot.getChildren().size());
        } catch (IOException e) {
            showError("Failed to load project: " + e.getMessage());
            log.severe("Failed to load project: " + e.getMessage());
        }
    }

    @FXML
    private void handleLoadProject(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Project File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showOpenDialog(treeView.getScene().getWindow());

        if (file != null) {
            treeView.setRoot(null); // clear old tree
            loadTreeFromFile(file); // 🔥 call your helper
            Preferences.userNodeForPackage(MainController.class)
                    .put("lastProjectPath", file.getAbsolutePath());
            updateExecutionPreview("Loaded project: " + file.getName());
        } else {
            updateExecutionPreview("Project load cancelled.");
        }
    }


    private TestStep findStepById(String id) {
        return findStepRecursive(treeView.getRoot(), id);
    }

    private TestStep findStepRecursive(TreeItem<TestNode> item, String id) {
        if (item == null) return null;

        TestNode node = item.getValue();
        if (node.getType() == NodeType.TEST_STEP) {
            TestStep step = node.getStepRef();
            if (step != null && step.getId().equals(id)) {
                return step;
            }
        }

        for (TreeItem<TestNode> child : item.getChildren()) {
            TestStep found = findStepRecursive(child, id);
            if (found != null) return found;
        }
        return null;
    }


    public static TestStep cloneStep(TestStep original) {
        if (original == null) return null;
        return new TestStep(original); // deep copy with new UUID
    }


    private ContextMenu buildContextMenu(TreeItem<TestNode> item) {
        ContextMenu menu = new ContextMenu();
        NodeType type = item.getValue().getType();

        // --- Copy ---
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            handleCopy(item, content);
        });
        // Disable copy on Root
        copyItem.setDisable(type == NodeType.ROOT);
        menu.getItems().add(copyItem);

        // --- Paste ---
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                handlePaste(item, clipboard.getString());
            }
        });
        // Disable paste where invalid
        if (type == NodeType.TEST_STEP) {
            pasteItem.setDisable(true); // steps cannot have children
        }
        menu.getItems().add(pasteItem);

        // --- Rename ---
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> handleRenameNode(item)); // ✅ use the unified handler
        renameItem.setDisable(type == NodeType.ROOT);
        menu.getItems().add(renameItem);


        // --- Run Step (only for steps) ---
        if (type == NodeType.TEST_STEP) {
            MenuItem runItem = new MenuItem("Run Step");
            runItem.setOnAction(e -> {
                treeView.getSelectionModel().select(item);
                handleRun(new ActionEvent());
            });
            menu.getItems().add(runItem);
        }

        return menu;
    }

    private TreeItem<TestNode> buildTreeItem(TestNode node) {
        // --- Fix naming for steps ---
        if (node.getType() == NodeType.TEST_STEP) {
            TestStep step = node.getStepRef();
            String stepName;
            if (step != null) {
                if (step.getDescription() != null && !step.getDescription().isBlank()) {
                    stepName = step.getDescription();
                } else if (step.getAction() != null && !step.getAction().isBlank()) {
                    stepName = step.getAction() + " (" + step.getObject() + ")";
                } else {
                    stepName = "Step";
                }
            } else {
                stepName = "Step";
            }
            node.setName(stepName);
        }

        TreeItem<TestNode> item = new TreeItem<>(node);

        // --- Add icon only for steps ---
        if (node.getType() == NodeType.TEST_STEP) {
            item.setGraphic(new FontIcon("mdi-play-circle"));
        }

        return item;
    }

    private void applyStepToggle(TreeItem<TestNode> scenarioItem, TestScenario scenario) {
        scenarioItem.getChildren().clear();

        if (showStepsToggle != null && showStepsToggle.isSelected()) {
            int rowIndex = 1;
            for (TestStep step : scenario.getSteps()) {
                String stepName = (step.getDescription() != null && !step.getDescription().isBlank())
                        ? step.getDescription()
                        : step.getAction();

                String displayName = "Row " + rowIndex + ": " + (stepName != null ? stepName : "Unnamed Step");

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
    }


    public static void setCopiedStep(TestStep step) {
        copiedStep = step == null ? null : new TestStep(step); // deep copy }

    }

    public static TestStep getCopiedStep() {
        return copiedStep == null ? null : new TestStep(copiedStep); // return deep copy }

    }

    @FXML
    private void handleCopyStep(ActionEvent event) {
        TestStep selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            setCopiedStep(selected);
            log.info(() -> "Copied step: " + selected);
        }
    }

    @FXML
    private void handlePasteStep(ActionEvent event) {
        TreeItem<TestNode> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue().getType() != NodeType.TEST_SCENARIO) return;

        TestScenario scenario = selected.getValue().getScenarioRef();
        if (scenario == null) return;

        TestStep copiedStep = MainController.getCopiedStep();
        if (copiedStep == null) return;

        // --- Add new step (deep copy for paste) ---
        TestStep newStep = TestStep.deepCopy(copiedStep);
        tableView.getItems().add(newStep);
        tableView.getSelectionModel().select(newStep);

        // --- Sync scenario + persist ---
        scenario.getSteps().add(TestStep.deepCopy(newStep)); // keep scenario list consistent
        persistScenarioChange(selected, scenario, "Paste");

        log.info(() -> "Pasted step into scenario " + scenario.getId() + ": " + newStep);
    }


    /**
     * Persist scenario changes after modifying steps (AddRow, Paste, Run, etc.)
     * Ensures model, cache, UI, and snapshot are all updated consistently.
     */
    private void persistScenarioChange(TreeItem<TestNode> scenarioItem,
                                       TestScenario scenario,
                                       String actionLabel) {
        if (scenarioItem == null || scenario == null) return;

        // 🔥 Update scenario model immediately
        scenario.getSteps().setAll(
                tableView.getItems().stream().map(TestStep::new).toList()
        );

        // 🔥 Update cached observable list
        scenarioSteps.put(scenario.getId(), tableView.getItems());

        // 🔥 Apply toggle so TreeView reflects changes
        applyStepToggle(scenarioItem, scenario);

        // Mark dirty so commit logic knows to save
        tableDirty = true;

        // 🔥 Write snapshot to file for persistence
        String timestamp = LocalDateTime.now().toString();
        logScenarioSnapshot(actionLabel, scenario, scenario.getSteps(), timestamp);
        writeScenarioSnapshotToFile(scenario, scenario.getSteps(), actionLabel, timestamp);

        log.info(() -> actionLabel + " scenario " + scenario.getId() +
                " with " + tableView.getItems().size() + " steps");
    }

    @FXML
    private void handleExportExcel(ActionEvent event) {
        saveProject(); // your existing Excel export method
        updateExecutionPreview("Excel report exported successfully.");
    }


    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Automonius");
        primaryStage.show();

        // 🔥 Defer until after stage is visible
        Platform.runLater(controller::setupInitialProject);
    }


    private void setupSelectColumn() {
        // Bind each row's checkbox to TestStep.selectedProperty
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // --- Add master checkbox in header ---
        CheckBox masterCheckBox = new CheckBox();
        masterCheckBox.setOnAction(e -> {
            boolean selected = masterCheckBox.isSelected();
            for (TestStep step : tableView.getItems()) {
                step.setSelected(selected);
            }
            tableView.refresh();
        });

        selectColumn.setGraphic(masterCheckBox); // place checkbox in header
        selectColumn.setPrefWidth(40);           // keep it compact
    }





}