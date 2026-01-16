package org.automonius.exec;

public class TestCase {
    private final String objectName;
    private final String actionName;
    private final String description;
    private final String input;

    public TestCase(String objectName, String actionName, String description, String input) {
        this.objectName = objectName;
        this.actionName = actionName;
        this.description = description;
        this.input = input;
    }

    public String getObjectName() { return objectName; }
    public String getActionName() { return actionName; }
    public String getDescription() { return description; }
    public String getInput() { return input; }
}

