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
            for (int i = 0; i < 3; i++) {
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
        for (int i = 0; i < 3; i++) {
            list.add(getCamera(i));
        }
        return list;
    }

    public Camera getCamera(int id) {
        if (id < 0 || id > 2) return null;
        try {
            Path file = Paths.get(CAMS_DIR, "cam" + id);
            String content = Files.readString(file).trim();
            return new Camera(id, content);
        } catch (IOException e) {
            return new Camera(id, "UNKNOWN");
        }
    }

    public void setStatus(int id, String action) {
        if (id < 0 || id > 2) throw new IllegalArgumentException("Invalid camera ID");
        String status = action.equalsIgnoreCase("START") ? "RECORDING" : "STOPPED";
        try {
            Path file = Paths.get(CAMS_DIR, "cam" + id);
            Files.writeString(file, status, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write camera file", e);
        }
    }

    public void setAllStatus(String action) {
        for (int i = 0; i < 3; i++) {
            setStatus(i, action);
        }
    }
}