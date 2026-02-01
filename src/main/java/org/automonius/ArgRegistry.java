package org.automonius;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Central registry of expected argument keys for each action.
 * Provides validation to catch missing mappings early.
 */
public final class ArgRegistry {

    private static final Logger LOG = Logger.getLogger(ArgRegistry.class.getName());

    // --- Static mapping of actions to expected args ---
    private static final Map<String, List<String>> argsByAction = Map.of(
            // File actions
            "checkFileContainsKeyword", List.of("path", "keyword"),

            // Web actions
            "sendRequest", List.of("url", "method", "headers", "body"),
            "assertResponse", List.of("statusCode", "expectedBody"),

            // UI actions
            "clickButton", List.of("buttonId"),
            "enterText", List.of("selector", "text"),

            // Navigation
            "navigate", List.of("url"),

            // Validation
            "assertText", List.of("selector", "expectedText")
    );

    private ArgRegistry() {
        // prevent instantiation
    }

    /**
     * Get the expected argument keys for a given action.
     * Logs a warning if the action is not defined in the registry.
     */
    public static List<String> getArgsForAction(String action) {
        if (action == null || action.isBlank()) {
            return List.of();
        }
        List<String> args = argsByAction.get(action);
        if (args == null) {
            LOG.warning("[ArgRegistry] No mapping found for action: " + action);
            return List.of();
        }
        return args;
    }
}
