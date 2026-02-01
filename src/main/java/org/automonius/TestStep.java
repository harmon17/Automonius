package org.automonius;

import javafx.beans.property.*;
import org.automonius.Controller.ArgEntry;
import org.automonius.exec.TestCase;

import java.util.*;


/**
 * Represents a single test step row in the TableView.
 * Extras and global extras are stored as JavaFX StringProperty for live binding.
 */
public class TestStep {

    // --- Identity ---
    private final String id;

    // --- Core fields ---
    private final StringProperty item;
    private final StringProperty action;
    private final StringProperty object;
    private final StringProperty input;
    private final StringProperty description;
    private final StringProperty type;
    private final StringProperty status;

    // --- Extras ---
    private final Map<String, StringProperty> extras = new LinkedHashMap<>();       // step-specific args
    private final Map<String, StringProperty> globalExtras = new LinkedHashMap<>(); // global args

    private int maxArgs = 0;
    private boolean isNew = false;

    // --- Selection flag for multi-run ---
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    // --- Constructors ---

    /** Default blank step */
    public TestStep() {
        this(UUID.randomUUID().toString(), "", "", "", "", "", "Step", "NEW", null, 0, true);
    }

    /** Core constructor */
    public TestStep(String item, String action, String object, String input) {
        this(UUID.randomUUID().toString(), item, action, object, input, "", "Step", "NEW", null, 0, true);
    }

    /** Construct from TestCase */
    public TestStep(TestCase tc) {
        this(UUID.randomUUID().toString(),
                "", tc.getActionName(), tc.getObjectName(),
                tc.getInputs().isEmpty() ? "" : tc.getInputs().get(0),
                tc.getDescription(), "Step", "NEW", null, tc.getInputs().size(), true);

        tc.getInputs().forEach(arg -> this.setExtra(arg, ""));
    }

    /** Deep copy constructor (preserve ID) */
    public TestStep(TestStep original) {
        this(original.getId() == null || original.getId().isBlank()
                        ? UUID.randomUUID().toString()
                        : original.getId(),
                original.getItem(),
                original.getAction(),
                original.getObject(),
                original.getInput(),
                original.getDescription(),
                original.getType(),
                original.getStatus(),
                original.getExtras(),   // ✅ pass Map<String,StringProperty>
                0,                      // placeholder, will be recalculated
                false);

        // Copy global extras separately
        original.getGlobalExtras().forEach((k, v) ->
                this.globalExtras.put(k, new SimpleStringProperty(v.get()))
        );

        // ✅ Recalculate maxArgs from ArgRegistry
        List<String> expectedArgs = ArgRegistry.getArgsForAction(original.getAction());
        this.maxArgs = expectedArgs.size();
    }



    /** Explicit constructor with ID (for DTO restore) */
    public TestStep(String id,
                    String item,
                    String action,
                    String object,
                    String input,
                    String description,
                    String type,
                    String status,
                    Map<String, StringProperty> extrasMap,
                    int maxArgs,
                    boolean isNew) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.item = new SimpleStringProperty(item == null ? "" : item);
        this.action = new SimpleStringProperty(action == null ? "" : action);
        this.object = new SimpleStringProperty(object == null ? "" : object);
        this.input = new SimpleStringProperty(input == null ? "" : input);
        this.description = new SimpleStringProperty(description == null ? "" : description);
        this.type = new SimpleStringProperty(type == null ? "Step" : type);
        this.status = new SimpleStringProperty(status == null ? "NEW" : status);
        this.isNew = isNew;

        if (extrasMap != null) {
            extrasMap.forEach((k, prop) -> {
                attachDirtyListener(prop, k);
                this.extras.put(k, prop);
            });
        }

        // ✅ Align maxArgs with ArgRegistry
        List<String> expectedArgs = ArgRegistry.getArgsForAction(action);
        this.maxArgs = expectedArgs.size();
    }



    // --- Copy helpers ---
    public static TestStep deepCopy(TestStep original) {
        return (original == null) ? null : new TestStep(original);
    }

    public static TestStep copyTemplate(TestStep template) {
        if (template == null) return new TestStep();

        TestStep step = new TestStep();
        step.setItem(template.getItem());
        step.setObject(template.getObject());
        step.setAction(template.getAction());
        step.setDescription(template.getDescription());
        step.setType(template.getType());
        step.setStatus("NEW");
        step.setMaxArgs(template.getMaxArgs());

        // Copy step extras but blank values
        template.getExtras().forEach((k, v) -> step.setExtra(k, ""));
        step.setNew(true);
        return step;
    }

    // --- Identity & Flags ---
    public String getId() { return id; }
    public boolean isNew() { return isNew; }
    public void setNew(boolean value) { this.isNew = value; }

    // --- Selection ---
    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean value) { selected.set(value); }

    // --- Core Getters ---
    public String getItem() { return item.get(); }
    public String getAction() { return action.get(); }
    public String getObject() { return object.get(); }
    public String getInput() { return input.get(); }
    public String getDescription() { return description.get(); }
    public String getType() { return type.get(); }
    public String getStatus() { return status.get(); }

    // --- Core Setters ---
    public void setItem(String value) { this.item.set(value); }
    public void setAction(String value) { this.action.set(value); }
    public void setObject(String value) { this.object.set(value); }
    public void setInput(String value) { this.input.set(value); }
    public void setDescription(String value) { this.description.set(value); }
    public void setType(String value) { this.type.set(value); }
    public void setStatus(String value) { this.status.set(value); }

    // --- Property accessors ---
    public StringProperty itemProperty() { return item; }
    public StringProperty actionProperty() { return action; }
    public StringProperty objectProperty() { return object; }
    public StringProperty inputProperty() { return input; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty typeProperty() { return type; }
    public StringProperty statusProperty() { return status; }

    // --- Extras ---
    public StringProperty getExtraProperty(String columnName) {
        return extras.computeIfAbsent(columnName, k -> {
            StringProperty prop = new SimpleStringProperty("");
            attachDirtyListener(prop, k);
            return prop;
        });
    }

    public String getExtra(String columnName) { return getExtraProperty(columnName).get(); }
    public void setExtra(String columnName, String value) { getExtraProperty(columnName).set(value); }
    public Map<String, StringProperty> getExtras() { return Collections.unmodifiableMap(extras); }

    public void setExtras(Map<String, StringProperty> newExtras) {
        extras.clear();
        if (newExtras != null) {
            newExtras.forEach((key, prop) -> {
                attachDirtyListener(prop, key);
                extras.put(key, prop);
            });
        }

        // --- Ensure expected args remain visible ---
        List<String> expectedArgs = ArgRegistry.getArgsForAction(getAction());
        for (String arg : expectedArgs) {
            extras.computeIfAbsent(arg, k -> {
                StringProperty prop = new SimpleStringProperty("");
                attachDirtyListener(prop, k);
                return prop;
            });
        }
    }


    // --- MaxArgs ---
    public int getMaxArgs() { return maxArgs; }
    public void setMaxArgs(int maxArgs) { this.maxArgs = maxArgs; }

    // --- Args helper ---
    public List<String> getArgs() {
        return extras.values().stream().map(StringProperty::get).toList();
    }

    // --- Dirty tracking ---
    private void attachDirtyListener(StringProperty prop, String key) {
        prop.addListener((obs, oldVal, newVal) -> {
            MainController.markTableDirty();
            System.out.printf("Edited arg=%s, newValue=%s%n", key, newVal);
        });
    }

    // --- Utility: convert extras to plain map ---
    public static Map<String, String> toStringMap(Map<String, StringProperty> original) {
        if (original == null) return null;
        Map<String, String> copy = new LinkedHashMap<>();
        original.forEach((k, v) -> copy.put(k, v.get()));
        return copy;
    }

    // --- Global Extras ---
    public StringProperty getGlobalExtraProperty(String columnName) {
        return globalExtras.computeIfAbsent(columnName, k -> {
            StringProperty prop = new SimpleStringProperty("");
            attachDirtyListener(prop, k);
            return prop;
        });
    }

    public String getGlobalExtra(String columnName) { return getGlobalExtraProperty(columnName).get(); }

    public void setGlobalExtra(String key, StringProperty valueProp) {
        attachDirtyListener(valueProp, key);
        globalExtras.put(key, valueProp);
    }

    public Map<String, StringProperty> getGlobalExtras() {
        return Collections.unmodifiableMap(globalExtras);
    }

    public void setGlobalExtras(Map<String, StringProperty> newGlobals) {
        globalExtras.clear();
        if (newGlobals != null) {
            newGlobals.forEach((key, prop) -> {
                attachDirtyListener(prop, key);
                globalExtras.put(key, prop);
            });
        }
    }

    // --- Execution args merge ---
    public Map<String, String> getAllArgsForExecution() {
        Map<String, String> merged = new LinkedHashMap<>();
        // Step-specific extras first
        extras.forEach((k, v) -> merged.put(k, v.get()));
        // Global extras second (do not overwrite step-specific values)
        globalExtras.forEach((k, v) -> merged.putIfAbsent(k, v.get()));
        return merged;
    }

    // --- Convert to ArgEntry list for ListView ---
    public List<ArgEntry> toArgEntries(int rowIndex) {
        List<ArgEntry> entries = new ArrayList<>();

        // Step extras
        extras.forEach((k, v) ->
                entries.add(new ArgEntry(rowIndex, id, k, v, false)) // false = not header/global
        );

        // Global extras
        globalExtras.forEach((k, v) ->
                entries.add(new ArgEntry(rowIndex, id, k, v, true)) // true = global
        );

        return entries;
    }

    // --- Promote/Demote helpers ---
    public void promoteToGlobal(String key, StringProperty globalProp) {
        extras.remove(key);              // remove manual
        setGlobalExtra(key, globalProp); // add global
    }

    public void demoteToExtra(String key, String value) {
        globalExtras.remove(key);        // remove global
        setExtra(key, value);            // add manual
    }

    // --- Debugging ---
    @Override
    public String toString() {
        return "TestStep{" +
                "id=" + id +
                ", item=" + getItem() +
                ", action=" + getAction() +
                ", object=" + getObject() +
                ", input=" + getInput() +
                ", description=" + getDescription() +
                ", type=" + getType() +
                ", status=" + getStatus() +
                ", selected=" + isSelected() +
                ", maxArgs=" + maxArgs +
                ", extras=" + extras.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().get())
                .toList() +
                ", globals=" + globalExtras.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().get())
                .toList() +
                '}';
    }
}
