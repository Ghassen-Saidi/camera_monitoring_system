package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/** Development adapter. Replace this with an FFmpeg/RTSP or webcam adapter for real hardware. */
public class FileCameraControl implements CameraControl {
    private final Path directory;
    private final int count;

    public FileCameraControl(String directory, int count) {
        this.directory = Paths.get(directory);
        this.count = count;
        try {
            Files.createDirectories(this.directory);
            for (int i = 0; i < count; i++) {
                Files.writeString(file(i), "STOPPED", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize camera state", e);
        }
    }

    @Override
    public synchronized String status(int id) {
        validate(id);
        try { return Files.readString(file(id)).trim(); }
        catch (IOException e) { return "UNKNOWN"; }
    }

    @Override public void start(int id) { write(id, "RECORDING"); }
    @Override public void stop(int id) { write(id, "STOPPED"); }

    private synchronized void write(int id, String value) {
        validate(id);
        try { Files.writeString(file(id), value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); }
        catch (IOException e) { throw new RuntimeException("Failed to update camera state", e); }
    }

    private Path file(int id) { return directory.resolve("cam" + id); }
    private void validate(int id) { if (id < 0 || id >= count) throw new IllegalArgumentException("Invalid camera ID"); }
}
