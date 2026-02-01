package org.automonius.Controller;

/**
 * Plain DTO for persistence of Global Arguments.
 * Used only for saving/loading JSON (no JavaFX properties).
 */
public class GlobalArgDTO {
    private String description;
    private String fieldName;
    private String value;

    // --- Constructors ---
    public GlobalArgDTO() {
        // no-arg constructor needed for Gson
    }

    public GlobalArgDTO(String description, String fieldName, String value) {
        this.description = description;
        this.fieldName = fieldName;
        this.value = value;
    }

    // --- Getters & Setters ---
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

