package server;

public class Main {
    public static void main(String[] args) {
        int port = 8081;
        HttpServer server = new HttpServer(port);
        server.start();
        System.out.println("Server started on port " + port);
        System.out.println("Camera files directory: " + CameraService.CAMS_DIR);
    }
}