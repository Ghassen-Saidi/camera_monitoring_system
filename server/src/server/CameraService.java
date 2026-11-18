package server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        if (!isValidId(id)) return null;
        return new Camera(id, control.status(id), cameraType(id), streamUrl(id));
    }

    public void setStatus(int id, String action) {
        validateId(id);
        validateAction(action);
        if ("START".equalsIgnoreCase(action)) control.start(id);
        else control.stop(id);
    }

    /**
     * Attempts every camera and returns per-camera results. A failure on one camera
     * does not prevent commands from being sent to the remaining cameras.
     */
    public Map<String, Object> setAllStatus(String action) {
        validateAction(action);
        String normalizedAction = action.toUpperCase(Locale.ROOT);
        String expectedStatus = "START".equals(normalizedAction) ? "RECORDING" : "STOPPED";

        List<Map<String, Object>> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (int id = 0; id < count; id++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cameraId", id);
            try {
                setStatus(id, normalizedAction);
                item.put("success", true);
                item.put("status", expectedStatus);
                succeeded++;
            } catch (RuntimeException e) {
                item.put("success", false);
                item.put("error", safeMessage(e));
                failed++;
            }
            results.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", normalizedAction);
        response.put("success", failed == 0);
        response.put("partial", succeeded > 0 && failed > 0);
        response.put("attempted", count);
        response.put("succeeded", succeeded);
        response.put("failed", failed);
        response.put("message", failed == 0
                ? "All " + count + " cameras set to " + normalizedAction
                : succeeded + " of " + count + " cameras set to " + normalizedAction
                    + "; " + failed + " failed");
        response.put("results", results);
        return response;
    }

    private boolean isValidId(int id) {
        return id >= 0 && id < count;
    }

    private void validateId(int id) {
        if (!isValidId(id)) throw new IllegalArgumentException("Invalid camera ID: " + id);
    }

    private void validateAction(String action) {
        if (!"START".equalsIgnoreCase(action) && !"STOP".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    private String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    private static int configuredCount() {
        try {
            return Math.max(1, Integer.parseInt(System.getenv().getOrDefault("CAMERA_COUNT", "3")));
        } catch (NumberFormatException ignored) {
            return 3;
        }
    }

    private String cameraType(int id) {
        return System.getenv().getOrDefault("CAMERA_" + id + "_TYPE", "HTTP").toUpperCase(Locale.ROOT);
    }

    private String streamUrl(int id) {
        return System.getenv().getOrDefault("CAMERA_" + id + "_URL", "");
    }
}
