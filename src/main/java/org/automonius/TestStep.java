package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import org.automonius.exec.TestCase;
import java.util.*;

/**
 * Represents a single test step row in the TableView.
 * - input: stores the primary variable name reference (first argument).
 * - extras: stores dynamic argument values (Arg1, Arg2, ...).
 * - maxArgs: tracks the maximum number of arguments defined for this step.
 */
public class TestStep {
    private final String id;  // unique identifier for this step

    private final SimpleStringProperty item;
    private final SimpleStringProperty action;
    private final SimpleStringProperty object;
    private final SimpleStringProperty input;
    private final SimpleStringProperty description;

    private final Map<String, SimpleStringProperty> extras = new HashMap<>();

    private int maxArgs = 0;

    // Default constructor
    public TestStep() {
        this("", "", "", "");
    }



    // Main constructor
    public TestStep(String item, String action, String object, String input) {
        this.id = UUID.randomUUID().toString();   // assign unique ID
        this.item = new SimpleStringProperty(item == null ? "" : item);
        this.action = new SimpleStringProperty(action == null ? "" : action);
        this.object = new SimpleStringProperty(object == null ? "" : object);
        this.input = new SimpleStringProperty(input == null ? "" : input);
        this.description = new SimpleStringProperty("");
    }

    // Convenience constructor for TestCase
    public TestStep(TestCase tc) {
        this(
                "",
                tc.getActionName(),
                tc.getObjectName(),
                tc.getInputs().isEmpty() ? "" : tc.getInputs().get(0) // first input only
        );
        this.setDescription(tc.getDescription());

        // Populate extras with all input names
        for (String arg : tc.getInputs()) {
            this.setExtra(arg, "");
        }

        this.maxArgs = tc.getInputs().size();
    }

    // Copy constructor with deep extras copy and preserved ID
    public TestStep(TestStep original) {
        this.id = original.id; // ✅ preserve ID for continuity
        this.item = new SimpleStringProperty(original.getItem());
        this.action = new SimpleStringProperty(original.getAction());
        this.object = new SimpleStringProperty(original.getObject());
        this.input = new SimpleStringProperty(original.getInput());
        this.description = new SimpleStringProperty(original.getDescription());
        this.maxArgs = original.getMaxArgs();

        if (original.getExtras() != null) {
            original.getExtras().forEach((k, v) -> {
                SimpleStringProperty prop = new SimpleStringProperty(v.get());
                attachDirtyListener(prop, k);
                this.extras.put(k, prop);
            });
        }
    }

    // --- New getter ---
    public String getId() {
        return id;
    }

    // --- Getters ---
    public String getItem() { return item.get(); }
    public String getAction() { return action.get(); }
    public String getObject() { return object.get(); }
    public String getInput() { return input.get(); }
    public String getDescription() { return description.get(); }

    // --- Setters ---
    public void setItem(String value) { this.item.set(value); }
    public void setAction(String value) { this.action.set(value); }
    public void setObject(String value) { this.object.set(value); }
    public void setInput(String value) { this.input.set(value); }
    public void setDescription(String value) { this.description.set(value); }

    // --- Property methods ---
    public SimpleStringProperty itemProperty() { return item; }
    public SimpleStringProperty actionProperty() { return action; }
    public SimpleStringProperty objectProperty() { return object; }
    public SimpleStringProperty inputProperty() { return input; }
    public SimpleStringProperty descriptionProperty() { return description; }

    // --- Dynamic extras (arguments and more) ---
    public SimpleStringProperty getExtraProperty(String columnName) {
        return extras.computeIfAbsent(columnName, k -> {
            SimpleStringProperty prop = new SimpleStringProperty("");
            attachDirtyListener(prop, k);
            return prop;
        });
    }

    public String getExtra(String columnName) {
        return getExtraProperty(columnName).get();
    }

    public void setExtra(String columnName, String value) {
        getExtraProperty(columnName).set(value);
    }

    public Map<String, SimpleStringProperty> getExtras() {
        return Collections.unmodifiableMap(extras); // ✅ protect against external mutation
    }

    public void setExtras(Map<String, String> newExtras) {
        extras.clear();
        if (newExtras != null) {
            newExtras.forEach((k, v) -> {
                SimpleStringProperty prop = new SimpleStringProperty(v);
                attachDirtyListener(prop, k);
                extras.put(k, prop);
            });
        }
    }

    // Spread a pipe-separated string into annotation-driven extras
    public void setJoinedArguments(String joined, String[] inputNames) {
        if (joined == null || joined.isEmpty() || inputNames == null) return;
        String[] parts = joined.split("\\|");
        for (int i = 0; i < inputNames.length && i < parts.length; i++) {
            setExtra(inputNames[i], parts[i].trim());
        }
    }

    // Collect annotation-driven extras back into a pipe-separated string
    public String getJoinedArguments(String[] inputNames) {
        if (inputNames == null) return "";
        List<String> values = new ArrayList<>();
        for (String name : inputNames) {
            values.add(getExtra(name));
        }
        return String.join("|", values);
    }

    // --- New methods for logging ---
    public List<String> getArgs() {
        return extras.values().stream()
                .map(SimpleStringProperty::get)
                .toList();
    }

    // --- MaxArgs accessors ---
    public int getMaxArgs() {
        return maxArgs;
    }

    public void setMaxArgs(int maxArgs) {
        this.maxArgs = maxArgs;
    }

    @Override
    public String toString() {
        return "TestStep{" +
                "item=" + getItem() +
                ", action=" + getAction() +
                ", object=" + getObject() +
                ", input=" + getInput() +
                ", description=" + getDescription() +
                ", maxArgs=" + maxArgs +
                ", extras=" + extras +
                '}';
    }

    // --- Helper for dirty tracking ---
    private void attachDirtyListener(SimpleStringProperty prop, String key) {
        prop.addListener((obs, oldVal, newVal) -> {
            MainController.markTableDirty(); // flip global dirty flag
            System.out.printf("Edited arg=%s, newValue=%s%n", key, newVal);
        });
    }
}
