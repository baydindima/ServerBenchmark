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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TCP server with non blocking processing.
 */
@Slf4j
public final class NonBlockingTCPServer extends ServerWithStatistic {
    @NotNull
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private volatile Selector selector;
    private volatile ServerSocketChannel serverSocketChannel;


    @Override
    public void start(int portNum) {
        new Thread(() -> {
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();

                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.bind(new InetSocketAddress(portNum), Integer.MAX_VALUE);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                while (serverSocketChannel.isOpen()) {
                    int selectKeysCount = selector.select();
                    if (selectKeysCount > 0) {
                        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey selectionKey = keyIterator.next();
                            if (selectionKey.isAcceptable()) {
                                accept(selectionKey);
                            } else {
                                if (selectionKey.isReadable()) {
                                    read(selectionKey);
                                }

                                if (selectionKey.isValid() && selectionKey.isWritable()) {
                                    write(selectionKey);
                                }
                            }
                            keyIterator.remove();
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Exception during non blocking server work", e);
                close();
            }
        }).start();
    }

    @Override
    public void stop() {
        close();
        log.info("Non blocking server closed.");
    }

    private void close() {
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.error("Exception during close server socket channel", e);
            }
        }

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                log.error("Exception during close selector", e);
            }
        }

        executorService.shutdown();
    }

    private void write(@NotNull final SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (selectionKey.attachment() == null) {
            log.warn("Unexpected attachment in write method. Client {}", channel.getRemoteAddress().toString());
            return;
        }
        StatusWithContext statusWithContext = (StatusWithContext) selectionKey.attachment();
        if (statusWithContext.status == ProcessingStatus.WRITE) {
            WriteContext context = (WriteContext) statusWithContext.context;
            channel.write(context.messageBuffer);
            if (!context.messageBuffer.hasRemaining()) {
                addToAverageRequestProcessingTime(System.currentTimeMillis() - context.startRequestProcessingTime);
                selectionKey.attach(null);
                channel.register(selector,
                        SelectionKey.OP_READ,
                        null);
            }
        } else if (statusWithContext.status == ProcessingStatus.PROCESSING) {
            ProcessingContext context = (ProcessingContext) statusWithContext.context;
            if (context.futureResponse.isDone()) {
                try {
                    WriteContext writeContext = new WriteContext(context, context.futureResponse.get());
                    channel.write(writeContext.messageBuffer);
                    if (writeContext.messageBuffer.hasRemaining()) {
                        selectionKey.attach(new StatusWithContext(ProcessingStatus.WRITE, writeContext));
                    } else {
                        addToAverageRequestProcessingTime(System.currentTimeMillis() - context.startRequestProcessingTime);
                        selectionKey.attach(null);
                        channel.register(selector,
                                SelectionKey.OP_READ,
                                null);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Exception while processing message.", e);
                    channel.close();
                }
            }
        } else {
            log.warn("Unexpected status of context in write method: " + statusWithContext.status.toString());
        }

    }

    private void read(@NotNull final SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (selectionKey.attachment() == null) {
            selectionKey.attach(
                    new StatusWithContext(
                            ProcessingStatus.READ_SIZE,
                            new ReadSizeContext(System.currentTimeMillis()))
            );
        }
        StatusWithContext statusWithContext = (StatusWithContext) selectionKey.attachment();
        if (statusWithContext.status == ProcessingStatus.READ_MESSAGE) {
            ReadMessageContext context = (ReadMessageContext) statusWithContext.context;
            channel.read(context.messageBuffer);
            if (!context.messageBuffer.hasRemaining()) {
                final Messages.ArrayMessage arrayMessage = MessageDesializer.deserialize(context.messageBuffer.array());

                Future<ByteBuffer> futureResponse = executorService.submit(() -> {
                    long processingStart = System.currentTimeMillis();
                    Messages.ArrayMessage resultMessage = new MessageHandler().processMessage(arrayMessage);
                    addToAverageClientProcessingTime(System.currentTimeMillis() - processingStart);
                    return ByteBuffer.wrap(MessageSerializer.serialize(resultMessage));
                });

                channel.register(selector,
                        SelectionKey.OP_WRITE,
                        new StatusWithContext(
                                ProcessingStatus.PROCESSING,
                                new ProcessingContext(context, futureResponse))
                );
            }
        } else if (statusWithContext.status == ProcessingStatus.READ_SIZE) {
            ReadSizeContext context = (ReadSizeContext) statusWithContext.context;
            int read = channel.read(context.sizeBuffer);
            if (read == -1) {
                selectionKey.cancel();
                channel.close();
            }
            if (!context.sizeBuffer.hasRemaining()) {
                selectionKey.attach(
                        new StatusWithContext(ProcessingStatus.READ_MESSAGE, new ReadMessageContext(context)));
            }
        } else {
            log.warn("Unexpected status of context in read method: " + statusWithContext.status.toString());
        }
    }

    private void accept(@NotNull final SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector,
                SelectionKey.OP_READ,
                null);
    }


    private enum ProcessingStatus {
        READ_SIZE,
        READ_MESSAGE,
        PROCESSING,
        WRITE
    }

    private static final class StatusWithContext {
        private final ProcessingStatus status;
        private final RequestContext context;

        private StatusWithContext(ProcessingStatus status, RequestContext context) {
            this.status = status;
            this.context = context;
        }
    }

    private interface RequestContext {
    }

    private static final class ReadSizeContext implements RequestContext {
        private final long startRequestProcessingTime;
        @NotNull
        private final ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);

        private ReadSizeContext(long startRequestProcessingTime) {
            this.startRequestProcessingTime = startRequestProcessingTime;
        }
    }

    private static final class ReadMessageContext implements RequestContext {
        private final long startRequestProcessingTime;
        @NotNull
        private final ByteBuffer messageBuffer;

        private ReadMessageContext(@NotNull ReadSizeContext readSizeContext) {
            startRequestProcessingTime = readSizeContext.startRequestProcessingTime;
            readSizeContext.sizeBuffer.flip();
            messageBuffer = ByteBuffer.allocate(readSizeContext.sizeBuffer.getInt());
        }
    }

    private static final class ProcessingContext implements RequestContext {
        private final long startRequestProcessingTime;
        @NotNull
        private final Future<ByteBuffer> futureResponse;

        private ProcessingContext(@NotNull final ReadMessageContext context,
                                  @NotNull final Future<ByteBuffer> futureResponse) {
            this.startRequestProcessingTime = context.startRequestProcessingTime;
            this.futureResponse = futureResponse;
        }
    }

    private static final class WriteContext implements RequestContext {
        private final long startRequestProcessingTime;
        @NotNull
        private final ByteBuffer messageBuffer;

        private WriteContext(@NotNull final ProcessingContext context, @NotNull ByteBuffer messageBuffer) {
            this.startRequestProcessingTime = context.startRequestProcessingTime;
            this.messageBuffer = messageBuffer;
        }
    }

}
