package edu.spbau.master.java.bemchmark.servers.client.impl;

import edu.spbau.master.java.bemchmark.servers.client.AbstractClient;
import edu.spbau.master.java.bemchmark.servers.client.ClientStatisticBroker;
import edu.spbau.master.java.bemchmark.servers.model.MessageFactory;
import edu.spbau.master.java.bemchmark.servers.model.MessageSerializer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP client without closing connection after request.
 */
@Slf4j
public final class TCPLongConnectionClient extends AbstractClient {

    private volatile long startTime;

    @Builder
    public TCPLongConnectionClient(int elementPerRequest,
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
        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            final AtomicInteger curRequestNum = new AtomicInteger();
            socket = new Socket(serverAddress, serverPort);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            startScheduling(curRequestNum, socket, inputStream, outputStream);
        } catch (IOException e) {
            log.error("Error while opening socket.", e);
            close(socket, inputStream, outputStream);
        }
    }

    private void startScheduling(@NotNull
                                 final AtomicInteger curRequestNum,
                                 @NotNull
                                 final Socket socket,
                                 @NotNull
                                 final InputStream inputStream,
                                 @NotNull
                                 final OutputStream outputStream
    ) {
        executorService.schedule(
                () -> query(curRequestNum, socket, inputStream, outputStream),
                nextDelay(),
                TimeUnit.MILLISECONDS);
    }

    private void query(@NotNull
                       final AtomicInteger curRequestNum,
                       @NotNull
                       final Socket socket,
                       @NotNull
                       final InputStream inputStream,
                       @NotNull
                       final OutputStream outputStream) {
        try {
            @NotNull byte[] messageBytes = MessageSerializer.serialize(
                    MessageFactory.newMessage(elementPerRequest));
            outputStream.write(messageBytes);
            int readCount = inputStream.read(messageBytes);
            while (readCount < messageBytes.length) {
                readCount += inputStream.read(messageBytes, readCount, messageBytes.length - readCount);
            }
            if (readCount != messageBytes.length) {
                log.warn(
                        String.format("Received message length (%d) hasn't equal to sent message length (%d)",
                                readCount, messageBytes.length));
            }
            log.info("Request completed");
            if (curRequestNum.incrementAndGet() < requestCount) {
                executorService.schedule(
                        () -> query(curRequestNum, socket, inputStream, outputStream),
                        nextDelay(),
                        TimeUnit.MILLISECONDS);
            } else {
                close(socket, inputStream, outputStream);
                clientStatisticBroker.addAverageWorkTimeStatistic(System.currentTimeMillis() - startTime);
            }
        } catch (IOException e) {
            log.error("Error while communicate with server", e);
            close(socket, inputStream, outputStream);
        }

    }

    private void close(
            @Nullable
            final Socket socket,
            @Nullable
            final InputStream inputStream,
            @Nullable
            final OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e1) {
                log.error("Exception while closing output stream.", e1);
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e1) {
                log.error("Exception while closing input stream.", e1);
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e1) {
                log.error("Exception while closing socket.", e1);
            }
        }
    }

}
