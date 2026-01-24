package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import org.automonius.exec.TestCase;

import java.util.*;

/**
 * Represents a single test step row in the TableView.
 * - input: stores the primary variable name reference (first argument).
 * - extras: stores dynamic argument values (Arg1, Arg2, ...).
 * - maxArgs: tracks the maximum number of arguments defined for this step.
 * - type: classification of the step (read-only in TableView).
 * - status: execution result (PASS/FAIL/etc.).
 */
public class TestStep {
    private final String id;  // unique identifier for this step

    private final SimpleStringProperty item;
    private final SimpleStringProperty action;
    private final SimpleStringProperty object;
    private final SimpleStringProperty input;
    private final SimpleStringProperty description;

    private final SimpleStringProperty type;
    private final SimpleStringProperty status;

    private final Map<String, SimpleStringProperty> extras = new LinkedHashMap<>();
    private int maxArgs = 0;
    private boolean isNew = false;

    // --- Constructors ---

    public TestStep() {
        this("", "", "", "");
    }

    public TestStep(String item, String action, String object, String input) {
        this.id = UUID.randomUUID().toString();
        this.item = new SimpleStringProperty(item == null ? "" : item);
        this.action = new SimpleStringProperty(action == null ? "" : action);
        this.object = new SimpleStringProperty(object == null ? "" : object);
        this.input = new SimpleStringProperty(input == null ? "" : input);
        this.description = new SimpleStringProperty("");

        this.type = new SimpleStringProperty("Step");
        this.status = new SimpleStringProperty("");
    }

    public TestStep(TestCase tc) {
        this("", tc.getActionName(), tc.getObjectName(),
                tc.getInputs().isEmpty() ? "" : tc.getInputs().get(0));
        this.setDescription(tc.getDescription());

        for (String arg : tc.getInputs()) {
            this.setExtra(arg, "");
        }
        this.maxArgs = tc.getInputs().size();
    }


    // --- New flag accessors ---
    public boolean isNew() { return isNew; }
    public void setNew(boolean value) { this.isNew = value; }

    // Copy constructor: deep copy with values
    public TestStep(TestStep original) {
        this.id = UUID.randomUUID().toString();
        this.item = new SimpleStringProperty(original.getItem());
        this.action = new SimpleStringProperty(original.getAction());
        this.object = new SimpleStringProperty(original.getObject());
        this.input = new SimpleStringProperty(original.getInput());
        this.description = new SimpleStringProperty(original.getDescription());
        this.maxArgs = original.getMaxArgs();

        this.type = new SimpleStringProperty(original.getType());
        this.status = new SimpleStringProperty(original.getStatus());

        if (original.getExtras() != null) {
            original.getExtras().forEach((k, v) -> {
                SimpleStringProperty prop = new SimpleStringProperty(v.get()); // preserve value
                attachDirtyListener(prop, k);
                this.extras.put(k, prop);
            });
        }
    }

    // Explicit helper: copy with values (same as constructor)
    public static TestStep copyWithValues(TestStep original) {
        return new TestStep(original);
    }

    // Explicit helper: copy with blank extras (for AddRow)
    public static TestStep copyWithBlankExtras(TestStep template) {
        TestStep step = new TestStep();
        step.setObject(template.getObject());
        step.setAction(template.getAction());
        step.setDescription(template.getDescription());
        step.setMaxArgs(template.getMaxArgs());
        step.setType(template.getType());
        step.setStatus(template.getStatus());

        template.getExtras().forEach((k, v) -> {
            step.setExtra(k, ""); // always blank
        });
        return step;
    }

    // --- ID ---
    public String getId() { return id; }

    // --- Getters ---
    public String getItem() { return item.get(); }
    public String getAction() { return action.get(); }
    public String getObject() { return object.get(); }
    public String getInput() { return input.get(); }
    public String getDescription() { return description.get(); }
    public String getType() { return type.get(); }
    public String getStatus() { return status.get(); }

    // --- Setters ---
    public void setItem(String value) { this.item.set(value); }
    public void setAction(String value) { this.action.set(value); }
    public void setObject(String value) { this.object.set(value); }
    public void setInput(String value) { this.input.set(value); }
    public void setDescription(String value) { this.description.set(value); }
    public void setType(String value) { this.type.set(value); }
    public void setStatus(String value) { this.status.set(value); }

    // --- Property accessors ---
    public SimpleStringProperty itemProperty() { return item; }
    public SimpleStringProperty actionProperty() { return action; }
    public SimpleStringProperty objectProperty() { return object; }
    public SimpleStringProperty inputProperty() { return input; }
    public SimpleStringProperty descriptionProperty() { return description; }
    public SimpleStringProperty typeProperty() { return type; }
    public SimpleStringProperty statusProperty() { return status; }

    // --- Extras ---
    public SimpleStringProperty getExtraProperty(String columnName) {
        return extras.computeIfAbsent(columnName, k -> {
            SimpleStringProperty prop = new SimpleStringProperty("");
            attachDirtyListener(prop, k);
            return prop;
        });
    }

    public String getExtra(String columnName) { return getExtraProperty(columnName).get(); }
    public void setExtra(String columnName, String value) { getExtraProperty(columnName).set(value); }

    public Map<String, SimpleStringProperty> getExtras() {
        return Collections.unmodifiableMap(extras);
    }

    public void setExtras(Map<String, SimpleStringProperty> newExtras) {
        extras.clear();
        if (newExtras != null) {
            newExtras.forEach((key, prop) -> {
                SimpleStringProperty copy = new SimpleStringProperty(prop.get()); // preserve value
                attachDirtyListener(copy, key);
                extras.put(key, copy);
            });
        }
    }

    // --- MaxArgs ---
    public int getMaxArgs() { return maxArgs; }
    public void setMaxArgs(int maxArgs) { this.maxArgs = maxArgs; }

    @Override
    public String toString() {
        return "TestStep{" +
                "id=" + id +
                ", item=" + getItem() +
                ", action=" + getAction() +
                ", object=" + getObject() +
                ", input=" + getInput() +
                ", description=" + getDescription() +
                ", type=" + getType() +
                ", status=" + getStatus() +
                ", maxArgs=" + maxArgs +
                ", extras=" + extras.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().get())
                .toList() +
                '}';
    }

    // --- Dirty tracking ---
    private void attachDirtyListener(SimpleStringProperty prop, String key) {
        prop.addListener((obs, oldVal, newVal) -> {
            MainController.markTableDirty();
            System.out.printf("Edited arg=%s, newValue=%s%n", key, newVal);
        });
    }

    // --- Args helper ---
    public List<String> getArgs() {
        if (extras.isEmpty()) {
            return List.of();
        }
        return extras.values().stream()
                .map(SimpleStringProperty::get)
                .toList();
    }






}
