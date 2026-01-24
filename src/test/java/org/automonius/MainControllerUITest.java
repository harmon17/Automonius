package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MainControllerUITest {

    private MainController controller;

    @BeforeEach
    void setup() {
        controller = new MainController();
        controller.initialize(); // sets up tableView, resolvedVariableList, maps
    }

    @Test
    public void testObjectComboBoxSelectionUpdatesStep() {
        TestStep step = new TestStep();
        step.setObject("OldObject");
        step.setAction("OldAction");
        controller.tableView.setItems(FXCollections.observableArrayList(step));

        TableCell<TestStep, String> cell = controller.buildObjectComboCell();
        cell.updateItem("NewObject", false);

        ComboBox<String> combo = (ComboBox<String>) cell.getGraphic();
        combo.getSelectionModel().select("NewObject");

        assertEquals("NewObject", step.getObject());
        assertNotNull(step.getAction()); // auto‑assigned
    }

    @Test
    public void testActionComboBoxSelectionUpdatesStep() {
        TestStep step = new TestStep();
        step.setObject("SomeObject");
        step.setAction("OldAction");
        controller.tableView.setItems(FXCollections.observableArrayList(step));

        controller.actionsByObject.put("SomeObject", List.of("NewAction"));

        TableCell<TestStep, String> cell = controller.buildActionComboCell();
        cell.updateItem("NewAction", false);

        ComboBox<String> combo = (ComboBox<String>) cell.getGraphic();
        combo.getSelectionModel().select("NewAction");

        assertEquals("NewAction", step.getAction());
    }

    @Test
    public void testRefreshScenarioUIUpdatesListView() {
        TestScenario scenario = new TestScenario("id123", "TestScenario");
        TestStep step = new TestStep();
        step.setAction("Login");
        step.setExtras(Map.of("username", new SimpleStringProperty("admin")));
        scenario.getSteps().add(step);
        controller.tableView.setItems(FXCollections.observableArrayList(step));

        controller.argsByAction.put("Login", List.of("username", "password"));

        controller.refreshScenarioUI(scenario);

        ObservableList<String> items = controller.resolvedVariableList.getItems();
        assertTrue(items.contains("Row 1: Login"));
        assertTrue(items.stream().anyMatch(s -> s.startsWith("--username=")));
    }

    @Test
    public void testCommitTableEditsToScenario() {
        TestScenario scenario = new TestScenario("id456", "CommitScenario");
        TestStep step = new TestStep("1", "Login", "User", "input");
        step.setExtras(Map.of("username", new SimpleStringProperty("admin")));
        controller.tableView.setItems(FXCollections.observableArrayList(step));

        controller.argsByAction.put("Login", List.of("username"));

        controller.commitTableEditsToScenario(scenario);

        assertEquals(1, scenario.getSteps().size());
        assertEquals("admin", scenario.getSteps().get(0).getExtra("username"));
    }
}
