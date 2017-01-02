package edu.spbau.master.java.bemchmark.servers.app;

import edu.spbau.master.java.bemchmark.servers.server.Server;
import edu.spbau.master.java.bemchmark.servers.server.ServerWithStatistic;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server for launch different server architectures.
 */
@Slf4j
public final class MasterServer implements Server {
    private volatile ServerSocket serverSocket = null;

    @Override
    public void start(int portNum) {
        try {
            serverSocket = new ServerSocket(portNum);
        } catch (IOException e) {
            log.error("Exception during server creating", e);
        }
        new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try (final Socket clientSocket = serverSocket.accept();
                     final InputStream inputStream = clientSocket.getInputStream();
                     final OutputStream outputStream = clientSocket.getOutputStream();
                     final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                     final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {

                    ServerBenchmarkConfig config = (ServerBenchmarkConfig) objectInputStream.readObject();
                    ServerWithStatistic instance = ServerFactory.getInstance(config);
                    instance.start(config.getServerPort());

                    ServerGetStatQuery query = (ServerGetStatQuery) objectInputStream.readObject();
                    instance.stop();
                    objectOutputStream.writeObject(new ServerBenchmarkResult(
                            instance.getAverageClientProcessingTime(),
                            instance.getAverageRequestProcessingTime()
                    ));
                } catch (SocketException e) {
                    log.info("Server socket closed");
                } catch (IOException e) {
                    log.error("Exception during accept", e);
                } catch (ClassNotFoundException e) {
                    log.error("Invalid server config.", e);
                }
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
