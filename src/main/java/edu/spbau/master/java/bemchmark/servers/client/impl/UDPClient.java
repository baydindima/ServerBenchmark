package edu.spbau.master.java.bemchmark.servers.client.impl;

import edu.spbau.master.java.bemchmark.servers.client.AbstractClient;
import edu.spbau.master.java.bemchmark.servers.client.ClientStatisticBroker;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * UDP client.
 */
@Slf4j
public final class UDPClient extends AbstractClient {

    private volatile long startTime;
    private volatile DatagramSocket datagramSocket;

    @Builder
    public UDPClient(int elementPerRequest,
                     int requestCount,
                     int delayBetweenRequest,
                     int serverPort,
                     @NotNull InetAddress serverAddress,
                     @NotNull ScheduledExecutorService executorService,
                     @NotNull final ClientStatisticBroker clientStatisticBroker) {
        super(elementPerRequest, requestCount, delayBetweenRequest, serverPort, serverAddress, executorService, clientStatisticBroker);
    }

    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        try {
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(10000);
            sendRequest(0);
        } catch (SocketException e) {
            log.error("Exception during socket start.", e);
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    private void sendRequest(final int curRequestNum) {
        executorService.schedule(() -> {
                    try {
                        int[] data = new int[elementPerRequest];
                        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
                        for (int i = 0; i < data.length; i++) {
                            data[i] = localRandom.nextInt();
                        }
                        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * Integer.BYTES);
                        byteBuffer.asIntBuffer().put(data);
                        byte[] rawData = byteBuffer.array();
                        DatagramPacket datagramPacket = new DatagramPacket(rawData, rawData.length);
                        datagramPacket.setAddress(serverAddress);
                        datagramPacket.setPort(serverPort);
                        datagramSocket.send(datagramPacket);

                        DatagramPacket resultPacket = new DatagramPacket(rawData, rawData.length);
                        try {
                            datagramSocket.receive(resultPacket);
                        } catch (SocketTimeoutException ex) {
                            log.warn("UDP timeout.");
                        }


                        log.info("Request completed");
                        if (curRequestNum + 1 < requestCount) {
                            sendRequest(curRequestNum + 1);
                        } else {
                            datagramSocket.close();
                            clientStatisticBroker.addAverageWorkTimeStatistic(System.currentTimeMillis() - startTime);
                        }
                    } catch (IOException e) {
                        log.error("Exception while sending message", e);
                        datagramSocket.close();
                    }
                }, nextDelay(),
                TimeUnit.MILLISECONDS);
    }
}
