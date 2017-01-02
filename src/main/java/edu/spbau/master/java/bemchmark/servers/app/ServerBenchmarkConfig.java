package edu.spbau.master.java.bemchmark.servers.app;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Describe servers's settings for benchmark.
 */
@Data
public final class ServerBenchmarkConfig implements Serializable {
    @NotNull
    private final ServerArchitecture serverArchitecture;


    private final int serverPort;

    /**
     * Only for UDP.
     */
    private final int datagramSize;

}
