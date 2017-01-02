package edu.spbau.master.java.bemchmark.servers.benchmark;

import edu.spbau.master.java.bemchmark.servers.app.ServerArchitecture;
import edu.spbau.master.java.bemchmark.servers.app.ServerBenchmarkConfig;
import edu.spbau.master.java.bemchmark.servers.app.ServerBenchmarkResult;
import edu.spbau.master.java.bemchmark.servers.app.ServerGetStatQuery;
import edu.spbau.master.java.bemchmark.servers.client.ClientStarter;
import edu.spbau.master.java.bemchmark.servers.client.ClientStatisticBroker;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Run one benchmark.
 * Start clients.
 * Send to server config.
 */
@Builder
@Slf4j
public final class BenchmarkRunner {


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
    protected final int mainServerPort;
    /**
     * Port of server.
     */
    protected final int testServerPort;
    /**
     * Inet address of server.
     */
    @NotNull
    private final InetAddress serverAddress;

    private final ServerArchitecture serverArchitecture;

    @NotNull
    private ServerBenchmarkConfig getServerBenchmarkConfig() {
        return new ServerBenchmarkConfig(serverArchitecture, testServerPort, elementPerRequest * Integer.BYTES);
    }

    /**
     * Start clients.
     * Send to server config.
     */
    public CompletableFuture<BenchmarkResult> start() {
        CompletableFuture<BenchmarkResult> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println(serverArchitecture.toString());
                final Socket socket = new Socket(serverAddress, mainServerPort);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.writeObject(getServerBenchmarkConfig());

                ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(clientCount);
                ClientStatisticBroker clientStatisticBroker = new ClientStatisticBroker(cs -> {
                    try {
                        objectOutputStream.writeObject(new ServerGetStatQuery());
                        ServerBenchmarkResult result = (ServerBenchmarkResult) new ObjectInputStream(socket.getInputStream()).readObject();

                        System.out.println(result.getAverageClientProcessingTime() + "\t" + result.getAverageRequestProcessingTime() + "\t" + cs.getAverageWorkTime());

                        socket.close();
                        poolExecutor.shutdown();
                        future.complete(new BenchmarkResult(
                                result.getAverageClientProcessingTime(),
                                result.getAverageRequestProcessingTime(),
                                cs.getAverageWorkTime()
                        ));
                    } catch (ClassNotFoundException | IOException e) {
                        log.error("Exception during get stat from server", e);
                        future.completeExceptionally(e);
                    }
                }, clientCount);

                Thread.sleep(1000);

                ClientStarter.builder()
                        .clientCount(clientCount)
                        .clientImplementation(serverArchitecture.getClientImplementation())
                        .clientStatisticBroker(clientStatisticBroker)
                        .delayBetweenRequest(delayBetweenRequest)
                        .elementPerRequest(elementPerRequest)
                        .serverAddress(serverAddress)
                        .serverPort(testServerPort)
                        .executorService(poolExecutor)
                        .requestCount(requestCount)
                        .build()
                        .start();

            } catch (InterruptedException | IOException e) {
                log.error("Exception during benchmark", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }


}
