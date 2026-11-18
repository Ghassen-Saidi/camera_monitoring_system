package camera;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Camera {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty status;

    public Camera(int id, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.status = new SimpleStringProperty(status);
    }

    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }
    public String getStatus() { return status.get(); }
    public SimpleStringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }
}