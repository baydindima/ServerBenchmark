package edu.spbau.master.java.bemchmark.servers.client;

import edu.spbau.master.java.bemchmark.servers.model.MessageFactory;
import edu.spbau.master.java.bemchmark.servers.model.MessageSerializer;
import lombok.AllArgsConstructor;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract client for benchmark
 */
@AllArgsConstructor
@Slf4j
public abstract class AbstractClient {
    /**
     * Max inaccuracy when calc next delay.
     */
    static final double MAX_EPS = 0.2;
    /**
     * Init delay before start sends requests.
     */
    static final int INITIAL_DELAY = 0;

    /**
     * Count of elements per request.
     */
    protected final int elementPerRequest;

    /**
     * Count of request which will make client.
     */
    protected final int requestCount;
    /**
     * Average delay between requests in milliseconds.
     */
    protected final int delayBetweenRequest;

    /**
     * Port of server.
     */
    protected final int serverPort;
    /**
     * Inet address of server.
     */
    @NotNull
    protected final InetAddress serverAddress;
    /**
     * Executor service for requests.
     */
    @NotNull
    protected final ScheduledExecutorService executorService;

    /**
     * Collects work time statistic.
     */
    @NotNull
    protected final ClientStatisticBroker clientStatisticBroker;

    public abstract void start();

    protected final int nextDelay() {
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        return (int) (delayBetweenRequest + delayBetweenRequest *
                (MAX_EPS * (localRandom.nextBoolean() ? 1 : -1) * localRandom.nextDouble()));
    }


}
