package org.automonius;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class TestSuite {
    private static final Logger log = Logger.getLogger(TestSuite.class.getName());

    private final String id;
    private String name;
    private final List<TestScenario> scenarios = new ArrayList<>();
    private final List<TestSuite> subSuites = new ArrayList<>(); // NEW

    // Existing constructor (auto-generates UUID)
    public TestSuite(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    // New constructor (explicit id + name)
    public TestSuite(String id, String name) {
        this.id = id;
        this.name = name;
        log.info(() -> "Created TestSuite: id=" + id + ", name=" + name);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) {
        log.fine(() -> "Renaming suite " + id + " from " + this.name + " to " + name);
        this.name = name;
    }

    public List<TestScenario> getScenarios() { return scenarios; }
    public List<TestSuite> getSubSuites() { return subSuites; } // NEW

    public void addScenario(TestScenario scenario) {
        scenarios.add(scenario);
        log.fine(() -> "Added scenario " + scenario.getId() + " to suite " + id);
    }

    public void removeScenario(TestScenario scenario) {
        scenarios.remove(scenario);
        log.fine(() -> "Removed scenario " + scenario.getId() + " from suite " + id);
    }

    public void addSubSuite(TestSuite subSuite) { // NEW
        subSuites.add(subSuite);
        log.fine(() -> "Added sub-suite " + subSuite.getId() + " to suite " + id);
    }

    public void removeSubSuite(TestSuite subSuite) { // NEW
        subSuites.remove(subSuite);
        log.fine(() -> "Removed sub-suite " + subSuite.getId() + " from suite " + id);
    }
}
