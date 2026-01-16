package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import java.util.HashMap;
import java.util.Map;

public class TestStep {
    private final SimpleStringProperty item;
    private final SimpleStringProperty action;
    private final SimpleStringProperty object;
    private final SimpleStringProperty input;
    private final SimpleStringProperty description;

    // Unlimited extras stored by column name
    private final Map<String, SimpleStringProperty> extras = new HashMap<>();

    // Default constructor for blank rows
    public TestStep() {
        this("", "", "", "");
    }

    public TestStep(String item, String action, String object, String input) {
        this.item = new SimpleStringProperty(item == null ? "" : item);
        this.action = new SimpleStringProperty(action == null ? "" : action);
        this.object = new SimpleStringProperty(object == null ? "" : object);
        this.input = new SimpleStringProperty(input == null ? "" : input);
        this.description = new SimpleStringProperty("");
        // ❌ removed "this.extra" — no single extra field anymore
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

    // --- Dynamic extras ---
    public SimpleStringProperty getExtraProperty(String columnName) {
        return extras.computeIfAbsent(columnName, k -> new SimpleStringProperty(""));
    }

    public String getExtra(String columnName) {
        return getExtraProperty(columnName).get();
    }

    public void setExtra(String columnName, String value) {
        getExtraProperty(columnName).set(value);
    }

    // Optional: expose all extras for cloning/export
    public Map<String, SimpleStringProperty> getExtras() {
        return extras;
    }
}
