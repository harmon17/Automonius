package org.automonius.Controller;

import java.util.ArrayList;
import java.util.List;

public class Scenario {
    private String name;
    private List<Step> steps = new ArrayList<>();

    public Scenario(String name) {
        this.name = name;
    }

    // Getter and Setter for name
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Steps list
    public List<Step> getSteps() { return steps; }

    // Helper to add a step
    public void addStep(Step step) {
        steps.add(step);
    }
}
