package org.automonius.Controller;

import javafx.beans.property.StringProperty;

/**
 * ArgEntry is a lightweight UI wrapper for arguments in the ListView.
 * It does not hold its own memory — it simply references the underlying
 * StringProperty from TestStep.extras or TestStep.globalExtras.
 */
public class ArgEntry {
    private final int rowIndex;
    private final String stepId;
    private final String name;
    private final StringProperty boundValue; // reference to TestStep property
    private final boolean isHeader;
    private final boolean isGlobal;

    // Header entry (no bound value)
    public ArgEntry(int rowIndex, String stepId, String name) {
        this.rowIndex = rowIndex;
        this.stepId = stepId;
        this.name = name;
        this.boundValue = null;
        this.isGlobal = false;
        this.isHeader = true;
    }

    // Manual arg entry
    public ArgEntry(int rowIndex, String stepId, String name, StringProperty boundValue) {
        this(rowIndex, stepId, name, boundValue, false);
    }

    // Global arg entry
    public ArgEntry(int rowIndex, String stepId, String name, StringProperty boundValue, boolean isGlobal) {
        this.rowIndex = rowIndex;
        this.stepId = stepId;
        this.name = name;
        this.boundValue = boundValue; // reference to TestStep map property
        this.isGlobal = isGlobal;
        this.isHeader = false;
    }

    public int getRowIndex() { return rowIndex; }
    public String getStepId() { return stepId; }
    public String getName() { return name; }
    public StringProperty valueProperty() { return boundValue; }
    public boolean isHeader() { return isHeader; }
    public boolean isGlobal() { return isGlobal; }

    @Override
    public String toString() {
        if (isHeader) return name;
        return (isGlobal ? "[GLOBAL] " : "") +
                "Row " + rowIndex + " [" + stepId + "] " +
                name + "=" + (boundValue != null ? boundValue.get() : "");
    }
}
