package org.automonius;

import org.annotations.InputType;

public class ActionData {
    private String object;
    private String description;
    private String method;
    private InputType input;

    public ActionData(String object, String description, String method, InputType input) {
        this.object = object;
        this.description = description;
        this.method = method;
        this.input = input;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public InputType getInput() {
        return input;
    }

    public void setInput(InputType input) {
        this.input = input;
    }
}
