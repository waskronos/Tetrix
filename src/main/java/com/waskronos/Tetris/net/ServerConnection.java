package com.waskronos.Tetris.net;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class ServerConnection {
    private ServerConnection() {}
    public static boolean ping() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", 3000), 200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}