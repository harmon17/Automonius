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

    // Each scenario owns its own ObservableList of steps
    private final ObservableList<TestStep> steps = FXCollections.observableArrayList();

    // Extras remain a simple List
    private final List<String> extras = new ArrayList<>();

    // Public constructor: always generates a unique UUID
    // Public constructor: always generates a unique UUID
    public TestScenario(String name,
                        Map<String, List<String>> argsByObject,
                        Map<String, List<String>> actionsByObject) {
        this(UUID.randomUUID().toString(), name);

        TestStep blank = new TestStep();

        // Pick default object with most args
        String defaultObject = argsByObject.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (defaultObject != null) {
            blank.setObject(defaultObject);

            // First action for that object
            String defaultAction = actionsByObject.getOrDefault(defaultObject, List.of())
                    .stream().findFirst().orElse(null);
            blank.setAction(defaultAction);

            // Seed extras
            List<String> defaultArgs = argsByObject.getOrDefault(defaultObject, List.of());

            blank.setExtras(defaultArgs.stream()
                    .collect(Collectors.toMap(arg -> arg, arg -> "", (a,b) -> a, LinkedHashMap::new)));

            blank.setMaxArgs(defaultArgs.size());
        }

        this.steps.add(blank);

        log.info(() -> "Seeded new scenario " + id +
                " with initial blank step (maxArgs=" + blank.getMaxArgs() + ").");
    }



    // Package-private constructor: for deserialization only
    TestScenario(String id, String name) {
        this.id = id;
        this.name.set(name);
        log.info(() -> "Created TestScenario: id=" + id + ", name=" + name);
    }

    // Copy constructor: deep copy of steps and extras
    public TestScenario(TestScenario original) {
        this(UUID.randomUUID().toString(), original.getName() + " Copy");

        // Deep copy steps (requires TestStep copy constructor)
        for (TestStep step : original.getSteps()) {
            this.steps.add(new TestStep(step));
        }

        // Copy extras
        this.extras.addAll(original.getExtras());

        log.info(() -> "Copied TestScenario from " + original.getId() + " to new id=" + this.id);
    }

    // --- ID ---
    public String getId() { return id; }

    // --- Name with reactive property ---
    public String getName() { return name.get(); }
    public void setName(String newName) {
        log.fine(() -> "Renaming scenario " + id + " from " + this.name.get() + " to " + newName);
        this.name.set(newName);
    }
    public StringProperty nameProperty() { return name; }

    // --- Steps ---
    public ObservableList<TestStep> getSteps() { return steps; }
    public void addStep(TestStep step) {
        steps.add(step);
        log.fine(() -> "Added step to scenario " + id + ": " + step);
    }

    // --- Extras ---
    public List<String> getExtras() { return extras; }
    public void setExtras(List<String> extras) {
        this.extras.clear();
        this.extras.addAll(extras);
        log.fine(() -> "Set extras for scenario " + id + ": " + extras);
    }
    public void addExtra(String extraName) {
        extras.add(extraName);
        log.fine(() -> "Added extra column to scenario " + id + ": " + extraName);
    }
}
