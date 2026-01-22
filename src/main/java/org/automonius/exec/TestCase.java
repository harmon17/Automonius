package org.automonius.exec;

import java.util.List;

public class TestCase {
    private final String objectName;
    private final String actionName;     // formerly methodName
    private final String description;
    private final List<String> inputs;   // store as list

    // Constructor
    public TestCase(String objectName, String actionName, String description, List<String> inputs) {
        this.objectName = objectName;
        this.actionName = actionName;
        this.description = description;
        this.inputs = inputs;
    }

    // --- Getters ---
    public String getObjectName() {
        return objectName;
    }

    public String getActionName() {
        return actionName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getInputs() {
        return inputs;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "objectName='" + objectName + '\'' +
                ", actionName='" + actionName + '\'' +
                ", description='" + description + '\'' +
                ", inputs=" + inputs +
                '}';
    }
}
