package org.automonius.Model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Variable {
    private final StringProperty name;
    private final StringProperty type;
    private final StringProperty value;
    private final StringProperty scope;

    public Variable(String name, String type, String value, String scope) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.value = new SimpleStringProperty(value);
        this.scope = new SimpleStringProperty(scope);
    }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public String getType() { return type.get(); }
    public void setType(String type) { this.type.set(type); }
    public StringProperty typeProperty() { return type; }

    public String getValue() { return value.get(); }
    public void setValue(String value) { this.value.set(value); }
    public StringProperty valueProperty() { return value; }

    public String getScope() { return scope.get(); }
    public void setScope(String scope) { this.scope.set(scope); }
    public StringProperty scopeProperty() { return scope; }
}
