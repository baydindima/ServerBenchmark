package edu.spbau.master.java.bemchmark.servers.benchmark;

import lombok.Data;

/**
 * Result of benchmark run (client processing time, request processing time, work time).
 */
@Data
public final class BenchmarkResult {

    private final long averageClientProcessingTime;

    private final long averageRequestProcessingTime;

    private final long averageWorkTime;

}
