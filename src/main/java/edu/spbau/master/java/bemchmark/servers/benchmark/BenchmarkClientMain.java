package edu.spbau.master.java.bemchmark.servers.benchmark;

import edu.spbau.master.java.bemchmark.servers.app.ServerArchitecture;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

/**
 * Main class for benchmark client.
 * For fast tests.
 */
public final class BenchmarkClientMain {

    private static int clientCount;
    private static int requestCount;
    private static int delayBetweenRequest;
    private static int elementPerRequestCount;

    private static void initValues() {
        clientCount = 100;
        requestCount = 10;
        delayBetweenRequest = 10;
        elementPerRequestCount = 3000;
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        initValues();
        for (ServerArchitecture serverArchitecture : ServerArchitecture.values()) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(serverArchitecture.getShortName() + "_client_test")))) {
                for (clientCount = 1; clientCount < 502; clientCount += 25) {
                    System.out.println("Current client count is " + clientCount);
                    BenchmarkResult result = BenchmarkRunner.builder()
                            .serverAddress(InetAddress.getLocalHost())
                            .elementPerRequest(elementPerRequestCount)
                            .delayBetweenRequest(delayBetweenRequest)
                            .requestCount(requestCount)
                            .mainServerPort(27527)
                            .testServerPort(27528)
                            .serverArchitecture(serverArchitecture)
                            .clientCount(clientCount)
                            .build()
                            .start()
                            .get();
                    out.write((result.getAverageClientProcessingTime() + "\t" + result.getAverageRequestProcessingTime() + "\t" + result.getAverageWorkTime() + "\t" + clientCount + "\n").getBytes());
                }
            }
        }

        initValues();
        for (ServerArchitecture serverArchitecture : ServerArchitecture.values()) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(serverArchitecture.getShortName() + "_delay_test")))) {
                for (delayBetweenRequest = 1; delayBetweenRequest < 1002; delayBetweenRequest += 50) {
                    System.out.println("Current delay time is " + delayBetweenRequest);
                    BenchmarkResult result = BenchmarkRunner.builder()
                            .serverAddress(InetAddress.getLocalHost())
                            .elementPerRequest(elementPerRequestCount)
                            .delayBetweenRequest(delayBetweenRequest)
                            .requestCount(requestCount)
                            .mainServerPort(27527)
                            .testServerPort(27528)
                            .serverArchitecture(serverArchitecture)
                            .clientCount(clientCount)
                            .build()
                            .start()
                            .get();
                    out.write((result.getAverageClientProcessingTime() + "\t" + result.getAverageRequestProcessingTime() + "\t" + result.getAverageWorkTime() + "\t" + delayBetweenRequest + "\n").getBytes());
                }
            }
        }

        initValues();
        for (ServerArchitecture serverArchitecture : ServerArchitecture.values()) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(serverArchitecture.getShortName() + "_element_test")))) {
                for (elementPerRequestCount = 100; elementPerRequestCount < 5102; elementPerRequestCount += 250) {
                    System.out.println("Current element count is " + elementPerRequestCount);
                    BenchmarkResult result = BenchmarkRunner.builder()
                            .serverAddress(InetAddress.getLocalHost())
                            .elementPerRequest(elementPerRequestCount)
                            .delayBetweenRequest(delayBetweenRequest)
                            .requestCount(requestCount)
                            .mainServerPort(27527)
                            .testServerPort(27528)
                            .serverArchitecture(serverArchitecture)
                            .clientCount(clientCount)
                            .build()
                            .start()
                            .get();
                    out.write((result.getAverageClientProcessingTime() + "\t" + result.getAverageRequestProcessingTime() + "\t" + result.getAverageWorkTime() + "\t" + elementPerRequestCount + "\n").getBytes());
                }
            }
        }
    }

}
