package edu.spbau.master.java.bemchmark.servers.client;


import edu.spbau.master.java.bemchmark.servers.statistic.AverageTimeStatistic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Collects work time statistic for  all clients.
 */
@AllArgsConstructor
public final class ClientStatisticBroker {

    @NotNull
    private final Consumer<ClientStatisticBroker> callback;

    private final int clientCount;

    @NotNull
    final private AverageTimeStatistic averageWorkTimeStatistic = new AverageTimeStatistic();

    @Getter
    @NotNull
    final private AtomicInteger statisticCount = new AtomicInteger();

    public void addAverageWorkTimeStatistic(final long time) {
        averageWorkTimeStatistic.addNewStat(time);
        if (statisticCount.incrementAndGet() == clientCount) {
            callback.accept(this);
        }
    }

    public long getAverageWorkTime() {
        return averageWorkTimeStatistic.getCurrentAverageTime();
    }

}
