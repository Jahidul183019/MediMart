// src/net/MedicineSyncClient.java
package net;

import utils.ConfigManager;
import utils.FileLogger;

import java.io.*;
import java.net.Socket;

public class MedicineSyncClient {
    private Thread loop;
    private volatile boolean running;

    public void start(Runnable onRefresh) {
        if (running) return;
        running = true;
        loop = new Thread(() -> runLoop(onRefresh), "SyncClient-Loop");
        loop.setDaemon(true);
        loop.start();
    }

    private void runLoop(Runnable onRefresh) {
        String host = ConfigManager.get("socket.host","127.0.0.1");
        int port = Integer.parseInt(ConfigManager.get("socket.port","5050"));
        while (running) {
            try (Socket s = new Socket(host, port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                FileLogger.info("Connected to SyncServer " + host + ":" + port);
                String line;
                while (running && (line = in.readLine()) != null) {
                    if ("REFRESH".equalsIgnoreCase(line.trim())) {
                        onRefresh.run();
                    }
                }
            } catch (IOException e) {
                FileLogger.warn("SyncClient reconnecting in 2s: " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
    }

    public void stop() { running = false; }
}
