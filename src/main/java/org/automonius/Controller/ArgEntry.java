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
    private final StringProperty value;
    private final boolean header;
    private final boolean global;

    // Header constructor
    public ArgEntry(int rowIndex, String stepId, String name) {
        this.rowIndex = rowIndex;
        this.stepId = stepId;
        this.name = name;
        this.value = null;
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
    public StringProperty valueProperty() { return value; }
    public boolean isHeader() { return header; }
    public boolean isGlobal() { return global; }   // ✅ Added this
}
