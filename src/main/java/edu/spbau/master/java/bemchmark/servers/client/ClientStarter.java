package edu.spbau.master.java.bemchmark.servers.client;


import edu.spbau.master.java.bemchmark.servers.client.impl.TCPLongConnectionClient;
import edu.spbau.master.java.bemchmark.servers.client.impl.TCPShortConnectionClient;
import edu.spbau.master.java.bemchmark.servers.client.impl.UDPClient;
import edu.spbau.master.java.bemchmark.servers.exception.NoSuchClientImplementation;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;

@Builder
public class ClientStarter {

    @NotNull
    private final ClientImplementation clientImplementation;

    private final int clientCount;
    /**
     * Count of elements per request.
     */
    private final int elementPerRequest;

    /**
     * Count of request which will make client.
     */
    private final int requestCount;
    /**
     * Average delay between requests in milliseconds.
     */
    private final int delayBetweenRequest;

    /**
     * Port of server.
     */
    protected final int serverPort;
    /**
     * Inet address of server.
     */
    @NotNull
    private final InetAddress serverAddress;
    /**
     * Executor service for requests.
     */
    @NotNull
    private final ScheduledExecutorService executorService;

    /**
     * Collects work time statistic.
     */
    @NotNull
    private final ClientStatisticBroker clientStatisticBroker;

    @NotNull
    private AbstractClient getClient() {
        switch (clientImplementation) {
            case TCP_LONG_CONNECTION_CLIENT:
                return TCPLongConnectionClient.builder()
                        .delayBetweenRequest(delayBetweenRequest)
                        .elementPerRequest(elementPerRequest)
                        .requestCount(requestCount)
                        .serverPort(serverPort)
                        .clientStatisticBroker(clientStatisticBroker)
                        .serverAddress(serverAddress)
                        .executorService(executorService)
                        .build();
            case TCP_SHORT_CONNECTION_CLIENT:
                return TCPShortConnectionClient.builder()
                        .delayBetweenRequest(delayBetweenRequest)
                        .elementPerRequest(elementPerRequest)
                        .requestCount(requestCount)
                        .serverPort(serverPort)
                        .clientStatisticBroker(clientStatisticBroker)
                        .serverAddress(serverAddress)
                        .executorService(executorService)
                        .build();
            case UDP_CLIENT:
                return UDPClient.builder()
                        .delayBetweenRequest(delayBetweenRequest)
                        .elementPerRequest(elementPerRequest)
                        .requestCount(requestCount)
                        .serverPort(serverPort)
                        .clientStatisticBroker(clientStatisticBroker)
                        .serverAddress(serverAddress)
                        .executorService(executorService)
                        .build();
        }
        throw new NoSuchClientImplementation();
    }

    public void start() {
        for (int i = 0; i < clientCount; i++) {
            getClient().start();
        }
    }


}
