package camera;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CameraService {
    private static final String BASE_URL = "http://localhost:8081/api";

    public List<Camera> getAllCameras() throws IOException {
        HttpURLConnection conn = createConnection(BASE_URL + "/cameras", "GET");
        if (conn.getResponseCode() != 200) throw new IOException("GET /cameras failed: " + conn.getResponseCode());
        String json = readResponse(conn);
        return parseCameraArray(json);
    }

    public Camera getCamera(int id) throws IOException {
        HttpURLConnection conn = createConnection(BASE_URL + "/cameras/" + id, "GET");
        if (conn.getResponseCode() != 200) throw new IOException("GET /cameras/" + id + " failed");
        String json = readResponse(conn);
        return parseSingleCamera(json);
    }

    public void sendCameraAction(int id, String action) throws IOException {
        String jsonBody = "{\"action\":\"" + action + "\"}";
        HttpURLConnection conn = createConnection(BASE_URL + "/cameras/" + id + "/record", "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code != 200 && code != 204) throw new IOException("POST action failed: " + code);
    }

    public void sendAllCamerasAction(String action) throws IOException {
        String jsonBody = "{\"action\":\"" + action + "\"}";
        HttpURLConnection conn = createConnection(BASE_URL + "/cameras/all/record", "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        if (code != 200 && code != 204) throw new IOException("POST all action failed: " + code);
    }

    private HttpURLConnection createConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private List<Camera> parseCameraArray(String json) {
        List<Camera> list = new ArrayList<>();
        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return list;
            String[] parts = json.split("\\},\\{");
            for (String part : parts) {
                part = part.replace("{", "").replace("}", "").trim();
                Camera c = parseCameraFromObject(part);
                if (c != null) list.add(c);
            }
        }
        return list;
    }

    private Camera parseSingleCamera(String json) {
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            return parseCameraFromObject(json);
        }
        return null;
    }

    private Camera parseCameraFromObject(String objStr) {
        String[] pairs = objStr.split(",");
        int id = -1;
        String status = null;
        String type = "HTTP";
        String streamUrl = "";
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                if (key.equals("cameraId") || key.equals("camerald")) id = Integer.parseInt(value);
                else if (key.equals("status")) status = value;
                else if (key.equals("type")) type = value;
                else if (key.equals("streamUrl")) streamUrl = value;
            }
        }
        return (id != -1 && status != null) ? new Camera(id, status, type, streamUrl) : null;
    }
}
