package server;

public interface CameraControl {
    String status(int id);
    void start(int id);
    void stop(int id);
}
