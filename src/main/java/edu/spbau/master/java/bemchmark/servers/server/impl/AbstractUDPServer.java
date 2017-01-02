package edu.spbau.master.java.bemchmark.servers.server.impl;

import edu.spbau.master.java.bemchmark.servers.server.ServerWithStatistic;
import edu.spbau.master.java.bemchmark.servers.sorting.SortingUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * Abstract UDP server
 */
@Slf4j
abstract class AbstractUDPServer extends ServerWithStatistic {
    final static int MAX_DATAGRAM_SIZE = 65507;

    protected final int datagramSize;

    protected AbstractUDPServer(int datagramSize) {
        this.datagramSize = datagramSize;
    }

    final void handle(@NotNull final DatagramPacket datagramPacket,
                      @NotNull final DatagramSocket datagramSocket) {
        long startProcessTime = System.currentTimeMillis();
        byte[] rawData = datagramPacket.getData();
        int[] intArray = new int[rawData.length / Integer.BYTES];
        ByteBuffer byteBuffer = ByteBuffer.wrap(rawData);
        byteBuffer.asIntBuffer().get(intArray);

        long startSortTime = System.currentTimeMillis();
        SortingUtils.bubbleSort(intArray);
        addToAverageClientProcessingTime(System.currentTimeMillis() - startSortTime);

        byteBuffer.asIntBuffer().put(intArray);
        DatagramPacket resultPacket = new DatagramPacket(rawData, rawData.length);
        resultPacket.setAddress(datagramPacket.getAddress());
        resultPacket.setPort(datagramPacket.getPort());
        try {
            datagramSocket.send(resultPacket);
            addToAverageRequestProcessingTime(System.currentTimeMillis() - startProcessTime);
        } catch (IOException e) {
            log.error("Exception while sending", e);
            datagramSocket.close();
        }
    }
}
