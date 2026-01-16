package org.automonius;

public class TestNode {
    private final String name;
    private final NodeType type;

    public TestNode(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public NodeType getType() { return type; }

    @Override
    public String toString() {
        return name; // ensures TreeView displays the name
    }
}

enum NodeType {
    ROOT, SUITE, SUB_SUITE, TEST_SCENARIO
}
