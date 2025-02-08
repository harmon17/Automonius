package org.automonius;

public class TestStep {
    private String steps;
    private String objectName;

    public TestStep(String steps, String objectName) {
        this.steps = steps;
        this.objectName = objectName;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }
}
