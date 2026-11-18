package camera;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CameraService {
    private static final String BASE_URL = System.getProperty("camera.api", "http://localhost:8081/api");
    private static final String API_KEY = System.getProperty("camera.apiKey", "");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public java.util.List<Camera> getAllCameras() throws IOException {
        String json = request(BASE_URL + "/cameras", "GET", null);
        return Arrays.asList(objectMapper.readValue(json, Camera[].class));
    }

    public Camera getCamera(int id) throws IOException {
        return objectMapper.readValue(request(BASE_URL + "/cameras/" + id, "GET", null), Camera.class);
    }

    public void sendCameraAction(int id, String action) throws IOException {
        request(BASE_URL + "/cameras/" + id + "/record", "POST", actionBody(action));
    }

    public void sendAllCamerasAction(String action) throws IOException {
        String response = request(BASE_URL + "/cameras/all/record", "POST", actionBody(action));
        JsonNode payload = objectMapper.readTree(response);
        if (payload == null || !payload.path("success").asBoolean(false)) {
            String message = payload == null
                    ? "The server returned an empty bulk-command response"
                    : payload.path("message").asText("One or more cameras could not be updated");
            List<String> failedIds = new ArrayList<>();
            JsonNode results = payload == null ? null : payload.path("results");
            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    if (!result.path("success").asBoolean(false)) {
                        failedIds.add(result.path("cameraId").asText("?"));
                    }
                }
            }
            if (!failedIds.isEmpty()) message += ". Failed camera IDs: " + String.join(", ", failedIds);
            throw new IOException(message);
        }
    }

    private String actionBody(String action) throws IOException {
        return objectMapper.writeValueAsString(Map.of("action", action));
    }

    private String request(String urlString, String method, String body) throws IOException {
        HttpURLConnection connection = createConnection(urlString, method);
        try {
            if (!API_KEY.isBlank()) connection.setRequestProperty("X-API-Key", API_KEY);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code + ": " + response);
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection createConnection(String urlString, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }
}
