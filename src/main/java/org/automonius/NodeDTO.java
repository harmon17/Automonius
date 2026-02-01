package org.automonius;

import java.util.*;

/**
 * Data Transfer Object (DTO) for persisting and restoring the TreeView hierarchy.
 * Captures both Scenario-level and Step-level fields in plain Java types
 * (Strings, Maps, Lists) for safe JSON serialization.
 *
 * - Scenario nodes: store scenario status and extras.
 * - Step nodes: store core fields, step-specific extras, and references to global args.
 */
public class NodeDTO {
    private String id;
    private NodeType type;

    // --- Common fields ---
    private String name;                        // Suite/SubSuite/Scenario/Step label
    private List<NodeDTO> children = new ArrayList<>();

    // --- Scenario-specific fields ---
    private String scenarioStatus;              // aggregated status (PASS/FAIL/PENDING)
    private Map<String, String> scenarioExtras; // scenario-level extras (argument headers)

    // --- Step-specific fields ---
    private String item;                        // primary variable reference
    private String action;                      // action name
    private String object;                      // object name
    private String input;                       // first argument
    private String description;                 // step description
    private String stepType;                    // classification of step
    private String stepStatus;                  // execution result
    private Map<String, String> stepExtras;     // dynamic argument values (local)
    private Map<String, String> stepGlobals;    // linked global args (references + values)

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<NodeDTO> getChildren() { return children; }
    public void setChildren(List<NodeDTO> children) { this.children = children; }

    // Scenario fields
    public String getScenarioStatus() { return scenarioStatus; }
    public void setScenarioStatus(String scenarioStatus) { this.scenarioStatus = scenarioStatus; }

    public Map<String, String> getScenarioExtras() { return scenarioExtras; }
    public void setScenarioExtras(Map<String, String> scenarioExtras) { this.scenarioExtras = scenarioExtras; }

    // Step fields
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }

    public String getStepStatus() { return stepStatus; }
    public void setStepStatus(String stepStatus) { this.stepStatus = stepStatus; }

    public Map<String, String> getStepExtras() { return stepExtras; }
    public void setStepExtras(Map<String, String> stepExtras) { this.stepExtras = stepExtras; }

    public Map<String, String> getStepGlobals() { return stepGlobals; }
    public void setStepGlobals(Map<String, String> stepGlobals) { this.stepGlobals = stepGlobals; }
}
