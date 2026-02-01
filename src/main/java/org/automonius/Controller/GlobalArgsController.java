package org.automonius.Controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.collections.FXCollections;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GlobalArgsController {

    private static final Logger log = Logger.getLogger(GlobalArgsController.class.getName());

    @FXML private TableView<GlobalArg> globalArgsTable;
    @FXML private TableColumn<GlobalArg, String> descriptionColumn;
    @FXML private TableColumn<GlobalArg, String> fieldNameColumn;
    @FXML private TableColumn<GlobalArg, String> valueColumn;
    @FXML private Label statusLabel;

    private final File persistenceFile = new File("globalArgs.json");

    @FXML
    public void initialize() {
        log.info("GlobalArgsController.initialize() called — setting up UI");

        setupTableColumns();
        globalArgsTable.setEditable(true);
        setupCommitHandlers();

        // Load persisted args
        try {
            List<GlobalArg> loaded = GlobalArgsManager.loadFromFile(persistenceFile);
            globalArgsTable.setItems(FXCollections.observableArrayList(loaded));
        } catch (IOException e) {
            log.warning("Failed to load global args: " + e.getMessage());
        }

        log.info("GlobalArgsController.initialize() finished");
    }

    private void setupTableColumns() {
        // ✅ Bind directly to StringProperty fields in GlobalArg
        descriptionColumn.setCellValueFactory(data -> data.getValue().descriptionProperty());
        fieldNameColumn.setCellValueFactory(data -> data.getValue().fieldNameProperty());
        valueColumn.setCellValueFactory(data -> data.getValue().valueProperty());

        descriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        fieldNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    private void setupCommitHandlers() {
        descriptionColumn.setOnEditCommit(event -> {
            GlobalArg arg = event.getRowValue();
            arg.setDescription(event.getNewValue());
            syncToManager();
        });
        fieldNameColumn.setOnEditCommit(event -> {
            GlobalArg arg = event.getRowValue();
            String normalized = event.getNewValue().trim().toLowerCase();
            arg.setFieldName(normalized);
            syncToManager();
        });
        valueColumn.setOnEditCommit(event -> {
            GlobalArg arg = event.getRowValue();
            arg.setValue(event.getNewValue());
            syncToManager();
        });
    }

    @FXML
    private void handleAddGlobalArg(ActionEvent event) {
        String uniqueName = "arg" + (globalArgsTable.getItems().size() + 1);
        GlobalArg newArg = new GlobalArg("New Arg", uniqueName, "value");
        globalArgsTable.getItems().add(newArg);
        syncToManager();
        showFloatingMessage("Added global arg: " + uniqueName);
    }

    @FXML
    private void handleRemoveGlobalArg(ActionEvent event) {
        GlobalArg selected = globalArgsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            globalArgsTable.getItems().remove(selected);
            syncToManager();
            showFloatingMessage("Removed global arg: " + selected.getFieldName());
        } else {
            showFloatingMessage("No arg selected to remove");
        }
    }

    private void syncToManager() {
        List<GlobalArg> allArgs = globalArgsTable.getItems();
        List<GlobalArg> validArgs = allArgs.stream().filter(GlobalArgsManager::isValid).collect(Collectors.toList());

        // ✅ Manager now reuses properties instead of recreating them
        GlobalArgsManager.updateFromList(validArgs);
        GlobalArgsManager.updateStepArgsFromList(validArgs);
        GlobalArgsManager.updatePayloadArgsFromList(validArgs);

        try {
            GlobalArgsManager.saveToFile(persistenceFile, validArgs);
        } catch (IOException e) {
            log.warning("Failed to save global args: " + e.getMessage());
        }

        statusLabel.setText(validArgs.size() + " valid");
    }

    private void showFloatingMessage(String message) {
        Label toastLabel = new Label(message);
        toastLabel.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 5px;");
        toastLabel.setOpacity(0);

        AnchorPane root = (AnchorPane) globalArgsTable.getScene().getRoot();
        root.getChildren().add(toastLabel);
        AnchorPane.setBottomAnchor(toastLabel, 50.0);
        AnchorPane.setLeftAnchor(toastLabel, 20.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toastLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> root.getChildren().remove(toastLabel));

        new SequentialTransition(fadeIn, stay, fadeOut).play();
    }

    @FXML
    private void handleLoadGlobalArgs(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Global Args JSON File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        File file = chooser.showOpenDialog(globalArgsTable.getScene().getWindow());
        if (file != null) {
            try {
                List<GlobalArg> loaded = GlobalArgsManager.loadFromFile(file);
                globalArgsTable.setItems(FXCollections.observableArrayList(loaded));
                statusLabel.setText("Reloaded " + loaded.size() + " args from " + file.getName());
                showFloatingMessage("Reloaded global args from " + file.getAbsolutePath());
            } catch (IOException e) {
                log.warning("Failed to reload global args: " + e.getMessage());
                showFloatingMessage("Failed to reload global args");
            }
        } else {
            showFloatingMessage("No file selected");
        }
    }
}
