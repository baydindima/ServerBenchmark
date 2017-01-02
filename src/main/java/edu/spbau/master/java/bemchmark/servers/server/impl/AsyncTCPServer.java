package edu.spbau.master.java.bemchmark.servers.server.impl;

import edu.spbau.master.java.bemchmark.servers.model.MessageHandler;
import edu.spbau.master.java.bemchmark.servers.model.MessageDesializer;
import edu.spbau.master.java.bemchmark.servers.model.MessageSerializer;
import edu.spbau.master.java.bemchmark.servers.model.Messages;
import edu.spbau.master.java.bemchmark.servers.server.ServerWithStatistic;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * TCP server with async processing.
 */
@Slf4j
public final class AsyncTCPServer extends ServerWithStatistic {
    private volatile AsynchronousServerSocketChannel serverSocketChannel;

    @NotNull
    private final CompletionHandler<AsynchronousSocketChannel, Void> acceptCompletionHandler = new CompletionHandler<AsynchronousSocketChannel, Void>() {
        @Override
        public void completed(AsynchronousSocketChannel client, Void attachment) {
            serverSocketChannel.accept(null, this);
            ReadSizeContext readSizeContext = new ReadSizeContext(client);
            client.read(readSizeContext.sizeBuffer, readSizeContext, sizeReadCompletionHandler);
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
            log.error("Async accept exception", exc);
        }
    };

    @NotNull
    private final CompletionHandler<Integer, ReadSizeContext> sizeReadCompletionHandler =
            new CompletionHandler<Integer, ReadSizeContext>() {
                @Override
                public void completed(Integer result, ReadSizeContext readSizeContext) {
                    if (result == -1) {
                        return;
                    }
                    if (!readSizeContext.sizeBuffer.hasRemaining()) {
                        ReadMessageContext readMessageContext = new ReadMessageContext(readSizeContext, System.currentTimeMillis());
                        readMessageContext.clientSocketChannel.read(
                                readMessageContext.messageBuffer,
                                readMessageContext,
                                messageReadCompletionHandler
                        );
                    } else {
                        readSizeContext.clientSocketChannel.read(
                                readSizeContext.sizeBuffer,
                                readSizeContext,
                                sizeReadCompletionHandler
                        );
                    }
                }

                @Override
                public void failed(Throwable exc, ReadSizeContext attachment) {
                    log.error("Async size reading exception", exc);
                }
            };

    private final CompletionHandler<Integer, ReadMessageContext> messageReadCompletionHandler =
            new CompletionHandler<Integer, ReadMessageContext>() {
                @Override
                public void completed(Integer result, ReadMessageContext readMessageContext) {
                    if (result == -1) {
                        return;
                    }
                    if (!readMessageContext.messageBuffer.hasRemaining()) {
                        try {
                            Messages.ArrayMessage arrayMessage =
                                    MessageDesializer.deserialize(readMessageContext.messageBuffer.array());

                            long processingStart = System.currentTimeMillis();
                            Messages.ArrayMessage resultMessage = new MessageHandler().processMessage(arrayMessage);
                            addToAverageClientProcessingTime(System.currentTimeMillis() - processingStart);

                            ByteBuffer messageBuffer = ByteBuffer.wrap(MessageSerializer.serialize(resultMessage));
                            WriteMessageContext writeMessageContext = new WriteMessageContext(readMessageContext, messageBuffer);
                            writeMessageContext.clientSocketChannel.write(
                                    writeMessageContext.messageBuffer,
                                    writeMessageContext,
                                    messageWriteCompletionHandler
                            );
                        } catch (IOException exc) {
                            log.error("Exception while processing message", exc);
                        }
                    } else {
                        readMessageContext.clientSocketChannel.read(
                                readMessageContext.messageBuffer,
                                readMessageContext,
                                messageReadCompletionHandler
                        );
                    }
                }

                @Override
                public void failed(Throwable exc, ReadMessageContext attachment) {
                    log.error("Async message reading exception", exc);
                }
            };

    private final CompletionHandler<Integer, WriteMessageContext> messageWriteCompletionHandler =
            new CompletionHandler<Integer, WriteMessageContext>() {
                @Override
                public void completed(Integer result, WriteMessageContext writeMessageContext) {
                    if (!writeMessageContext.messageBuffer.hasRemaining()) {
                        addToAverageRequestProcessingTime(System.currentTimeMillis() - writeMessageContext.startRequestProcessingTime);
                        ReadSizeContext readSizeContext = new ReadSizeContext(writeMessageContext.clientSocketChannel);
                        writeMessageContext.clientSocketChannel.read(
                                readSizeContext.sizeBuffer,
                                readSizeContext,
                                sizeReadCompletionHandler
                        );
                    } else {
                        writeMessageContext.clientSocketChannel.write(
                                writeMessageContext.messageBuffer,
                                writeMessageContext,
                                messageWriteCompletionHandler
                        );
                    }
                }

                @Override
                public void failed(Throwable exc, WriteMessageContext attachment) {
                    log.error("Async message writing exception", exc);
                }
            };


    @Override
    public void start(int portNum) {
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(portNum));
            log.info("Server listening port {}", portNum);
            serverSocketChannel.accept(null, acceptCompletionHandler);
        } catch (IOException e) {
            log.error("Exception during server start.", e);
            stop();
        }
    }


    @Override
    public void stop() {
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.error("Exception during server closing.", e);
            }
        }
    }

    private final static class ReadSizeContext {
        private final AsynchronousSocketChannel clientSocketChannel;
        private final ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);

        private ReadSizeContext(AsynchronousSocketChannel clientSocketChannel) {
            this.clientSocketChannel = clientSocketChannel;
        }
    }

    private final static class ReadMessageContext {
        private final AsynchronousSocketChannel clientSocketChannel;
        private final long startRequestProcessingTime;
        private final ByteBuffer messageBuffer;

        private ReadMessageContext(@NotNull final ReadSizeContext readSizeContext, long startRequestProcessingTime) {
            this.clientSocketChannel = readSizeContext.clientSocketChannel;
            this.startRequestProcessingTime = startRequestProcessingTime;
            readSizeContext.sizeBuffer.flip();
            this.messageBuffer = ByteBuffer.allocate(readSizeContext.sizeBuffer.getInt());
        }

    }


    private final static class WriteMessageContext {
        private final AsynchronousSocketChannel clientSocketChannel;
        private final long startRequestProcessingTime;
        private final ByteBuffer messageBuffer;

        private WriteMessageContext(@NotNull final ReadMessageContext readMessageContext,
                                    @NotNull final ByteBuffer messageBuffer) {
            this.clientSocketChannel = readMessageContext.clientSocketChannel;
            this.startRequestProcessingTime = readMessageContext.startRequestProcessingTime;
            this.messageBuffer = messageBuffer;
        }
    }

}
