package server;

public class Main {
    public static void main(String[] args) {
        int port = 8081;
        HttpServer server = new HttpServer(port);
        Thread serverThread = new Thread(server::start, "camera-http-server");
        serverThread.start();
        try {
            if (!server.awaitReady(5000)) throw new IllegalStateException("Server did not bind within 5 seconds");
            System.out.println("Server started on port " + port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting server", e);
        }
        System.out.println("Camera files directory: " + CameraService.CAMS_DIR);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
