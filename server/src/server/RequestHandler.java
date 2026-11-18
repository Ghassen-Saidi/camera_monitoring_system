package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

    public RequestHandler(Socket socket, CameraController cameraController) {
        this.socket = socket;
        this.cameraController = cameraController;
    }

    @Override
    public void run() {
        try (Socket ignored = socket;
             InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            Request request = readRequest(input);
            if (request == null) return;
            try {
                writeResponse(output, 200, dispatch(request.method(), request.path(), request.body()));
            } catch (NoSuchElementException e) {
                writeError(output, 404, e.getMessage());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                writeError(output, 400, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                writeError(output, 500, "Internal Server Error");
            }
        } catch (IOException e) {
            System.err.println("Handler error: " + e.getMessage());
        }
    }

    private String dispatch(String method, String rawPath, String body) throws Exception {
        String path = rawPath.split("\\?", 2)[0];
        if ("GET".equals(method) && "/api/cameras".equals(path)) return cameraController.getAllCameras();
        Matcher getOne = Pattern.compile("/api/cameras/(\\d+)").matcher(path);
        if ("GET".equals(method) && getOne.matches()) return cameraController.getCamera(Integer.parseInt(getOne.group(1)));
        Matcher postOne = Pattern.compile("/api/cameras/(\\d+)/record").matcher(path);
        if ("POST".equals(method) && postOne.matches()) {
            return cameraController.updateCamera(Integer.parseInt(postOne.group(1)), readAction(body));
        }
        if ("POST".equals(method) && "/api/cameras/all/record".equals(path)) {
            return cameraController.updateAllCameras(readAction(body));
        }
        throw new NoSuchElementException("Endpoint not found");
    }

    @SuppressWarnings("unchecked")
    private String readAction(String body) throws JsonProcessingException {
        Map<String, String> payload = objectMapper.readValue(body, Map.class);
        String action = payload.get("action");
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Missing 'action' field");
        return action;
    }

    private Request readRequest(InputStream input) throws IOException {
        byte[] headerBytes = readUntilHeaderEnd(input);
        if (headerBytes == null) return null;
        String[] lines = new String(headerBytes, StandardCharsets.ISO_8859_1).split("\\r?\\n");
        String[] requestLine = lines[0].split("\\s+");
        if (requestLine.length < 3) throw new IllegalArgumentException("Malformed request line");
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int separator = lines[i].indexOf(':');
            if (separator > 0) headers.put(lines[i].substring(0, separator).trim().toLowerCase(), lines[i].substring(separator + 1).trim());
        }
        if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) {
            throw new IllegalArgumentException("Chunked requests are not supported");
        }
        int length = parseContentLength(headers.get("content-length"));
        byte[] bodyBytes = input.readNBytes(length);
        if (bodyBytes.length != length) throw new IllegalArgumentException("Incomplete request body");
        return new Request(requestLine[0].toUpperCase(), requestLine[1], new String(bodyBytes, StandardCharsets.UTF_8));
    }

    private int parseContentLength(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            int length = Integer.parseInt(value);
            if (length < 0 || length > MAX_BODY_BYTES) throw new IllegalArgumentException("Request body is too large");
            return length;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid Content-Length"); }
    }

    private byte[] readUntilHeaderEnd(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            buffer.write(current);
            if (buffer.size() > MAX_HEADER_BYTES) throw new IllegalArgumentException("Request headers are too large");
            if (previous == '\r' && current == '\n') {
                byte[] bytes = buffer.toByteArray();
                int size = bytes.length;
                if (size >= 4 && bytes[size - 4] == '\r' && bytes[size - 3] == '\n') {
                    return java.util.Arrays.copyOf(bytes, size - 2);
                }
            }
            previous = current;
        }
        return buffer.size() == 0 ? null : buffer.toByteArray();
    }

    private void writeResponse(OutputStream output, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + statusText(status) + "\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(bytes);
        output.flush();
    }

    private void writeError(OutputStream output, int status, String message) throws IOException {
        writeResponse(output, status, objectMapper.writeValueAsString(Map.of("error", message == null ? "Request failed" : message)));
    }

    private String statusText(int code) {
        return switch (code) { case 200 -> "200 OK"; case 400 -> "400 Bad Request"; case 404 -> "404 Not Found"; default -> "500 Internal Server Error"; };
    }

    private record Request(String method, String path, String body) { }
}
