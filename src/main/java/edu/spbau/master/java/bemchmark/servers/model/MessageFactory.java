package edu.spbau.master.java.bemchmark.servers.model;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Class for generating ArrayMessage.
 */
public final class MessageFactory {
    private MessageFactory() {
    }

    /**
     * Create new array message.
     *
     * @param elementCount count of elements in array message.
     * @return array message elements with unsorted elements
     */
    @NotNull
    public static Messages.ArrayMessage newMessage(final int elementCount) {
        Messages.ArrayMessage.Builder builder = Messages.ArrayMessage.newBuilder();
        builder.setElementCount(elementCount);
        ThreadLocalRandom localRandom = ThreadLocalRandom.current();
        for (int i = 0; i < elementCount; i++) {
            builder.addElements(localRandom.nextInt());
        }
        return builder.build();
    }

}
