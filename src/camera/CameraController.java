package camera;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.util.List;

public class CameraController {

    @FXML private TableView<Camera> tableView;
    @FXML private TableColumn<Camera, Integer> idColumn;
    @FXML private TableColumn<Camera, String> statusColumn;
    @FXML private TableColumn<Camera, String> typeColumn;
    @FXML private TableColumn<Camera, String> streamUrlColumn;
    @FXML private Label statusLabel;

    private final ObservableList<Camera> cameraList = FXCollections.observableArrayList();
    private final CameraService service = new CameraService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        streamUrlColumn.setCellValueFactory(new PropertyValueFactory<>("streamUrl"));
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

    @FXML
    private void handleOpenStream() {
        Camera selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a camera first.");
            return;
        }
        String url = selected.getStreamUrl();
        if (url == null || url.isBlank()) {
            showAlert("No Stream URL", "Configure CAMERA_" + selected.getId() + "_URL first.");
            return;
        }
        if (selected.getType().equalsIgnoreCase("RTSP") || selected.getType().equalsIgnoreCase("WEBCAM")) {
            showAlert("Stream Bridge Required", selected.getType() + " sources should be converted to HLS, HTTP, or another JavaFX-compatible stream first.\n\nSource: " + url);
            return;
        }
        try {
            MediaPlayer player = new MediaPlayer(new Media(url));
            MediaView view = new MediaView(player);
            view.setPreserveRatio(true);
            view.setFitWidth(900);
            view.setFitHeight(600);
            StackPane root = new StackPane(view);
            Stage stage = new Stage();
            stage.setTitle("Camera " + selected.getId() + " - " + selected.getType());
            stage.setScene(new javafx.scene.Scene(root, 900, 600));
            stage.setOnShown(e -> player.play());
            stage.setOnHidden(e -> player.dispose());
            stage.show();
        } catch (RuntimeException ex) {
            showAlert("Stream Error", "Could not open stream: " + ex.getMessage());
        }
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
