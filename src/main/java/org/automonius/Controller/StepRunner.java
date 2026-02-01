package org.automonius.Controller;


import javafx.beans.property.StringProperty;

public class StepRunner {

    // Run a whole scenario
    public void executeScenario(Scenario scenario) {
        System.out.println("=== Executing Scenario: " + scenario.getName() + " ===");
        for (Step step : scenario.getSteps()) {
            executeStep(step);
        }
        System.out.println("=== Scenario Complete ===");
    }

    // Run a single step
    private void executeStep(Step step) {
        System.out.println("Step: " + step.getName());

        for (var entry : step.getExtras().entrySet()) {
            String rawValue = entry.getValue().get();
            String resolvedValue = resolveArg(rawValue);

            System.out.println("Arg [" + entry.getKey() + "] = " + resolvedValue);

            // TODO: plug resolvedValue into your actual driver/API logic
        }
    }

    // Resolve tokens against GlobalArgsManager
    private String resolveArg(String token) {
        if (token != null && token.startsWith("{") && token.endsWith("}")) {
            String inner = token.substring(1, token.length() - 1);
            String[] parts = inner.split(":", 2);

            if (parts.length == 2) {
                String varName = parts[1];
                StringProperty valueProp = GlobalArgsManager.getGlobalArgs().get(varName);

                if (valueProp != null) {
                    String resolved = valueProp.get();
                    System.out.println("[GLOBAL] " + varName + " = " + resolved);
                    return resolved;
                } else {
                    System.out.println("⚠ Missing global arg: " + varName);
                    return "";
                }
            }
        }
        System.out.println("[LITERAL] " + token);
        return token;
    }
}

