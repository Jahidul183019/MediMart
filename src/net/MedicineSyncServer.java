// src/net/MedicineSyncServer.java
package net;

import utils.ConfigManager;
import utils.FileLogger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MedicineSyncServer {
    private final int port;
    private volatile boolean running;
    private ServerSocket server;
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private Thread acceptThread;

    public MedicineSyncServer() {
        this.port = Integer.parseInt(ConfigManager.get("socket.port","5050"));
    }

    public void start() {
        if (running) return;
        try {
            server = new ServerSocket(port);
            running = true;
            FileLogger.info("SyncServer started on port " + port);
            acceptThread = new Thread(this::acceptLoop, "SyncServer-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (IOException e) {
            FileLogger.error("SyncServer failed to start: " + e.getMessage(), e);
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = server.accept();
                clients.add(s);
                FileLogger.info("Client connected: " + s.getRemoteSocketAddress());
                // Handle client in separate thread (reads nothing; just keep alive)
                new Thread(() -> clientLoop(s), "SyncServer-Client").start();
            } catch (IOException e) {
                if (running) FileLogger.warn("Accept failed: " + e.getMessage());
            }
        }
    }

    private void clientLoop(Socket s) {
        try (s) {
            // Keep connection until closed by client
            new BufferedReader(new InputStreamReader(s.getInputStream())).readLine();
        } catch (IOException ignored) {
        } finally {
            clients.remove(s);
        }
    }

    public void broadcastRefresh() {
        List<Socket> toRemove = new ArrayList<>();
        for (Socket s : clients) {
            try {
                var out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                out.write("REFRESH\n");
                out.flush();
            } catch (IOException e) {
                toRemove.add(s);
            }
        }
        clients.removeAll(toRemove);
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        FileLogger.info("SyncServer stopped.");
    }
}
