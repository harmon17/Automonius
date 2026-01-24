    package org.automonius.Actions.WebServer;

    import org.automonius.Annotations.ActionMeta;

    import java.net.URI;
    import java.net.http.HttpClient;
    import java.net.http.HttpRequest;
    import java.net.http.HttpResponse;

    public class BrowserCheck {
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
