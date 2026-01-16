package org.automonius.Actions;

import org.automonius.Annotations.ActionMeta;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Central library of automation actions.
 * Each method is annotated with @ActionMeta so the framework can discover it.
 */
public class ActionLibrary {

    @ActionMeta(
            objectName = "FileSystem",
            description = "Verify that a file contains a given keyword",
            inputs = {"path", "keyword"}
    )
    public static boolean checkFileContainsKeyword(String path, String keyword) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) return false;
        try {
            return Files.readString(file.toPath()).contains(keyword);
        } catch (Exception e) {
            return false;
        }
    }

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

    @ActionMeta(
            objectName = "WebServer",
            description = "Send a GET request to a web server",
            inputs = {"url"}
    )
    public static String sendRequest(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return null;
        }
    }
}

