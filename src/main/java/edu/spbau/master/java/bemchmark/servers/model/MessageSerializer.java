package edu.spbau.master.java.bemchmark.servers.model;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Serialize Array message to byte array.
 */
public final class MessageSerializer {
    private MessageSerializer() {
    }

    /**
     * Serialize array message to byte array.
     * First 4-bytes is message length.
     *
     * @param message array message
     * @return serialized message with message size
     * @throws IOException if IO Exception occurs
     */
    @NotNull
    public static byte[] serialize(@NotNull final Messages.ArrayMessage message) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(message.getSerializedSize() + 4);
        new DataOutputStream(byteStream).writeInt(message.getSerializedSize());
        message.writeTo(byteStream);
        return byteStream.toByteArray();
    }
}
