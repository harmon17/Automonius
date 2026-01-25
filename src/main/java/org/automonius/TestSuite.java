    package org.automonius;

    import javafx.beans.property.SimpleStringProperty;
    import javafx.beans.property.StringProperty;
    import javafx.collections.FXCollections;
    import javafx.collections.ObservableList;

    import java.util.UUID;
    import java.util.logging.Logger;

    public class TestSuite {
        private static final Logger log = Logger.getLogger(TestSuite.class.getName());

        private final String id;
        private final StringProperty name = new SimpleStringProperty();

        // Reactive lists for scenarios and sub-suites
        private final ObservableList<TestScenario> scenarios = FXCollections.observableArrayList();
        private final ObservableList<TestSuite> subSuites = FXCollections.observableArrayList();

        // --- Constructors ---

        // Public constructor: auto-generates UUID
        public TestSuite(String name) {
            this(UUID.randomUUID().toString(), name);
        }

        // Explicit constructor: id + name
        public TestSuite(String id, String name) {
            this.id = id;
            this.name.set(name);
            log.info(() -> "Created TestSuite: id=" + id + ", name=" + name);
        }

        // --- ID ---
        public String getId() { return id; }

        // --- Name ---
        public String getName() { return name.get(); }
        public void setName(String newName) {
            log.fine(() -> "Renaming suite " + id + " from " + this.name.get() + " to " + newName);
            this.name.set(newName);
        }
        public StringProperty nameProperty() { return name; }

        // --- Scenarios ---
        // --- Scenarios ---
        public ObservableList<TestScenario> getScenarios() {
            return scenarios;
        }

        public void addScenario(TestScenario scenario) {
            scenarios.add(scenario);
            log.fine(() -> "Added scenario " + scenario.getId() + " to suite " + id);
        }
        public void removeScenario(TestScenario scenario) {
            scenarios.remove(scenario);
            log.fine(() -> "Removed scenario " + scenario.getId() + " from suite " + id);
        }

        // --- Sub-suites ---
        public ObservableList<TestSuite> getSubSuites() { return subSuites; }
        public void addSubSuite(TestSuite subSuite) {
            subSuites.add(subSuite);
            log.fine(() -> "Added sub-suite " + subSuite.getId() + " to suite " + id);
        }
        public void removeSubSuite(TestSuite subSuite) {
            subSuites.remove(subSuite);
            log.fine(() -> "Removed sub-suite " + subSuite.getId() + " from suite " + id);
        }
    }
