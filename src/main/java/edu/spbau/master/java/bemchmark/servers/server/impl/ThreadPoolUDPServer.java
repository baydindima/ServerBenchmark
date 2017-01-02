package edu.spbau.master.java.bemchmark.servers.server.impl;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server with fixed thread pool and UDP sockets.
 */
@Slf4j
public final class ThreadPoolUDPServer extends AbstractUDPServer {
    @NotNull
    private final ExecutorService threadPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    private volatile DatagramSocket datagramSocket;

    public ThreadPoolUDPServer(int datagramSize) {
        super(datagramSize);
    }

    @Override
    public void start(int portNum) {
        new Thread(() -> {
            try {
                datagramSocket = new DatagramSocket(portNum);
                datagramSocket.setReceiveBufferSize(datagramSize * 500);
                datagramSocket.setSendBufferSize(datagramSize * 500);
                while (!datagramSocket.isClosed()) {
                    byte[] bytes = new byte[datagramSize];
                    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);

                    try {
                        datagramSocket.receive(datagramPacket);
                        threadPool.execute(() -> handle(datagramPacket, datagramSocket));
                    } catch (SocketException e) {
                        log.warn("Socket closed.");
                    }
                }
            } catch (IOException e) {
                log.error("Exception while listening", e);
                stop();
            }

        }).start();
    }


    @Override
    public void stop() {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
        threadPool.shutdown();
    }
}
