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
                if (node.getType() == NodeType.TEST_SCENARIO) {
                    loadTestScenario(node.getName());
                } else {
                    tableView.getItems().clear();
                }
            }
        });
    }

    private void loadTestScenario(String scenarioName) {
        tableView.getItems().clear();
        tableView.getItems().addAll(
                new TestStep("", "Navigate", "HomePage", ""),
                new TestStep("", "Click", "LoginButton", ""),
                new TestStep("", "Enter", "PasswordField", "secret")
        );
    }
}

