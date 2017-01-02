package edu.spbau.master.java.bemchmark.servers.model;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jetbrains.annotations.NotNull;

/**
 * Deserialize byte array to array message
 */
public final class MessageDesializer {

    /**
     * Deserialize byte array to array message
     *
     * @param message byte array that contains message array
     * @return deserialized message array
     * @throws InvalidProtocolBufferException Thrown when a protocol message being parsed is invalid in some way,
     *                                        e.g. it contains a malformed varint or a negative byte length.
     */
    @NotNull
    public static Messages.ArrayMessage deserialize(@NotNull final byte[] message) throws InvalidProtocolBufferException {
        return Messages.ArrayMessage.parseFrom(message);
    }

}
