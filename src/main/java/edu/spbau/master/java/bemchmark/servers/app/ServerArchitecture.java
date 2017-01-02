package edu.spbau.master.java.bemchmark.servers.app;

import edu.spbau.master.java.bemchmark.servers.client.ClientImplementation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Possible server architecture.
 */
public enum ServerArchitecture {
    SIMPLE_TCP(ClientImplementation.TCP_LONG_CONNECTION_CLIENT, "TCP server with blocking IO. Creates a new thread per client."),
    CACHED_THREADS_TCP(ClientImplementation.TCP_LONG_CONNECTION_CLIENT, "TCP server with cached pool of threads."),
    NON_BLOCKING_TCP(ClientImplementation.TCP_LONG_CONNECTION_CLIENT, "TCP server with non blocking processing."),
    ASYNC_TCP(ClientImplementation.TCP_LONG_CONNECTION_CLIENT, "TCP server with async processing."),
    SINGLE_THREAD_TCP(ClientImplementation.TCP_SHORT_CONNECTION_CLIENT, "TCP server with one thread."),
    SIMPLE_UDP(ClientImplementation.UDP_CLIENT, "UDP server. Creates a new thread per client."),
    THREAD_POOL_UDP(ClientImplementation.UDP_CLIENT, "UDP server with fixed thread pool.");

    @Getter
    @NotNull
    private final ClientImplementation clientImplementation;

    @NotNull
    private final String fullName;

    @Getter
    private final String shortName = super.toString();

    public static boolean isUDP(@NotNull final ServerArchitecture serverArchitecture) {
        return serverArchitecture == SIMPLE_UDP || serverArchitecture == THREAD_POOL_UDP;
    }

    ServerArchitecture(@NotNull final ClientImplementation clientImplementation,
                       @NotNull final String fullName) {
        this.clientImplementation = clientImplementation;
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
