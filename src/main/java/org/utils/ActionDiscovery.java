package org.utils;

import org.annotations.Action;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ActionDiscovery {
    public static List<Method> discoverActions(Class<?> clazz) {
        List<Method> actions = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Action.class)) {
                actions.add(method);
            }
        }
        return actions;
    }
}
