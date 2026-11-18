package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler implements Runnable {
    private static final int MAX_HEADER_BYTES = 32 * 1024;
    private static final int MAX_BODY_BYTES = 1 * 1024 * 1024;

    private final Socket socket;
    private final CameraController cameraController;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String configuredApiKey = System.getenv().getOrDefault("CAMERA_API_KEY", "");

    public RequestHandler(Socket socket, CameraController cameraController) {
        this.socket = socket;
        this.cameraController = cameraController;
    }

    @Override
    public void run() {
        try (Socket ignored = socket;
             InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            Request request;
            try {
                request = readRequest(input);
            } catch (IllegalArgumentException e) {
                writeError(output, 400, e.getMessage());
                return;
            } catch (SocketTimeoutException e) {
                writeError(output, 408, "Request timed out");
                return;
            }

            if (request == null) return;
            if (!authorized(request.headers())) {
                writeError(output, 401, "Unauthorized");
                return;
            }

            try {
                writeResponse(output, 200,
                        dispatch(request.method(), request.path(), request.body()));
            } catch (NoSuchElementException e) {
                writeError(output, 404, e.getMessage());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                writeError(output, 400, e.getMessage());
            } catch (Exception e) {
                System.err.println("Request processing failed: " + e.getMessage());
                writeError(output, 500, "Internal Server Error");
            }
        } catch (IOException e) {
            System.err.println("Handler error: " + e.getMessage());
        }
    }

    private String dispatch(String method, String rawPath, String body) throws Exception {
        String path = rawPath.split("\\?", 2)[0];

        if ("GET".equals(method) && "/api/cameras".equals(path)) {
            return cameraController.getAllCameras();
        }

        Matcher getOne = Pattern.compile("/api/cameras/(\\d+)").matcher(path);
        if ("GET".equals(method) && getOne.matches()) {
            return cameraController.getCamera(Integer.parseInt(getOne.group(1)));
        }

        Matcher postOne = Pattern.compile("/api/cameras/(\\d+)/record").matcher(path);
        if ("POST".equals(method) && postOne.matches()) {
            return cameraController.updateCamera(
                    Integer.parseInt(postOne.group(1)), readAction(body));
        }

        if ("POST".equals(method) && "/api/cameras/all/record".equals(path)) {
            return cameraController.updateAllCameras(readAction(body));
        }

        throw new NoSuchElementException("Endpoint not found");
    }

    private String readAction(String body) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(body);
        JsonNode action = payload == null ? null : payload.get("action");
        if (action == null || !action.isTextual() || action.asText().isBlank()) {
            throw new IllegalArgumentException("'action' must be a non-empty string");
        }
        return action.asText();
    }

    private Request readRequest(InputStream input) throws IOException {
        byte[] headerBytes = readUntilHeaderEnd(input);
        if (headerBytes == null) return null;

        String[] lines = new String(headerBytes, StandardCharsets.ISO_8859_1).split("\\r?\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new IllegalArgumentException("Missing request line");
        }

        String[] requestLine = lines[0].trim().split("\\s+");
        if (requestLine.length != 3) {
            throw new IllegalArgumentException("Malformed request line");
        }

        String method = requestLine[0].toUpperCase(Locale.ROOT);
        if (!method.matches("[A-Z]+")) {
            throw new IllegalArgumentException("Invalid HTTP method");
        }
        if (!"HTTP/1.0".equals(requestLine[2]) && !"HTTP/1.1".equals(requestLine[2])) {
            throw new IllegalArgumentException("Unsupported HTTP version");
        }
        if (!requestLine[1].startsWith("/")) {
            throw new IllegalArgumentException("Invalid request target");
        }

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Malformed HTTP header");
            }

            String name = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
                throw new IllegalArgumentException("Malformed HTTP header name");
            }

            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (headers.containsKey(normalizedName)) {
                throw new IllegalArgumentException("Duplicate HTTP header: " + normalizedName);
            }
            headers.put(normalizedName, value);
        }

        // This server intentionally supports Content-Length only. Reject every
        // Transfer-Encoding value rather than accidentally accepting ambiguous bodies.
        if (headers.containsKey("transfer-encoding")) {
            throw new IllegalArgumentException("Transfer-Encoding is not supported");
        }

        int length = parseContentLength(headers.get("content-length"));
        byte[] bodyBytes = input.readNBytes(length);
        if (bodyBytes.length != length) {
            throw new IllegalArgumentException("Incomplete request body");
        }

        return new Request(method, requestLine[1],
                new String(bodyBytes, StandardCharsets.UTF_8), headers);
    }

    private boolean authorized(Map<String, String> headers) {
        if (configuredApiKey.isBlank()) return true;
        String supplied = headers.getOrDefault("x-api-key", "");
        return MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    private int parseContentLength(String value) {
        if (value == null) return 0;
        if (value.isBlank()) throw new IllegalArgumentException("Invalid Content-Length");

        final int length;
        try {
            length = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Content-Length");
        }

        if (length < 0) throw new IllegalArgumentException("Content-Length must not be negative");
        if (length > MAX_BODY_BYTES) throw new IllegalArgumentException("Request body is too large");
        return length;
    }

    private byte[] readUntilHeaderEnd(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;

        while ((current = input.read()) != -1) {
            buffer.write(current);
            if (buffer.size() > MAX_HEADER_BYTES) {
                throw new IllegalArgumentException("Request headers are too large");
            }

            if (previous == '\\r' && current == '\\n') {
                byte[] bytes = buffer.toByteArray();
                int size = bytes.length;
                if (size >= 4
                        && bytes[size - 4] == '\\r'
                        && bytes[size - 3] == '\\n') {
                    return java.util.Arrays.copyOf(bytes, size - 2);
                }
            }
            previous = current;
        }

        if (buffer.size() == 0) return null;
        throw new IllegalArgumentException("Incomplete request headers");
    }

    private void writeResponse(OutputStream output, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + statusText(status)
                + "\r\nContent-Type: application/json; charset=utf-8"
                + "\r\nContent-Length: " + bytes.length
                + "\r\nConnection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(bytes);
        output.flush();
    }

    private void writeError(OutputStream output, int status, String message) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("error", message == null ? "Request failed" : message);
        writeResponse(output, status, objectMapper.writeValueAsString(payload));
    }

    private String statusText(int code) {
        return switch (code) {
            case 200 -> "200 OK";
            case 400 -> "400 Bad Request";
            case 401 -> "401 Unauthorized";
            case 404 -> "404 Not Found";
            case 408 -> "408 Request Timeout";
            default -> "500 Internal Server Error";
        };
    }

    private record Request(String method, String path, String body,
                           Map<String, String> headers) { }
}
