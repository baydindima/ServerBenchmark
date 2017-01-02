package edu.spbau.master.java.bemchmark.servers.app;

import edu.spbau.master.java.bemchmark.servers.exception.NoSuchArchitectureException;
import edu.spbau.master.java.bemchmark.servers.server.ServerWithStatistic;
import edu.spbau.master.java.bemchmark.servers.server.impl.*;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for different server implementations.
 */
public final class ServerFactory {
    private ServerFactory() {
    }

    public static ServerWithStatistic getInstance(@NotNull final ServerBenchmarkConfig config) {
        switch (config.getServerArchitecture()) {
            case SIMPLE_TCP:
                return new SimpleTCPServer();
            case CACHED_THREADS_TCP:
                return new CachedThreadTCPThread();
            case NON_BLOCKING_TCP:
                return new NonBlockingTCPServer();
            case ASYNC_TCP:
                return new AsyncTCPServer();
            case SINGLE_THREAD_TCP:
                return new SingleThreadTCPServer();
            case SIMPLE_UDP:
                return new SimpleUDPServer(config.getDatagramSize());
            case THREAD_POOL_UDP:
                return new ThreadPoolUDPServer(config.getDatagramSize());
        }
        throw new NoSuchArchitectureException();
    }


}
