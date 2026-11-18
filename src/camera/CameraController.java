package camera;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class CameraController {

    @FXML private TableView<Camera> tableView;
    @FXML private TableColumn<Camera, Integer> idColumn;
    @FXML private TableColumn<Camera, String> statusColumn;
    @FXML private Label statusLabel;

    private final ObservableList<Camera> cameraList = FXCollections.observableArrayList();
    private final CameraService service = new CameraService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableView.setItems(cameraList);
        refreshCameraList();
    }

    @FXML
    private void handleRefresh() {
        refreshCameraList();
    }

    @FXML
    private void handleStartSelected() {
        sendActionToSelected("START");
    }

    @FXML
    private void handleStopSelected() {
        sendActionToSelected("STOP");
    }

    @FXML
    private void handleStartAll() {
        sendActionToAll("START");
    }

    @FXML
    private void handleStopAll() {
        sendActionToAll("STOP");
    }

    private void refreshCameraList() {
        Task<List<Camera>> task = new Task<>() {
            @Override
            protected List<Camera> call() throws Exception {
                updateMessage("Fetching cameras...");
                return service.getAllCameras();
            }
        };

        // Listen to message changes and update the label on the JavaFX thread
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> statusLabel.setText(newMsg));
        });

        task.setOnSucceeded(e -> {
            cameraList.setAll(task.getValue());
            statusLabel.setText("Camera list updated");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            statusLabel.setText("Error: " + ex.getMessage());
            showAlert("Error", "Failed to fetch cameras: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void sendActionToSelected(String action) {
        Camera selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a camera first.");
            return;
        }
        int id = selected.getId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Sending " + action + " to camera " + id + "...");
                service.sendCameraAction(id, action);
                return null;
            }
        };

        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> statusLabel.setText(newMsg));
        });

        task.setOnSucceeded(e -> {
            statusLabel.setText("Command sent successfully");
            refreshCameraList();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            statusLabel.setText("Error: " + ex.getMessage());
            showAlert("Error", "Failed to send command: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void sendActionToAll(String action) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Sending " + action + " to all cameras...");
                service.sendAllCamerasAction(action);
                return null;
            }
        };

        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> statusLabel.setText(newMsg));
        });

        task.setOnSucceeded(e -> {
            statusLabel.setText("Command sent to all cameras");
            refreshCameraList();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            statusLabel.setText("Error: " + ex.getMessage());
            showAlert("Error", "Failed to send global command: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}