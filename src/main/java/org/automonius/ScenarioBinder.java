package org.automonius;

import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;

public class ScenarioBinder {
    private final TreeView<TestNode> treeView;
    private final TableView<TestStep> tableView;

    public ScenarioBinder(TreeView<TestNode> treeView, TableView<TestStep> tableView) {
        this.treeView = treeView;
        this.tableView = tableView;
        bind();
    }

    private void bind() {
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                TestNode node = newVal.getValue();

                switch (node.getType()) {
                    case SUITE -> loadSuite(node.getSuiteRef());
                    case TEST_SCENARIO -> loadScenario(node.getScenarioRef());
                    case TEST_STEP -> loadSingleStep(node.getStepRef());
                    default -> tableView.getItems().clear();
                }
            }
        });
    }

    // --- Load all scenarios in a suite ---
    private void loadSuite(TestSuite suite) {
        tableView.getItems().clear();
        if (suite != null) {
            suite.getScenarios().forEach(scenario -> tableView.getItems().addAll(scenario.getSteps()));
        }
    }

    // --- Load all steps in a scenario ---
    private void loadScenario(TestScenario scenario) {
        tableView.getItems().clear();
        if (scenario != null) {
            tableView.getItems().addAll(scenario.getSteps());
        }
    }

    // --- Load a single step ---
    private void loadSingleStep(TestStep step) {
        tableView.getItems().clear();
        if (step != null) {
            tableView.getItems().add(step);
        }
    }
}
