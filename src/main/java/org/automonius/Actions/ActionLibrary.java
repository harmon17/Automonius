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
import java.util.Arrays;

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

    @ActionMeta(
            objectName = "Calculator",
            description = "Perform arithmetic on ten numbers with a chosen operation",
            inputs = {
                    "num1","num2","num3","num4","num5",
                    "num6","num7","num8","num9","num10","operation"
            }
    )
    public static double calculateTen(
            double num1, double num2, double num3, double num4, double num5,
            double num6, double num7, double num8, double num9, double num10,
            String operation
    ) {
        double[] nums = {num1,num2,num3,num4,num5,num6,num7,num8,num9,num10};

        switch (operation.toLowerCase()) {
            case "sum":
            case "add":
            case "+":
                return Arrays.stream(nums).sum();

            case "average":
                return Arrays.stream(nums).average().orElse(0);

            case "max":
                return Arrays.stream(nums).max().orElse(Double.NaN);

            case "min":
                return Arrays.stream(nums).min().orElse(Double.NaN);

            case "multiply":
            case "*":
                return Arrays.stream(nums).reduce(1, (a,b) -> a * b);

            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
    }


}

