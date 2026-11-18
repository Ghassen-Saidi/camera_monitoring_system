package server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Camera {
    @JsonProperty("cameraId")
    private int id;
    private String status;
    private String type;
    private String streamUrl;

    public Camera() {} // for Jackson

    public Camera(int id, String status, String type, String streamUrl) {
        this.id = id;
        this.status = status;
        this.type = type;
        this.streamUrl = streamUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
}
