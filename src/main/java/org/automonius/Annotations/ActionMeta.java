package org.automonius.Annotations;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to describe automation actions.
 * Carries grouping (objectName), description, and expected inputs.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionMeta {
    String objectName();     // Grouping: FileSystem, Database, WebServer, etc.
    String description();    // Human-readable description
    String[] inputs();       // Expected inputs (parameter names)
}

