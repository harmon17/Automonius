package org.automonius;

import java.util.UUID;
import java.util.logging.Logger;

public class TestNode {
    private static final Logger log = Logger.getLogger(TestNode.class.getName());

    private String name;
    private final NodeType type;
    private final String id;

    // References to backing model objects
    private TestSuite suiteRef;
    private TestScenario scenarioRef;
    private TestStep stepRef;

    // --- Constructor ---
    public TestNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
        this.id = UUID.randomUUID().toString();
        log.info(() -> "Created TestNode: id=" + id + ", name=" + name + ", type=" + type);
    }

    // --- Name ---
    public String getName() { return name; }
    public void setName(String name) {
        log.fine(() -> "Renaming node " + id + " from " + this.name + " to " + name);
        this.name = name;
    }

    // --- Type ---
    public NodeType getType() { return type; }

    // --- ID ---
    public String getId() { return id; }

    // --- Suite reference ---
    public TestSuite getSuiteRef() { return suiteRef; }
    public void setSuiteRef(TestSuite suiteRef) {
        this.suiteRef = suiteRef;
        if (suiteRef != null) {
            log.info(() -> "Linked TestSuite " + suiteRef.getId() + " to node " + id);
        }
    }

    // --- Scenario reference ---
    public TestScenario getScenarioRef() { return scenarioRef; }
    public void setScenarioRef(TestScenario scenarioRef) {
        this.scenarioRef = scenarioRef;
        if (scenarioRef != null) {
            log.info(() -> "Linked TestScenario " + scenarioRef.getId() + " to node " + id);
        }
    }

    // --- Step reference ---
    public TestStep getStepRef() { return stepRef; }
    public void setStepRef(TestStep stepRef) {
        this.stepRef = stepRef;
        if (stepRef != null) {
            log.info(() -> "Linked TestStep " + stepRef.getId() + " to node " + id);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}

// --- Enum for node types ---
enum NodeType {
    ROOT,
    SUITE,
    SUB_SUITE,
    TEST_SCENARIO,
    TEST_STEP
}
