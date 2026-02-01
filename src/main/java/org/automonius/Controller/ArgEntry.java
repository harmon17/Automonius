package org.automonius.Controller;

import javafx.beans.property.StringProperty;

public class ArgEntry {
    private final int rowIndex;
    private final String stepId;
    private final String name;
    private final StringProperty value;
    private final boolean isHeader;
    private final boolean isGlobal;

    // Normal arg entry
    public ArgEntry(int rowIndex, String stepId, String name, StringProperty value) {
        this(rowIndex, stepId, name, value, false, name.startsWith("Row "));
    }

    // Global arg entry
    public ArgEntry(int rowIndex, String stepId, String name, StringProperty value, boolean isGlobal) {
        this(rowIndex, stepId, name, value, isGlobal, name.startsWith("Row "));
    }

    private ArgEntry(int rowIndex, String stepId, String name, StringProperty value,
                     boolean isGlobal, boolean isHeader) {
        this.rowIndex = rowIndex;
        this.stepId = stepId;
        this.name = name;
        this.value = value;
        this.isGlobal = isGlobal;
        this.isHeader = isHeader;
    }

    public int getRowIndex() { return rowIndex; }
    public String getStepId() { return stepId; }
    public String getName() { return name; }
    public StringProperty valueProperty() { return value; }
    public boolean isHeader() { return isHeader; }
    public boolean isGlobal() { return isGlobal; }

    @Override
    public String toString() {
        if (isHeader) return name;
        return (isGlobal ? "[GLOBAL] " : "") +
                "Row " + rowIndex + " [" + stepId + "] " +
                name + "=" + value.get();
    }
}
