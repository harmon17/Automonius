package org.automonius.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GlobalArg {

    private final StringProperty description;
    private final StringProperty fieldName;
    private final StringProperty value;

    public GlobalArg(String description, String fieldName, String value) {
        this.description = new SimpleStringProperty(description);
        this.fieldName = new SimpleStringProperty(fieldName);
        this.value = new SimpleStringProperty(value);
    }

    // --- Properties for binding ---
    public StringProperty descriptionProperty() { return description; }
    public StringProperty fieldNameProperty() { return fieldName; }
    public StringProperty valueProperty() { return value; }

    // --- Getters/Setters ---
    public String getDescription() { return description.get(); }
    public void setDescription(String d) { description.set(d); }

    public String getFieldName() { return fieldName.get(); }
    public void setFieldName(String f) { fieldName.set(f); }

    public String getValue() { return value.get(); }
    public void setValue(String v) { value.set(v); }
}
