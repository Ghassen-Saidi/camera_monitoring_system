package server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int port = 8081;
        HttpServer server = new HttpServer(port);
        Thread serverThread = new Thread(server::start, "camera-http-server");

        Runtime.getRuntime().addShutdownHook(
                new Thread(server::stop, "camera-server-shutdown"));
        serverThread.start();

        try {
            if (!server.awaitReady(5_000)) {
                server.stop();
                IOException startupFailure = server.getStartupFailure();
                String message = startupFailure == null
                        ? "Server did not become ready within 5 seconds"
                        : "Server failed to bind: " + startupFailure.getMessage();
                throw new IllegalStateException(message, startupFailure);
            }
        } catch (InterruptedException e) {
            server.stop();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting server", e);
        }

        System.out.println("Server started on port " + server.getBoundPort());
        System.out.println("Camera files directory: " + CameraService.CAMS_DIR);
    }
}
