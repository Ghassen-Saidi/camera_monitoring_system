package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private HttpServer server;
    private Thread serverThread;

    @AfterEach
    void stopServer() throws InterruptedException {
        if (server != null) server.stop();
        if (serverThread != null) {
            serverThread.join(3_000);
            assertFalse(serverThread.isAlive(), "Server thread should terminate after stop()");
        }
    }

    @Test
    void startsAndServesCameraListAndSingleCameraCommand() throws Exception {
        FakeCameraControl control = new FakeCameraControl();
        startServer(new CameraService(3, control));

        HttpResponse<String> list = get("/api/cameras");
        assertEquals(200, list.statusCode());
        JsonNode cameras = JSON.readTree(list.body());
        assertEquals(3, cameras.size());
        assertEquals(0, cameras.get(0).get("cameraId").asInt());

        HttpResponse<String> update = post("/api/cameras/0/record", "{\"action\":\"START\"}");
        assertEquals(200, update.statusCode());
        assertEquals("RECORDING", control.status(0));
    }

    @Test
    void bulkCommandReportsPartialFailureAndContinuesOtherCameras() throws Exception {
        FakeCameraControl control = new FakeCameraControl();
        control.failStartForCamera = 1;
        startServer(new CameraService(3, control));

        HttpResponse<String> response =
                post("/api/cameras/all/record", "{\"action\":\"START\"}");

        assertEquals(200, response.statusCode());
        JsonNode payload = JSON.readTree(response.body());
        assertFalse(payload.get("success").asBoolean());
        assertTrue(payload.get("partial").asBoolean());
        assertEquals(2, payload.get("succeeded").asInt());
        assertEquals(1, payload.get("failed").asInt());
        assertEquals("RECORDING", control.status(0));
        assertEquals("STOPPED", control.status(1));
        assertEquals("RECORDING", control.status(2));
    }

    @Test
    void rejectsDuplicateContentLengthHeaders() throws Exception {
        startServer(new CameraService(1, new FakeCameraControl()));
        String rawRequest = "POST /api/cameras/0/record HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: 2\r\n"
                + "Content-Length: 2\r\n"
                + "\r\n{}";
        assertRawRequestIsBadRequest(rawRequest);
    }

    @Test
    void rejectsCombinedTransferEncodingHeaders() throws Exception {
        startServer(new CameraService(1, new FakeCameraControl()));
        String rawRequest = "POST /api/cameras/0/record HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Transfer-Encoding: gzip, chunked\r\n"
                + "\r\n0\r\n\r\n";
        assertRawRequestIsBadRequest(rawRequest);
    }

    @Test
    void bindFailureIsReportedAsNotReady() throws Exception {
        try (ServerSocket occupied = new ServerSocket(0)) {
            server = new HttpServer(occupied.getLocalPort(), new CameraService(1, new FakeCameraControl()));
            serverThread = new Thread(server::start, "test-server-bind-failure");
            serverThread.start();

            assertFalse(server.awaitReady(3_000));
            assertNotNull(server.getStartupFailure());
            serverThread.join(3_000);
            assertFalse(serverThread.isAlive());
        }
    }

    @Test
    void stopClosesAnAcceptedIdleClientSocket() throws Exception {
        startServer(new CameraService(1, new FakeCameraControl()));

        try (Socket socket = new Socket("127.0.0.1", server.getBoundPort())) {
            socket.setSoTimeout(2_000);
            socket.getOutputStream().write(
                    "GET /api/cameras HTTP/1.1\r\nHost: localhost\r\n"
                            .getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();

            Thread.sleep(100); // Give the accept loop time to hand the connection to a worker.
            server.stop();

            try {
                assertEquals(-1, socket.getInputStream().read());
            } catch (SocketException expectedWhenPeerCloses) {
                // A close/reset is also an acceptable prompt shutdown result.
            }
        }
    }

    @Test
    void rejectsInvalidActionWithoutChangingCamera() throws Exception {
        FakeCameraControl control = new FakeCameraControl();
        startServer(new CameraService(1, control));

        HttpResponse<String> response =
                post("/api/cameras/0/record", "{\"action\":\"PAUSE\"}");

        assertEquals(400, response.statusCode());
        assertEquals("STOPPED", control.status(0));
    }

    private void assertRawRequestIsBadRequest(String rawRequest) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.getBoundPort())) {
            socket.setSoTimeout(2_000);
            OutputStream output = socket.getOutputStream();
            output.write(rawRequest.getBytes(StandardCharsets.US_ASCII));
            output.flush();

            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(response.startsWith("HTTP/1.1 400 Bad Request"), response);
        }
    }

    private void startServer(CameraService service) throws Exception {
        server = new HttpServer(0, service);
        serverThread = new Thread(server::start, "test-camera-server");
        serverThread.start();
        assertTrue(server.awaitReady(3_000), "Server should bind and report ready");
        assertTrue(server.getBoundPort() > 0);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri(path))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri(path))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI baseUri(String path) {
        return URI.create("http://127.0.0.1:" + server.getBoundPort() + path);
    }

    private static final class FakeCameraControl implements CameraControl {
        private final Map<Integer, String> statuses = new java.util.concurrent.ConcurrentHashMap<>();
        private volatile int failStartForCamera = -1;

        @Override
        public String status(int id) {
            return statuses.getOrDefault(id, "STOPPED");
        }

        @Override
        public void start(int id) {
            if (id == failStartForCamera) throw new IllegalStateException("Simulated adapter failure");
            statuses.put(id, "RECORDING");
        }

        @Override
        public void stop(int id) {
            statuses.put(id, "STOPPED");
        }
    }
}
