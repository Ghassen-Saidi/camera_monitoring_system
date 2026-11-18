package server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Camera {
    @JsonProperty("camerald")
    private int id;
    private String status;

    public Camera() {} // for Jackson

    public Camera(int id, String status) {
        this.id = id;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}