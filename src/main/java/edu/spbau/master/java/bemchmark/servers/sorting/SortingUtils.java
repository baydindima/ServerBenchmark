package edu.spbau.master.java.bemchmark.servers.sorting;

import org.jetbrains.annotations.NotNull;

/**
 * Class for sorting with n^2 sort
 */
public final class SortingUtils {
    private SortingUtils() {
    }

    /**
     * Bubble sort
     * @param array array for sorting
     */
    public static void bubbleSort(@NotNull final int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < array.length - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    int buf = array[j + 1];
                    array[j + 1] = array[j];
                    array[j] = buf;
                    swapped = true;
                }
            }

            if (!swapped) {
                break;
            }
        }
    }


}
