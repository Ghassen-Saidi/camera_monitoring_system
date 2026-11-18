package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CameraController {
    private final CameraService cameraService;
    private final ObjectMapper objectMapper;

    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
        this.objectMapper = new ObjectMapper();
    }

    public String getAllCameras() throws JsonProcessingException {
        List<Camera> cameras = cameraService.getAllCameras();
        return objectMapper.writeValueAsString(cameras);
    }

    public String getCamera(int id) throws JsonProcessingException {
        Camera camera = cameraService.getCamera(id);
        if (camera == null) throw new java.util.NoSuchElementException("Camera not found");
        return objectMapper.writeValueAsString(camera);
    }

    public String updateCamera(int id, String action) throws JsonProcessingException {
        cameraService.setStatus(id, action);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("cameraId", id);
        response.put("action", action.toUpperCase(java.util.Locale.ROOT));
        response.put("message", "Camera " + id + " set to " + action);
        return objectMapper.writeValueAsString(response);
    }

    public String updateAllCameras(String action) throws JsonProcessingException {
        return objectMapper.writeValueAsString(cameraService.setAllStatus(action));
    }
}
