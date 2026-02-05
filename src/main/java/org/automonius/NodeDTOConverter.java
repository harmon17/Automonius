package org.automonius;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.LinkedHashMap;
import java.util.Map;

public class NodeDTOConverter {

    // --- Convert TestStep → NodeDTO ---
    public static NodeDTO fromStep(TestStep step) {
        NodeDTO dto = new NodeDTO();
        dto.setId(step.getId());
        dto.setType(NodeType.TEST_STEP);
        dto.setName(step.getDescription());

        dto.setItem(step.getItem());
        dto.setAction(step.getAction());
        dto.setObject(step.getObject());
        dto.setInput(step.getInput());
        dto.setDescription(step.getDescription());
        dto.setStepType(step.getType());
        dto.setStepStatus(step.getStatus());

        // 🔥 Flatten extras: include all expected args for this action
        Map<String, String> extrasMap = new LinkedHashMap<>();
        if (step.getAction() != null && ArgRegistry.getArgsForAction(step.getAction()) != null) {
            for (String argName : ArgRegistry.getArgsForAction(step.getAction())) {
                String value = "";
                if (step.getExtras() != null && step.getExtras().containsKey(argName)) {
                    value = step.getExtras().get(argName).get();
                }
                extrasMap.put(argName, value);
            }
        }
        dto.setStepExtras(extrasMap);

        // 🔥 Flatten globals
        dto.setStepGlobals(TestStep.toStringMap(step.getGlobalExtras()));

        return dto;
    }

    // --- Convert NodeDTO → TestStep ---
    public static TestStep toStep(NodeDTO dto) {
        TestStep step = new TestStep(
                dto.getId(),
                dto.getItem(),
                dto.getAction(),
                dto.getObject(),
                dto.getInput(),
                dto.getDescription(),
                dto.getStepType(),
                dto.getStepStatus(),
                null,
                0,
                false
        );

        // 🔥 Rebuild extras as StringProperty
        Map<String, StringProperty> extrasMap = new LinkedHashMap<>();
        if (dto.getStepExtras() != null) {
            dto.getStepExtras().forEach((k, v) -> extrasMap.put(k, new SimpleStringProperty(v)));
        }

        // Ensure all expected args exist for this action
        if (dto.getAction() != null && ArgRegistry.getArgsForAction(dto.getAction()) != null) {
            for (String argName : ArgRegistry.getArgsForAction(dto.getAction())) {
                extrasMap.putIfAbsent(argName, new SimpleStringProperty(""));
            }
        }
        step.setExtras(extrasMap);

        // 🔥 Rebuild globals as StringProperty
        Map<String, StringProperty> globalsMap = new LinkedHashMap<>();
        if (dto.getStepGlobals() != null) {
            dto.getStepGlobals().forEach((k, v) -> globalsMap.put(k, new SimpleStringProperty(v)));
        }
        step.setGlobalExtras(globalsMap);

        return step;
    }
}
