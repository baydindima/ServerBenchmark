package edu.spbau.master.java.bemchmark.servers.sorting;

import org.junit.Test;

//import org.opentest4j.AssertionFailedError;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;


public class SortingUtilsTest {


    @Test
    public void singleElement() {
        test(new int[]{1});
    }

    @Test
    public void simpleSorted() {
        test(new int[]{1, 2, 3});
    }

    @Test
    public void simpleNotSorted() {
        test(new int[]{1, 4, 3});
    }

    @Test
    public void negative() {
        test(new int[]{1, -4, -3});
    }

    @Test
    public void randomTest() {
        for (int i = 0; i < 100; i++) {
            int arraySize = Math.abs(ThreadLocalRandom.current().nextInt() % 100);
            int[] array = new int[arraySize];
            for (int j = 0; j < array.length; j++) {
                array[j] = ThreadLocalRandom.current().nextInt();
            }
            test(array);
        }
    }


    private void test(int[] array) {
        SortingUtils.bubbleSort(array);
        try {
            assertIsSorted(array);
        } catch (AssertionError error) {
            System.err.println("Failed on array: " + Arrays.toString(array));
            throw error;
        }
    }


    private void assertIsSorted(int[] array) {
        if (array.length == 0) {
            return;
        }
        int prev = array[0];
        for (int i = 1; i < array.length; i++) {
            assertTrue(prev <= array[i]);
            prev = array[i];
        }
    }


}