package org.automonius;

import org.annotations.InputType;

import java.util.HashMap;
import java.util.Map;

public class ActionData {
    private String object;
    private String method;
    private String description;
    private InputType input;
    private final Map<String, String> additionalProperties = new HashMap<>();

    public ActionData(String object, String method, String description, InputType input) {
        this.object = object;
        this.method = method;
        this.description = description;
        this.input = input;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InputType getInput() {
        return input;
    }

    public void setInput(InputType input) {
        this.input = input;
    }

    public String getAdditionalProperty(String key) {
        return additionalProperties.getOrDefault(key, "");
    }

    public void setAdditionalProperty(String key, String value) {
        additionalProperties.put(key, value);
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
}
