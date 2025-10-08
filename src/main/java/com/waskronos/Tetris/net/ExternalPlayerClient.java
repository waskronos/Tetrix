package com.waskronos.Tetris.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * This connects to the external player server.
 */
public final class ExternalPlayerClient {
    private final String host;
    private final int port;
    private final Consumer<String> onCommand;
    private final Consumer<Boolean> onConnectionChanged;

    private volatile boolean running;
    private Thread worker;

    public ExternalPlayerClient(String host, int port,
                                Consumer<String> onCommand,
                                Consumer<Boolean> onConnectionChanged) {
        this.host = host;
        this.port = port;
        this.onCommand = onCommand;
        this.onConnectionChanged = onConnectionChanged;
    }

    public void start() {
        if (running) return;
        running = true;
        worker = new Thread(this::runLoop, "external-player");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    private void runLoop() {
        while (running) {
            try (Socket socket = new Socket(host, port);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                signalConnected(true);

                String line;
                while (running && (line = in.readLine()) != null) {
                    String cmd = line.trim();
                    if (!cmd.isEmpty()) onCommand.accept(cmd);
                }
            } catch (Exception ignore) {
            } finally {
                signalConnected(false);
                sleep(600);
            }
        }
    }

    private void signalConnected(boolean v) {
        try {
            onConnectionChanged.accept(v);
        } catch (Exception ignore) {}
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignore) {}
    }
}