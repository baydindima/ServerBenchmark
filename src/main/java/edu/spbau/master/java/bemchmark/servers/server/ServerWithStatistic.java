package edu.spbau.master.java.bemchmark.servers.server;

import edu.spbau.master.java.bemchmark.servers.statistic.AverageTimeStatistic;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Server that provides some statistics
 */
public abstract class ServerWithStatistic implements Server {
    /**
     * Average time for message processing.
     */
    @NotNull
    private final AverageTimeStatistic averageClientProcessing = new AverageTimeStatistic();
    /**
     * Average time for message reading, processing and writing.
     */
    @NotNull
    private final AverageTimeStatistic averageRequestProcessing = new AverageTimeStatistic();


    /**
     * Add new measure to average time for message processing.
     *
     * @param time measured time.
     */
    protected final void addToAverageClientProcessingTime(final long time) {
        averageClientProcessing.addNewStat(time);
    }

    /**
     * Get current average time for message processing.
     *
     * @return current average time for message processing.
     */
    public final long getAverageClientProcessingTime() {
        return averageClientProcessing.getCurrentAverageTime();
    }

    /**
     * Add new measure to average time for request processing.
     *
     * @param time measured time.
     */
    protected final void addToAverageRequestProcessingTime(final long time) {
        averageRequestProcessing.addNewStat(time);
    }

    /**
     * Get current average time for request processing,.
     *
     * @return current average time for message processing.
     */
    public final long getAverageRequestProcessingTime() {
        return averageRequestProcessing.getCurrentAverageTime();
    }
}
