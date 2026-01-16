package org.automonius.exec;

import org.automonius.Actions.ActionLibrary;
import org.automonius.Annotations.ActionMeta;

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

    public static Object runTest(TestCase testCase) {
        try {
            for (Method method : ActionLibrary.class.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ActionMeta.class)) {
                    if (method.getName().equals(testCase.getActionName())) {
                        // Split the input string into arguments
                        String[] args = testCase.getInput().split(",");
                        // Trim whitespace
                        for (int i = 0; i < args.length; i++) {
                            args[i] = args[i].trim();
                        }
                        // Invoke with arguments
                        return method.invoke(null, (Object[]) args);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}