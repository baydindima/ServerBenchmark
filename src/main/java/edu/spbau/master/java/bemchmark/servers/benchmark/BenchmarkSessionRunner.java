package edu.spbau.master.java.bemchmark.servers.benchmark;

import edu.spbau.master.java.bemchmark.servers.app.ServerArchitecture;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Run session of benchmarks.
 */
@Slf4j
@Builder
public final class BenchmarkSessionRunner {

    @NotNull
    private final InetAddress serverAddress;

    @NotNull
    private final ServerArchitecture serverArchitecture;

    private final int mainServerPort;

    private final int testServerPort;

    private final int requestCount;

    @NotNull
    private final Consumer<BenchmarkResultWithVariableValue> onResult;

    public CompletableFuture<Void> runWithVariableElementsPerRequest(
            final int startElementPerRequest,
            final int endElementPerRequest,
            final int step,
            final int clientCount,
            final int delayBetweenRequest) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                for (int elementPerRequest = startElementPerRequest; elementPerRequest <= endElementPerRequest; elementPerRequest += step) {
                    if (future.isDone()) {
                        break;
                    }
                    BenchmarkResult benchmarkResult = runBenchmark(clientCount, elementPerRequest, delayBetweenRequest);
                    onResult.accept(new BenchmarkResultWithVariableValue(benchmarkResult, elementPerRequest));
                }
                future.complete(null);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception during session of benchmark", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }


    public CompletableFuture<Void> runWithVariableClientCount(
            final int startClientCount,
            final int endClientCount,
            final int step,
            final int elementPerRequest,
            final int delayBetweenRequest
    ) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                for (int clientCount = startClientCount; clientCount <= endClientCount; clientCount += step) {
                    if (future.isDone()) {
                        break;
                    }
                    BenchmarkResult benchmarkResult = runBenchmark(clientCount, elementPerRequest, delayBetweenRequest);
                    onResult.accept(new BenchmarkResultWithVariableValue(benchmarkResult, clientCount));
                }
                future.complete(null);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception during session of benchmark", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }


    public CompletableFuture<Void> runWithVariableDelayBetweenRequest(
            final int startDelayBetweenRequest,
            final int endDelayBetweenRequest,
            final int step,
            final int clientCount,
            final int elementPerRequest
    ) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                for (int delayBetweenRequest = startDelayBetweenRequest; delayBetweenRequest <= endDelayBetweenRequest; delayBetweenRequest += step) {
                    if (future.isDone()) {
                        break;
                    }
                    BenchmarkResult benchmarkResult = runBenchmark(clientCount, elementPerRequest, delayBetweenRequest);
                    onResult.accept(new BenchmarkResultWithVariableValue(benchmarkResult, delayBetweenRequest));
                }
                future.complete(null);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Exception during session of benchmark", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @NotNull
    private BenchmarkResult runBenchmark(
            final int clientCount,
            final int elementPerRequest,
            final int delayBetweenRequest
    ) throws ExecutionException, InterruptedException {
        return BenchmarkRunner.builder()
                .serverAddress(serverAddress)
                .elementPerRequest(elementPerRequest)
                .delayBetweenRequest(delayBetweenRequest)
                .requestCount(requestCount)
                .mainServerPort(mainServerPort)
                .testServerPort(testServerPort)
                .serverArchitecture(serverArchitecture)
                .clientCount(clientCount)
                .build()
                .start()
                .get();
    }

    @Data
    public final static class BenchmarkResultWithVariableValue {
        @NotNull
        private final BenchmarkResult benchmarkResult;

        private final int variableValue;

    }

}
