package edu.spbau.master.java.bemchmark.servers.server.impl;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.*;

/**
 * Simple UDP server.
 * Creates a new thread per client.
 */
@Slf4j
public class SimpleUDPServer extends AbstractUDPServer {
    private volatile DatagramSocket datagramSocket;

    public SimpleUDPServer(int datagramSize) {
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
                        new Thread(() -> handle(datagramPacket, datagramSocket)).start();
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
    }
}
