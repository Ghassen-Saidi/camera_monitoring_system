package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler implements Runnable {
    private final Socket socket;
    private final CameraController cameraController;
    private final ObjectMapper objectMapper;

    public RequestHandler(Socket socket) {
        this.socket = socket;
        this.cameraController = new CameraController();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void run() {
        try (InputStream is = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            // Read request line
            String requestLine = reader.readLine();
            if (requestLine == null) return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) return;
            String method = parts[0];
            String path = parts[1];

            // Read headers
            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                String[] header = line.split(": ", 2);
                if (header.length == 2) headers.put(header[0].toLowerCase(), header[1]);
            }

            // Read body if POST
            String body = "";
            if ("POST".equalsIgnoreCase(method)) {
                int contentLength = Integer.parseInt(headers.getOrDefault("content-length", "0"));
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    reader.read(buffer);
                    body = new String(buffer);
                }
            }

            // Dispatch to controller and get response
            String responseBody;
            int statusCode;
            try {
                responseBody = dispatch(method, path, body);
                statusCode = 200;
            } catch (IllegalArgumentException e) {
                responseBody = "{\"error\":\"" + e.getMessage() + "\"}";
                statusCode = 400;
            } catch (Exception e) {
                responseBody = "{\"error\":\"Internal Server Error\"}";
                statusCode = 500;
            }

            // Send HTTP response
            String httpResponse = "HTTP/1.1 " + statusCode + " " + getStatusText(statusCode) + "\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    responseBody;
            os.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            os.flush();

        } catch (IOException e) {
            System.err.println("Handler error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String dispatch(String method, String path, String body) throws Exception {
        // GET /api/cameras
        if (method.equals("GET") && path.equals("/api/cameras")) {
            return cameraController.getAllCameras();
        }
        // GET /api/cameras/{id}
        Pattern getOne = Pattern.compile("/api/cameras/(\\d+)");
        Matcher m1 = getOne.matcher(path);
        if (method.equals("GET") && m1.matches()) {
            int id = Integer.parseInt(m1.group(1));
            return cameraController.getCamera(id);
        }
        // POST /api/cameras/{id}/record
        Pattern postOne = Pattern.compile("/api/cameras/(\\d+)/record");
        Matcher m2 = postOne.matcher(path);
        if (method.equals("POST") && m2.matches()) {
            int id = Integer.parseInt(m2.group(1));
            Map<String, String> payload = objectMapper.readValue(body, Map.class);
            String action = payload.get("action");
            if (action == null) throw new IllegalArgumentException("Missing 'action' field");
            return cameraController.updateCamera(id, action);
        }
        // POST /api/cameras/all/record
        if (method.equals("POST") && path.equals("/api/cameras/all/record")) {
            Map<String, String> payload = objectMapper.readValue(body, Map.class);
            String action = payload.get("action");
            if (action == null) throw new IllegalArgumentException("Missing 'action' field");
            return cameraController.updateAllCameras(action);
        }
        throw new IllegalArgumentException("Endpoint not found");
    }

    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }
}