package edu.spbau.master.java.bemchmark.servers.client.impl;

import edu.spbau.master.java.bemchmark.servers.client.AbstractClient;
import edu.spbau.master.java.bemchmark.servers.client.ClientStatisticBroker;
import edu.spbau.master.java.bemchmark.servers.model.MessageFactory;
import edu.spbau.master.java.bemchmark.servers.model.MessageSerializer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TCP client with closing connection after request.
 */
@Slf4j
public final class TCPShortConnectionClient extends AbstractClient {

    private volatile long startTime;


    @Builder
    public TCPShortConnectionClient(int elementPerRequest,
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
        sendRequest(0);
    }

    private void sendRequest(final int curRequestNum) {
        executorService.schedule(() -> {
                    try (Socket socket = new Socket(serverAddress, serverPort);
                         InputStream inputStream = socket.getInputStream();
                         OutputStream outputStream = socket.getOutputStream()) {
                        @NotNull byte[] messageBytes = MessageSerializer.serialize(
                                MessageFactory.newMessage(elementPerRequest));
                        int readCount = 0;
                        try {
                            outputStream.write(messageBytes);
                            readCount = inputStream.read(messageBytes);
                            while (readCount < messageBytes.length) {
                                readCount += inputStream.read(messageBytes, readCount, messageBytes.length - readCount);
                            }
                        } catch (IOException ex) {
                            log.error("Exception during message.", ex);
                        }

                        if (readCount != messageBytes.length) {
                            log.warn(
                                    String.format("Received message length (%d) hasn't equal to sent message length (%d)",
                                            readCount, messageBytes.length));
                        }
                        log.info("Request completed");
                        if (curRequestNum + 1 < requestCount) {
                            sendRequest(curRequestNum + 1);
                        } else {
                            clientStatisticBroker.addAverageWorkTimeStatistic(System.currentTimeMillis() - startTime);
                        }
                    } catch (IOException e) {
                        log.error("Exception during message", e);
                        clientStatisticBroker.addAverageWorkTimeStatistic(System.currentTimeMillis() - startTime);
                    }

                }, nextDelay(),
                TimeUnit.MILLISECONDS);
    }
}
