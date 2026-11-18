package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
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
        Camera cam = cameraService.getCamera(id);
        if (cam == null) {
            throw new java.util.NoSuchElementException("Camera not found");
        }
        return objectMapper.writeValueAsString(cam);
    }

    public String updateCamera(int id, String action) throws JsonProcessingException {
        cameraService.setStatus(id, action);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Camera " + id + " set to " + action);
        return objectMapper.writeValueAsString(response);
    }

    public String updateAllCameras(String action) throws JsonProcessingException {
        cameraService.setAllStatus(action);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All cameras set to " + action);
        return objectMapper.writeValueAsString(response);
    }
}
