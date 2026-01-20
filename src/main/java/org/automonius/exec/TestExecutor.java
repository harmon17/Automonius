package org.automonius.exec;

import org.automonius.Actions.ActionLibrary;
import org.automonius.Annotations.ActionMeta;
import org.automonius.TestStep;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Discovers automation actions annotated with @ActionMeta.
 */
public class TestExecutor {

    public static Map<String, List<TestCase>> discoverActions(Class<?> clazz) {
        Map<String, List<TestCase>> grouped = new HashMap<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = method.getAnnotation(ActionMeta.class);

                TestCase tc = new TestCase(
                        meta.objectName(),
                        method.getName(),
                        meta.description(),
                        String.join(",", meta.inputs())
                );

                grouped.computeIfAbsent(meta.objectName(), k -> new ArrayList<>()).add(tc);
            }
        }
        return grouped;
    }

    public static TestCase getTestByAction(Class<?> clazz, String actionName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = method.getAnnotation(ActionMeta.class);
                if (method.getName().equals(actionName)) {
                    return new TestCase(meta.objectName(), method.getName(), meta.description(), String.join(",", meta.inputs()));
                }
            }
        }
        return null; // not found }
    }

    public static Map<String, List<String>> getActionsByObject(Class<?> clazz) {
        Map<String, List<String>> actionsByObject = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ActionMeta.class)) {
                ActionMeta meta = method.getAnnotation(ActionMeta.class);
                actionsByObject.computeIfAbsent(meta.objectName(), k -> new ArrayList<>()).add(method.getName());
            }
        }
        return actionsByObject;
    }

    public static Object runTest(TestStep step) {
        try {
            for (Method method : ActionLibrary.class.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ActionMeta.class) && method.getName().equals(step.getAction())) {
                    ActionMeta meta = method.getAnnotation(ActionMeta.class);
                    int paramCount = method.getParameterCount();

                    // Collect arguments using annotation-driven names
                    List<String> args = new ArrayList<>();
                    String[] inputNames = meta.inputs();
                    for (int i = 0; i < paramCount; i++) {
                        String name = (i < inputNames.length ? inputNames[i] : "arg" + (i + 1));
                        args.add(step.getExtra(name));
                    }

                    // Validate argument count
                    if (args.size() != paramCount) {
                        throw new IllegalArgumentException("Expected " + paramCount +
                                " args, got " + args.size());
                    }

                    // Convert String args to match parameter types
                    Class<?>[] paramTypes = method.getParameterTypes();
                    Object[] convertedArgs = new Object[paramCount];
                    for (int i = 0; i < paramCount; i++) {
                        String raw = args.get(i);
                        Class<?> targetType = paramTypes[i];

                        if (targetType == int.class || targetType == Integer.class) {
                            convertedArgs[i] = (raw == null || raw.isEmpty()) ? 0 : Integer.parseInt(raw);
                        } else if (targetType == boolean.class || targetType == Boolean.class) {
                            convertedArgs[i] = (raw != null && Boolean.parseBoolean(raw));
                        } else {
                            convertedArgs[i] = raw; // default: keep as String
                        }
                    }

                    // Invoke the method
                    Object result = method.invoke(null, convertedArgs);

                    // ✅ Clean, consistent logging
                    System.out.println("=== Test Execution ===");
                    System.out.println("Action: " + step.getAction());
                    System.out.println("Object: " + step.getObject());
                    System.out.println("Description: " + step.getDescription());

                    for (int i = 0; i < paramCount; i++) {
                        String name = (i < inputNames.length ? inputNames[i] : "arg" + (i + 1));
                        String value = args.get(i) == null ? "" : args.get(i);
                        System.out.println("Args: " + name + "=" + value);
                    }

                    System.out.println("Result: " + result);
                    System.out.println("======================");

                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("Error running action: " + step.getAction());
            e.printStackTrace();
        }
        return null;
    }




}