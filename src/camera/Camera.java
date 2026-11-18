package camera;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Camera {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty status;
    private final SimpleStringProperty type;
    private final SimpleStringProperty streamUrl;

    public Camera() {
        this(0, "UNKNOWN", "HTTP", "");
    }

    public Camera(int id, String status, String type, String streamUrl) {
        this.id = new SimpleIntegerProperty(id);
        this.status = new SimpleStringProperty(status);
        this.type = new SimpleStringProperty(type);
        this.streamUrl = new SimpleStringProperty(streamUrl == null ? "" : streamUrl);
    }

    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }
    public String getStatus() { return status.get(); }
    public SimpleStringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }
    public String getType() { return type.get(); }
    public SimpleStringProperty typeProperty() { return type; }
    public void setType(String type) { this.type.set(type); }
    public String getStreamUrl() { return streamUrl.get(); }
    public SimpleStringProperty streamUrlProperty() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl.set(streamUrl == null ? "" : streamUrl); }
}
