package edu.spbau.master.java.bemchmark.servers.statistic;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class for storing average time.
 */
public final class AverageTimeStatistic {
    @NotNull
    private final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();
    @NotNull
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantLock.readLock();
    @NotNull
    private final ReentrantReadWriteLock.WriteLock writeLock = reentrantLock.writeLock();


    private volatile long sumTime;

    private volatile int statCount;

    public void addNewStat(final long time) {
        writeLock.lock();
        try {
            sumTime += time;
            statCount += 1;
        } finally {
            writeLock.unlock();
        }
    }

    public long getCurrentAverageTime() {
        readLock.lock();
        try {
            if (statCount == 0) {
                return -1;
            }
            return sumTime / statCount;
        } finally {
            readLock.unlock();
        }
    }
}
