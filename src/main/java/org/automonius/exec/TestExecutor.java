package org.automonius.exec;

import org.automonius.Annotations.ActionMeta;
import org.automonius.TestStep;
import java.lang.reflect.Method;
import java.util.*;
import java.io.File;
import java.net.URL;

/**
 * Discovers automation actions annotated with @ActionMeta, grouped by Action.
 */
public class TestExecutor {

    // === Discover actions keyed by Action name ===
    public static Map<String, TestCase> discoverActionsByAction(String packageName) {
        Map<String, TestCase> grouped = new HashMap<>();
        String path = packageName.replace('.', '/');

        // Locate the directory on the classpath
        URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
        if (resource == null) {
            System.err.println("⚠️ Could not find package: " + packageName);
            return grouped;
        }

        File dir = new File(resource.getFile());
        try {
            scanDirectoryByAction(dir, packageName, grouped);
        } catch (ClassNotFoundException e) {
            System.err.println("⚠️ Error scanning package: " + packageName);
            e.printStackTrace();
        }

        return grouped;
    }



    private static void scanDirectoryByAction(File dir, String packageName, Map<String, TestCase> grouped)
            throws ClassNotFoundException {
        if (dir == null || !dir.exists()) return;

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                scanDirectoryByAction(file, packageName + "." + file.getName(), grouped);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(ActionMeta.class)) {
                        ActionMeta meta = method.getAnnotation(ActionMeta.class);

                        // ✅ include declaring class name
                        TestCase tc = new TestCase(
                                meta.objectName(),
                                method.getName(),
                                meta.description(),
                                Arrays.asList(meta.inputs()),
                                clazz.getName()   // new field in TestCase
                        );

                        // Keyed by Action name
                        grouped.put(method.getName(), tc);
                    }
                }
            }
        }
    }


    // === Get inputs keyed by Action name ===
    public static Map<String, List<String>> getInputsByAction(String packageName) {
        Map<String, List<String>> inputsByAction = new HashMap<>();
        Map<String, TestCase> discovered = discoverActionsByAction(packageName);

        for (Map.Entry<String, TestCase> entry : discovered.entrySet()) {
            inputsByAction.put(entry.getKey(), entry.getValue().getInputs());
        }
        return inputsByAction;
    }

    // === Run test by Action ===
    public static Object runTest(TestStep step) {
        try {
            Map<String, TestCase> actions = discoverActionsByAction("org.automonius.Actions");
            TestCase tc = actions.get(step.getAction());

            if (tc != null) {
                // ✅ Use the actual declaring class name stored in TestCase
                Class<?> clazz = Class.forName(tc.getDeclaringClass());

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(ActionMeta.class)
                            && method.getName().equals(step.getAction())) {

                        ActionMeta meta = method.getAnnotation(ActionMeta.class);
                        int paramCount = method.getParameterCount();

                        // Collect arguments
                        List<String> args = new ArrayList<>();
                        String[] inputNames = meta.inputs();
                        for (int i = 0; i < paramCount; i++) {
                            String name = (i < inputNames.length ? inputNames[i] : "arg" + (i + 1));
                            args.add(step.getExtra(name));
                        }

                        if (args.stream().anyMatch(a -> a == null || a.isBlank())) {
                            System.out.println("⚠️ Skipped execution: arguments not yet provided");
                            return null;
                        }

                        if (args.size() != paramCount) {
                            System.out.println("⚠️ Expected " + paramCount + " args, got " + args.size());
                            return null;
                        }

                        // Convert args
                        Class<?>[] paramTypes = method.getParameterTypes();
                        Object[] convertedArgs = new Object[paramCount];
                        for (int i = 0; i < paramCount; i++) {
                            String raw = args.get(i);
                            Class<?> targetType = paramTypes[i];
                            if (targetType == int.class || targetType == Integer.class) {
                                convertedArgs[i] = (raw == null || raw.isEmpty()) ? 0 : Integer.parseInt(raw);
                            } else if (targetType == double.class || targetType == Double.class) {
                                convertedArgs[i] = (raw == null || raw.isEmpty()) ? 0.0 : Double.parseDouble(raw);
                            } else if (targetType == long.class || targetType == Long.class) {
                                convertedArgs[i] = (raw == null || raw.isEmpty()) ? 0L : Long.parseLong(raw);
                            } else if (targetType == float.class || targetType == Float.class) {
                                convertedArgs[i] = (raw == null || raw.isEmpty()) ? 0f : Float.parseFloat(raw);
                            } else if (targetType == boolean.class || targetType == Boolean.class) {
                                convertedArgs[i] = (raw != null && Boolean.parseBoolean(raw));
                            } else {
                                convertedArgs[i] = raw;
                            }
                        }

                        Object result = method.invoke(null, convertedArgs);

                        System.out.println("=== Test Execution ===");
                        System.out.println("Action: " + step.getAction());
                        System.out.println("Object: " + tc.getObjectName());
                        System.out.println("Description: " + tc.getDescription());
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
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error running action: " + (step != null ? step.getAction() : "unknown"));
            e.printStackTrace();
        }
        return null;
    }


}
