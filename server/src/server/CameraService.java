package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CameraService {
    // Adjust this path for your system. On Windows, use e.g., "C:\\cams" or "./cams"
    public static final String CAMS_DIR = "./cams";

    public CameraService() {
        // Ensure directory and files exist with default STOPPED state
        try {
            Files.createDirectories(Paths.get(CAMS_DIR));
            for (int i = 0; i < cameraCount(); i++) {
                Path file = Paths.get(CAMS_DIR, "cam" + i);
                if (!Files.exists(file)) {
                    Files.writeString(file, "STOPPED");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize camera files", e);
        }
    }

    public List<Camera> getAllCameras() {
        List<Camera> list = new ArrayList<>();
        for (int i = 0; i < cameraCount(); i++) {
            list.add(getCamera(i));
        }
        return list;
    }

    public Camera getCamera(int id) {
        if (id < 0 || id >= cameraCount()) return null;
        try {
            Path file = Paths.get(CAMS_DIR, "cam" + id);
            String content = Files.readString(file).trim();
            return new Camera(id, content, cameraType(id), streamUrl(id));
        } catch (IOException e) {
            return new Camera(id, "UNKNOWN", cameraType(id), streamUrl(id));
        }
    }

    public synchronized void setStatus(int id, String action) {
        if (id < 0 || id >= cameraCount()) throw new IllegalArgumentException("Invalid camera ID");
        if (!"START".equalsIgnoreCase(action) && !"STOP".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
        String status = "START".equalsIgnoreCase(action) ? "RECORDING" : "STOPPED";
        try {
            Path file = Paths.get(CAMS_DIR, "cam" + id);
            Files.writeString(file, status, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write camera file", e);
        }
    }

    public void setAllStatus(String action) {
        for (int i = 0; i < cameraCount(); i++) {
            setStatus(i, action);
        }
    }

    private int cameraCount() {
        try { return Math.max(1, Integer.parseInt(System.getenv().getOrDefault("CAMERA_COUNT", "3"))); }
        catch (NumberFormatException ignored) { return 3; }
    }

    private String cameraType(int id) {
        return System.getenv().getOrDefault("CAMERA_" + id + "_TYPE", "HTTP").toUpperCase();
    }

    private String streamUrl(int id) {
        return System.getenv().getOrDefault("CAMERA_" + id + "_URL", "");
    }
}
