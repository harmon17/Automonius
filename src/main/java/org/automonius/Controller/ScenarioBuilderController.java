package org.automonius.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import org.automonius.TestStep;
import org.automonius.exec.TestExecutor;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ScenarioBuilderController {

    @FXML private TableView<TestStep> scenarioTable;
    @FXML private TableColumn<TestStep, String> stepNameCol;
    @FXML private TableColumn<TestStep, String> extrasCol;
    @FXML private TextArea logArea;



    private static final Logger log = Logger.getLogger(ScenarioBuilderController.class.getName());

    @FXML
    private void initialize() {
        scenarioTable.setEditable(true);

        stepNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAction()));
        extrasCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getExtras() != null ? data.getValue().getExtras().toString() : ""
        ));

        stepNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        stepNameCol.setOnEditCommit(event -> event.getRowValue().setAction(event.getNewValue()));

        log.info("Scenario Builder window initialized");
    }

    @FXML
    private void handleRunScenarioBuilder(ActionEvent event) {
        if (scenarioTable.getItems().isEmpty()) {
            logArea.appendText("⚠ No steps defined.\n");
            return;
        }

        logArea.appendText("▶ Running Scenario Builder steps...\n");
        int rowIndex = 1;
        for (TestStep step : scenarioTable.getItems()) {
            try {
                Object resultObj = TestExecutor.runTest(step);
                logArea.appendText("Row " + rowIndex + ": " + step.getAction() +
                        " on " + step.getObject() +
                        " → Result: " + (resultObj != null ? resultObj.toString() : "<null>") + "\n");
            } catch (Exception ex) {
                logArea.appendText("Row " + rowIndex + ": " + step.getAction() +
                        " on " + step.getObject() +
                        " → ERROR: " + ex.getMessage() + "\n");
                log.log(Level.SEVERE, "Error running builder step " + rowIndex, ex);
            }
            rowIndex++;
        }
        logArea.appendText("✔ Finished running steps.\n");
    }

    @FXML
    private void handleAddScenarioStep(ActionEvent event) {
        TestStep newStep = new TestStep();
        newStep.setAction("defaultAction");
        newStep.setObject("defaultObject");
        scenarioTable.getItems().add(newStep);

        logArea.appendText("➕ Added new step.\n");
    }

    @FXML
    private void handleRemoveScenarioStep(ActionEvent event) {
        TestStep selected = scenarioTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            scenarioTable.getItems().remove(selected);
            logArea.appendText("➖ Removed step.\n");
        } else {
            logArea.appendText("⚠ No step selected.\n");
        }
    }
}
