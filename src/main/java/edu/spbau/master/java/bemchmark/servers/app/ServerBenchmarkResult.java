package edu.spbau.master.java.bemchmark.servers.app;

import lombok.Data;

import java.io.Serializable;

/**
 * Result of server benchmark.
 */
@Data
public class ServerBenchmarkResult implements Serializable {

    private final long averageClientProcessingTime;

    private final long averageRequestProcessingTime;

}
