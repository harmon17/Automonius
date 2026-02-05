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
    private final String name;          // argument name or header text
    private final StringProperty value; // bound value
    private final boolean header;
    private final boolean global;

    // Header constructor
    public ArgEntry(int rowIndex, String stepId, String name) {
        this.rowIndex = rowIndex;
        this.stepId = stepId;
        this.name = name;
        this.value = null;   // lightweight: no dummy property
        this.header = true;
        this.global = false;
    }

    // Argument constructor (manual/local)
    public ArgEntry(int rowIndex, String stepId, String name, StringProperty value) {
        this(rowIndex, stepId, name, value, false, false);
    }

    // Argument constructor (global)
    public ArgEntry(int rowIndex, String stepId, String name, StringProperty value, boolean global) {
        this(rowIndex, stepId, name, value, false, global);
    }

    private ArgEntry(int rowIndex, String stepId, String name, StringProperty value,
                     boolean header, boolean global) {
        this.rowIndex = rowIndex;
        this.stepId = stepId;
        this.name = name;
        this.value = value;
        this.header = header;
        this.global = global;
    }

    // --- Getters ---
    public int getRowIndex() { return rowIndex; }
    public String getStepId() { return stepId; }
    public String getName() { return name; }

    // Alias for clarity (fixes "getArgName" calls)
    public String getArgName() { return name; }

    public StringProperty valueProperty() { return value; }
    public boolean isHeader() { return header; }
    public boolean isGlobal() { return global; }

    /**
     * Returns the text to display in the ListView cell.
     * This is used by the cell factory for consistent styling.
     */
    public String getDisplayText() {
        if (isHeader()) {
            return name; // header text only
        }
        return name + "=" + (value != null ? value.get() : "");
    }

    @Override
    public String toString() {
        // Fallback display if cell factory isn’t applied
        return getDisplayText();
    }
}
