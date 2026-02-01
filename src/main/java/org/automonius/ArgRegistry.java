package org.automonius;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ArgRegistry {
    private static final Logger LOG = Logger.getLogger(ArgRegistry.class.getName());

    // Dynamic mapping populated at runtime
    private static final Map<String, List<String>> argsByAction = new ConcurrentHashMap<>();

    private ArgRegistry() {}

    /** Register an action with its expected inputs */
    public static void register(String action, List<String> inputs) {
        argsByAction.put(action, inputs);
        LOG.info(() -> "[ArgRegistry] Registered action=" + action + " args=" + inputs);
    }

    /** Get expected args for an action */
    public static List<String> getArgsForAction(String action) {
        if (action == null || action.isBlank()) return List.of();
        return argsByAction.getOrDefault(action, List.of());
    }
}
