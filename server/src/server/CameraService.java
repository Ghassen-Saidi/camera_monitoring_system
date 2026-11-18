package server;

import java.util.ArrayList;
import java.util.List;

/** Application service; camera hardware is supplied through the CameraControl adapter. */
public class CameraService {
    public static final String CAMS_DIR = "./cams";
    private final int count;
    private final CameraControl control;

    public CameraService() {
        this(configuredCount(), null);
    }

    public CameraService(int count, CameraControl control) {
        if (count < 1) throw new IllegalArgumentException("Camera count must be positive");
        this.count = count;
        this.control = control == null ? new FileCameraControl(CAMS_DIR, count) : control;
    }

    public List<Camera> getAllCameras() {
        List<Camera> cameras = new ArrayList<>();
        for (int i = 0; i < count; i++) cameras.add(getCamera(i));
        return cameras;
    }

    public Camera getCamera(int id) {
        if (id < 0 || id >= count) return null;
        return new Camera(id, control.status(id), cameraType(id), streamUrl(id));
    }

    public void setStatus(int id, String action) {
        if (!"START".equalsIgnoreCase(action) && !"STOP".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
        if ("START".equalsIgnoreCase(action)) control.start(id); else control.stop(id);
    }

    public void setAllStatus(String action) {
        for (int i = 0; i < count; i++) setStatus(i, action);
    }

    private int configuredCount() {
        try { return Math.max(1, Integer.parseInt(System.getenv().getOrDefault("CAMERA_COUNT", "3"))); }
        catch (NumberFormatException ignored) { return 3; }
    }

    private String cameraType(int id) { return System.getenv().getOrDefault("CAMERA_" + id + "_TYPE", "HTTP").toUpperCase(); }
    private String streamUrl(int id) { return System.getenv().getOrDefault("CAMERA_" + id + "_URL", ""); }
}
