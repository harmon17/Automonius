package org.automonius;

public class TestNode {
    private String name;       // ✅ remove 'final' so it can be changed
    private final NodeType type;

    public TestNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    // --- Getter ---
    public String getName() {
        return name;
    }

    // --- Setter (needed for renaming) ---
    public void setName(String name) {
        this.name = name;
    }

    // --- Getter for type ---
    public NodeType getType() {
        return type;
    }

    @Override
    public String toString() {
        return name; // ensures TreeView displays the name
    }
}

enum NodeType {
    ROOT, SUITE, SUB_SUITE, TEST_SCENARIO
}
