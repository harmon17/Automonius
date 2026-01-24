package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TestScenario {
    private static final Logger log = Logger.getLogger(TestScenario.class.getName());

    private final String id;
    private final StringProperty name = new SimpleStringProperty();

    // Scenario-level status (aggregated from steps)
    private final StringProperty status = new SimpleStringProperty("");

    // Each scenario owns its own ObservableList of steps
    private final ObservableList<TestStep> steps = FXCollections.observableArrayList();

    // Scenario-level extras act as headers (argument names + optional defaults)
    private final Map<String, SimpleStringProperty> extras = new LinkedHashMap<>();

    // --- Constructors ---
    public TestScenario(String name,
                        Map<String, List<String>> argsByAction,
                        Map<String, List<String>> actionsByObject) {
        this(UUID.randomUUID().toString(), name);

        TestStep blank = new TestStep();

        String defaultObject = null;
        String defaultAction = null;
        List<String> defaultArgs = List.of();

        if (!actionsByObject.isEmpty()) {
            defaultObject = actionsByObject.keySet().iterator().next();
            blank.setObject(defaultObject);

            List<String> availableActions = actionsByObject.getOrDefault(defaultObject, List.of());
            if (!availableActions.isEmpty()) {
                defaultAction = availableActions.get(0);
                blank.setAction(defaultAction);

                defaultArgs = argsByAction.getOrDefault(defaultAction, List.of());
            }
        }

        Map<String, SimpleStringProperty> seededExtras = defaultArgs.stream()
                .collect(Collectors.toMap(
                        arg -> arg,
                        arg -> new SimpleStringProperty(""),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        blank.setExtras(seededExtras);
        blank.setMaxArgs(seededExtras.size());

        this.extras.putAll(seededExtras);
        this.steps.add(blank);

        log.info(() -> "Seeded new scenario " + id +
                " with initial step (object=" + blank.getObject() +
                ", action=" + blank.getAction() +
                ", maxArgs=" + blank.getMaxArgs() + ").");
    }

    TestScenario(String id, String name) {
        this.id = id;
        this.name.set(name);
        log.info(() -> "Created TestScenario: id=" + id + ", name=" + name);
    }

    public TestScenario(TestScenario original) {
        this(UUID.randomUUID().toString(), original.getName() + " Copy");

        for (TestStep step : original.getSteps()) {
            this.steps.add(new TestStep(step));
        }

        original.getExtras().forEach((key, prop) -> {
            this.extras.put(key, new SimpleStringProperty(prop.get()));
        });

        this.status.set(original.getStatus());

        log.info(() -> "Copied TestScenario from " + original.getId() + " to new id=" + this.id);
    }

    // --- ID ---
    public String getId() { return id; }

    // --- Name ---
    public String getName() { return name.get(); }
    public void setName(String newName) {
        log.fine(() -> "Renaming scenario " + id + " from " + this.name.get() + " to " + newName);
        this.name.set(newName);
    }
    public StringProperty nameProperty() { return name; }

    // --- Status ---
    public String getStatus() { return status.get(); }
    public void setStatus(String newStatus) {
        log.fine(() -> "Scenario " + id + " status changed to " + newStatus);
        this.status.set(newStatus);
    }
    public StringProperty statusProperty() { return status; }

    // --- Steps ---
    public ObservableList<TestStep> getSteps() { return steps; }
    public void addStep(TestStep step) {
        steps.add(step);
        log.fine(() -> "Added step to scenario " + id + ": " + step);
    }

    // --- Extras ---
    public Map<String, SimpleStringProperty> getExtras() {
        return Collections.unmodifiableMap(extras);
    }

    public void setExtras(Map<String, SimpleStringProperty> newExtras) {
        extras.clear();
        if (newExtras != null) {
            newExtras.forEach((key, prop) -> extras.put(key, new SimpleStringProperty(prop.get())));
        }
        log.fine(() -> "Set extras for scenario " + id + ": " + extras);
    }

    public void addExtra(String extraName) {
        extras.put(extraName, new SimpleStringProperty(""));
        log.fine(() -> "Added extra column to scenario " + id + ": " + extraName);
    }

    // --- Aggregated status helper ---
    public void updateAggregatedStatus() {
        if (steps.isEmpty()) {
            setStatus("PENDING");
            return;
        }
        boolean anyFail = steps.stream().anyMatch(s -> "FAIL".equalsIgnoreCase(s.getStatus()));
        boolean allPass = steps.stream().allMatch(s -> "PASS".equalsIgnoreCase(s.getStatus()));

        if (anyFail) {
            setStatus("FAIL");
        } else if (allPass) {
            setStatus("PASS");
        } else {
            setStatus("PENDING");
        }
    }

    // --- Convenience helpers ---
    public List<String> getExtraNames() { return new ArrayList<>(extras.keySet()); }
    public List<String> getExtraValues() {
        return extras.values().stream().map(SimpleStringProperty::get).toList();
    }
}
