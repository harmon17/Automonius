package org.automonius.Controller;


import javafx.beans.property.SimpleStringProperty;
import java.util.LinkedHashMap;
import java.util.Map;

public class Step {
    private String name;
    private Map<String, SimpleStringProperty> extras = new LinkedHashMap<>();

    public Step(String name) {
        this.name = name;
    }

    // Getter and Setter for name
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Extras map
    public Map<String, SimpleStringProperty> getExtras() { return extras; }

    // Helper to add an extra
    public void addExtra(String key, String value) {
        extras.put(key, new SimpleStringProperty(value));
    }
}

