package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpServer {
    private final int port;
    private final ExecutorService threadPool;
    private final CameraController cameraController;
    private final CountDownLatch startupCompleted = new CountDownLatch(1);
    private final Object lifecycleLock = new Object();
    private final Set<Socket> activeClients = ConcurrentHashMap.newKeySet();

    private volatile boolean running;
    private volatile boolean startedSuccessfully;
    private volatile ServerSocket serverSocket;
    private volatile IOException startupFailure;
    private boolean startInvoked;
    private boolean stopRequested;

    public HttpServer(int port) {
        this(port, new CameraService());
    }

    public HttpServer(int port, CameraService cameraService) {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Invalid port: " + port);
        this.port = port;
        this.cameraController = new CameraController(Objects.requireNonNull(cameraService, "cameraService"));
        this.threadPool = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());
    }

    public void start() {
        synchronized (lifecycleLock) {
            if (startInvoked) throw new IllegalStateException("Server can only be started once");
            startInvoked = true;
            if (stopRequested) {
                startupCompleted.countDown();
                return;
            }
        }

        try (ServerSocket openedSocket = new ServerSocket()) {
            openedSocket.setReuseAddress(true);
            openedSocket.bind(new InetSocketAddress(port));

            synchronized (lifecycleLock) {
                if (stopRequested) return;
                serverSocket = openedSocket;
                running = true;
                startedSuccessfully = true;
                startupCompleted.countDown();
            }

            System.out.println("Listening on port " + openedSocket.getLocalPort());

            while (running) {
                Socket clientSocket = null;
                try {
                    clientSocket = openedSocket.accept();
                    clientSocket.setSoTimeout(10_000);

                    synchronized (lifecycleLock) {
                        if (!running) {
                            closeQuietly(clientSocket);
                            clientSocket = null;
                            continue;
                        }
                        activeClients.add(clientSocket);
                    }

                    Socket acceptedSocket = clientSocket;
                    try {
                        threadPool.execute(() -> {
                            try {
                                new RequestHandler(acceptedSocket, cameraController).run();
                            } finally {
                                activeClients.remove(acceptedSocket);
                            }
                        });
                        clientSocket = null; // Ownership transferred to the worker.
                    } catch (RejectedExecutionException e) {
                        activeClients.remove(acceptedSocket);
                        closeQuietly(acceptedSocket);
                        clientSocket = null;
                        System.err.println("Request queue full; connection closed");
                    }
                } catch (SocketException e) {
                    closeQuietly(clientSocket);
                    if (running) System.err.println("Accept error: " + e.getMessage());
                } catch (IOException e) {
                    closeQuietly(clientSocket);
                    if (running) System.err.println("Connection setup failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            startupFailure = e;
            if (!startedSuccessfully) {
                System.err.println("Server failed to bind on port " + port + ": " + e.getMessage());
            } else if (running) {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            List<Socket> remainingClients;
            synchronized (lifecycleLock) {
                running = false;
                serverSocket = null;
                remainingClients = new ArrayList<>(activeClients);
            }
            remainingClients.forEach(this::closeQuietly);

            startupCompleted.countDown();
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Returns true only after the socket has successfully bound and begun listening. */
    public boolean awaitReady(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0) throw new IllegalArgumentException("Timeout must not be negative");
        return startupCompleted.await(timeoutMillis, TimeUnit.MILLISECONDS)
                && startedSuccessfully;
    }

    public IOException getStartupFailure() {
        return startupFailure;
    }

    public int getBoundPort() {
        ServerSocket current = serverSocket;
        return current == null ? port : current.getLocalPort();
    }

    public void stop() {
        ServerSocket listeningSocket;
        List<Socket> clients;
        synchronized (lifecycleLock) {
            stopRequested = true;
            running = false;
            listeningSocket = serverSocket;
            clients = new ArrayList<>(activeClients);
        }

        closeQuietly(listeningSocket);
        clients.forEach(this::closeQuietly);
        threadPool.shutdownNow();
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) return;
        try { socket.close(); } catch (IOException ignored) { }
    }

    private void closeQuietly(ServerSocket socket) {
        if (socket == null) return;
        try { socket.close(); } catch (IOException ignored) { }
    }
}
