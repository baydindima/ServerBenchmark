package edu.spbau.master.java.bemchmark.servers.model;

import edu.spbau.master.java.bemchmark.servers.model.Messages;
import edu.spbau.master.java.bemchmark.servers.sorting.SortingUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Class for handle messages
 */
public final class MessageHandler {

    /**
     * Sort elements in array message
     *
     * @param arrayMessage array message
     * @return array message with sorted elements
     */
    @NotNull
    public Messages.ArrayMessage processMessage(@NotNull final
                                                Messages.ArrayMessage arrayMessage) {
        int[] elements = new int[arrayMessage.getElementCount()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = arrayMessage.getElements(i);
        }
        SortingUtils.bubbleSort(elements);
        Messages.ArrayMessage.Builder builder = arrayMessage.toBuilder();
        for (int i = 0; i < elements.length; i++) {
            builder.setElements(i, elements[i]);
        }
        return builder.build();
    }

}
