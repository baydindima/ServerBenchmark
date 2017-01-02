package edu.spbau.master.java.bemchmark.servers.server.impl;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


/**
 * Simple TCP server with blocking IO.
 * Creates a new thread per client.
 */
@Slf4j
public final class SimpleTCPServer extends AbstractTCPServer {

    private volatile ServerSocket serverSocket = null;

    @Override
    public void start(int portNum) {
        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(portNum, Integer.MAX_VALUE);
            } catch (IOException e) {
                log.error("Exception during server creating", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    final Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleConnection(clientSocket)).start();
                }
            } catch (SocketException e) {
                log.info("Server socket closed");
            } catch (IOException e) {
                log.error("Exception during accept", e);
            }
        }).start();
    }

    @Override
    public void stop() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Exception during closing server socket.", e);
            }
        }
    }
}
