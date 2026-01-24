package org.automonius.Actions.Database;

import org.automonius.Annotations.ActionMeta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {

    @ActionMeta(
            objectName = "Database",
            description = "Run a SQL query against the database",
            inputs = {"query"}
    )
    public static boolean executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb");
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
