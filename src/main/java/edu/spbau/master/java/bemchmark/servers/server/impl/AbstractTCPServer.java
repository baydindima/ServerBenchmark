package edu.spbau.master.java.bemchmark.servers.server.impl;

import edu.spbau.master.java.bemchmark.servers.model.MessageHandler;
import edu.spbau.master.java.bemchmark.servers.model.MessageDesializer;
import edu.spbau.master.java.bemchmark.servers.model.MessageSerializer;
import edu.spbau.master.java.bemchmark.servers.model.Messages;
import edu.spbau.master.java.bemchmark.servers.server.ServerWithStatistic;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * Abstract server with common handle methods.
 */
@Slf4j
abstract class AbstractTCPServer extends ServerWithStatistic {

    void handleConnection(@NotNull final Socket clientSocket) {
        try {
            try (final InputStream inputStream = clientSocket.getInputStream();
                 final OutputStream outputStream = clientSocket.getOutputStream()) {
                while (!clientSocket.isClosed()) {
                    handle(inputStream, outputStream);
                }
            }
        } catch (SocketException e) {
            log.info("Socket closed");
        } catch (IOException e) {
            log.error("Exception during process message", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Exception during closing client socket", e);
            }
        }
    }

    void handle(@NotNull final InputStream inputStream,
                @NotNull final OutputStream outputStream) throws IOException {
        long processStart = System.currentTimeMillis();
        byte[] sizeBuffer = new byte[4];
        int readSize = 0;
        while (readSize < 4) {
            int result = inputStream.read(sizeBuffer, readSize, 4 - readSize);
            if (result == -1) {
                break;
            }
            readSize += result;
        }
        if (readSize < 4) {
            log.warn("Read less than size of int for size of message");
        }
        byte[] messageBuffer = new byte[ByteBuffer.wrap(sizeBuffer).getInt()];
        readSize = 0;
        while (readSize < messageBuffer.length) {
            int result = inputStream.read(messageBuffer, readSize, messageBuffer.length - readSize);
            if (result == -1) {
                break;
            }
            readSize += result;
        }
        if (readSize < messageBuffer.length) {
            log.warn("Read less than size of message");
        }
        Messages.ArrayMessage receivedMessage = MessageDesializer.deserialize(messageBuffer);
        long start = System.currentTimeMillis();
        Messages.ArrayMessage resultMessage = new MessageHandler().processMessage(receivedMessage);
        addToAverageClientProcessingTime(System.currentTimeMillis() - start);
        outputStream.write(MessageSerializer.serialize(resultMessage));
        addToAverageRequestProcessingTime(System.currentTimeMillis() - processStart);
    }
}
