package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

public class HttpServer {
    private final int port;
    private final ExecutorService threadPool;
    private final CameraController cameraController;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    private final CountDownLatch ready = new CountDownLatch(1);

    public HttpServer(int port) {
        this(port, new CameraService());
    }

    public HttpServer(int port, CameraService cameraService) {
        this.port = port;
        this.threadPool = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());
        this.cameraController = new CameraController(cameraService);
    }

    public void start() {
        running = true;
        try (ServerSocket openedSocket = new ServerSocket(port)) {
            serverSocket = openedSocket;
            System.out.println("Listening on port " + port);
            ready.countDown();
            while (running) {
                try {
                    Socket clientSocket = openedSocket.accept();
                    clientSocket.setSoTimeout(10_000);
                    try {
                        threadPool.execute(new RequestHandler(clientSocket, cameraController));
                    } catch (RejectedExecutionException e) {
                        try { clientSocket.close(); } catch (IOException ignored) { }
                        System.err.println("Request queue full; connection closed");
                    }
                } catch (SocketException e) {
                    if (running) System.err.println("Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("Server error: " + e.getMessage());
        } finally {
            ready.countDown();
            serverSocket = null;
            threadPool.shutdown();
        }
    }

    public boolean awaitReady(long timeoutMillis) throws InterruptedException {
        return ready.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        ServerSocket socket = serverSocket;
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) { }
        }
        threadPool.shutdownNow();
    }
}
