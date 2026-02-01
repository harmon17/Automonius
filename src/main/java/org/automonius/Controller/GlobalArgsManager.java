package org.automonius.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.automonius.Controller.GlobalArgDTO;

/**
 * Central manager for Global Arguments.
 * Maintains canonical maps for fast lookup and provides
 * observable list support for TableView binding.
 *
 * Separation:
 *  - stepArgs: used in ListView/TableView argument fields
 *  - payloadArgs: used in payload editor substitution
 */
public class GlobalArgsManager {

    private static final Logger log = Logger.getLogger(GlobalArgsManager.class.getName());

    // ✅ Changed to StringProperty everywhere
    private static final Map<String, StringProperty> globalArgs = new LinkedHashMap<>();
    private static final Map<String, StringProperty> stepArgs = new LinkedHashMap<>();
    private static final Map<String, StringProperty> payloadArgs = new LinkedHashMap<>();


    // --- Accessors ---
    public static Map<String, StringProperty> getGlobalArgs() { return globalArgs; }
    public static Map<String, StringProperty> getStepArgs() { return stepArgs; }
    public static Map<String, StringProperty> getPayloadArgs() { return payloadArgs; }


    // --- Update from List ---
    public static void updateFromList(List<GlobalArg> args) {
        Set<String> seen = new HashSet<>();

        for (GlobalArg arg : args) {
            if (isValid(arg)) {
                String normalized = normalizeKey(arg.getFieldName());
                seen.add(normalized);

                StringProperty existing = globalArgs.get(normalized);
                if (existing != null) {
                    existing.set(arg.getValue()); // ✅ update existing property
                } else {
                    globalArgs.put(normalized, new SimpleStringProperty(arg.getValue()));
                }
            }
        }

        // Remove any globals not in the new list
        globalArgs.keySet().retainAll(seen);

        log.info("Updated globalArgs: " + globalArgs.keySet());
    }


    public static ObservableList<GlobalArg> loadAsObservableList() {
        return FXCollections.observableArrayList(
                globalArgs.entrySet().stream()
                        .map(e -> new GlobalArg(
                                "Global arg",
                                e.getKey(),
                                e.getValue().get()
                        ))
                        .collect(Collectors.toList())
        );
    }

    public static List<GlobalArg> getSnapshot() {
        return globalArgs.entrySet().stream()
                .map(e -> new GlobalArg("Global arg", e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());
    }

    // --- Step Args ---
    public static void updateStepArgsFromList(List<GlobalArg> args) {
        Set<String> seen = new HashSet<>();

        for (GlobalArg arg : args) {
            if (isValid(arg)) {
                String normalized = normalizeKey(arg.getFieldName());
                seen.add(normalized);

                StringProperty existing = stepArgs.get(normalized);
                if (existing != null) {
                    existing.set(arg.getValue()); // ✅ update existing property
                } else {
                    stepArgs.put(normalized, new SimpleStringProperty(arg.getValue()));
                }
            }
        }

        stepArgs.keySet().retainAll(seen);

        log.info("Updated stepArgs: " + stepArgs.keySet());
    }


    public static ObservableList<GlobalArg> loadStepArgsAsObservableList() {
        return FXCollections.observableArrayList(
                stepArgs.entrySet().stream()
                        .map(e -> new GlobalArg("Step arg", e.getKey(), e.getValue().get()))
                        .collect(Collectors.toList())
        );
    }

    public static List<GlobalArg> getStepArgsSnapshot() {
        return stepArgs.entrySet().stream()
                .map(e -> new GlobalArg("Step arg", e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());
    }

    // --- Payload Args ---
    public static void updatePayloadArgsFromList(List<GlobalArg> args) {
        Set<String> seen = new HashSet<>();

        for (GlobalArg arg : args) {
            if (isValid(arg)) {
                String normalized = normalizeKey(arg.getFieldName());
                seen.add(normalized);

                StringProperty existing = payloadArgs.get(normalized);
                if (existing != null) {
                    existing.set(arg.getValue()); // ✅ update existing property
                } else {
                    payloadArgs.put(normalized, new SimpleStringProperty(arg.getValue()));
                }
            }
        }

        payloadArgs.keySet().retainAll(seen);

        log.info("Updated payloadArgs: " + payloadArgs.keySet());
    }


    public static ObservableList<GlobalArg> loadPayloadArgsAsObservableList() {
        return FXCollections.observableArrayList(
                payloadArgs.entrySet().stream()
                        .map(e -> new GlobalArg("Payload arg", e.getKey(), e.getValue().get()))
                        .collect(Collectors.toList())
        );
    }

    public static List<GlobalArg> getPayloadArgsSnapshot() {
        return payloadArgs.entrySet().stream()
                .map(e -> new GlobalArg("Payload arg", e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());
    }

    // --- Common ---
    public static void clearAll() {
        globalArgs.clear();
        stepArgs.clear();
        payloadArgs.clear();
        log.info("Cleared all global args");
    }

    public static boolean isValid(GlobalArg arg) {
        return arg != null
                && arg.getFieldName() != null && !arg.getFieldName().isBlank()
                && arg.getValue() != null && !arg.getValue().isBlank();
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    // --- Persistence ---
    public static void saveToFile(File file, List<GlobalArg> args) throws IOException {
        List<GlobalArgDTO> dtoList = toDTOList(args);

        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(dtoList, writer);
        }
        log.info("Saved " + dtoList.size() + " global args to " + file.getAbsolutePath());
    }

    public static List<GlobalArg> loadFromFile(File file) throws IOException {
        if (!file.exists() || file.length() == 0) {
            log.warning("Global args file missing or empty");
            return Collections.emptyList();
        }

        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<GlobalArgDTO>>() {}.getType();
            List<GlobalArgDTO> dtoList = gson.fromJson(reader, listType);

            if (dtoList == null) return Collections.emptyList();

            List<GlobalArg> argList = fromDTOList(dtoList);

            // ✅ Update manager maps with stable properties
            updateFromList(argList);
            updateStepArgsFromList(argList);
            updatePayloadArgsFromList(argList);

            log.info("Loaded " + argList.size() + " global args from " + file.getAbsolutePath());
            return argList;
        } catch (Exception e) {
            log.warning("Failed to parse global args file: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<GlobalArgDTO> toDTOList(List<GlobalArg> args) {
        return args.stream()
                .map(arg -> new GlobalArgDTO(
                        arg.getDescription(),
                        arg.getFieldName(),
                        arg.getValue()
                ))
                .toList();
    }

    public static List<GlobalArg> fromDTOList(List<GlobalArgDTO> dtoList) {
        return dtoList.stream()
                .map(dto -> new GlobalArg(
                        dto.getDescription(),
                        dto.getFieldName(),
                        dto.getValue()
                ))
                .toList();
    }

}
