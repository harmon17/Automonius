package org.automonius.exec;

import java.util.List;

public class TestCase {
    private final String objectName;
    private final String actionName;
    private final String description;
    private final List<String> inputs;
    private final String declaringClass;   // NEW field

    // Constructor
    public TestCase(String objectName, String actionName, String description,
                    List<String> inputs, String declaringClass) {
        this.objectName = objectName;
        this.actionName = actionName;
        this.description = description;
        this.inputs = inputs;
        this.declaringClass = declaringClass;
    }

    // --- Getters ---
    public String getObjectName() { return objectName; }
    public String getActionName() { return actionName; }
    public String getDescription() { return description; }
    public List<String> getInputs() { return inputs; }
    public String getDeclaringClass() { return declaringClass; }

    @Override
    public String toString() {
        return "TestCase{" +
                "objectName='" + objectName + '\'' +
                ", actionName='" + actionName + '\'' +
                ", description='" + description + '\'' +
                ", inputs=" + inputs +
                ", declaringClass='" + declaringClass + '\'' +
                '}';
    }
}
