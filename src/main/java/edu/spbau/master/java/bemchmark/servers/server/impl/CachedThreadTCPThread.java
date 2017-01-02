package edu.spbau.master.java.bemchmark.servers.server.impl;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server with cached pool of threads.
 */
@Slf4j
public final class CachedThreadTCPThread extends AbstractTCPServer {
    @NotNull
    private final ExecutorService executorService = Executors.newCachedThreadPool();

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
        executorService.execute(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    final Socket clientSocket = serverSocket.accept();
                    executorService.execute(() -> handleConnection(clientSocket));
                }
            } catch (SocketException e) {
                log.info("Server socket closed");
            } catch (IOException e) {
                log.error("Exception during accept", e);
            }
        });
    }

    @Override
    public void stop() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                executorService.shutdown();
            } catch (IOException e) {
                log.error("Exception during closing server socket.", e);
            }
        }
    }
}
