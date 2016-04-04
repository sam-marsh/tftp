package tftp.core.packet;

import tftp.core.ErrorType;
import tftp.core.TFTPException;
import tftp.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * Represents an error packet in the trivial file transfer protocol.
 */
public class ErrorPacket extends TFTPPacket {

    /**
     * The type of error contained in this packet.
     */
    private final ErrorType errorType;

    /**
     * The error message contained in this packet.
     */
    private final String message;

    /**
     * The raw packet bytes.
     */
    private final byte[] bytes;

    /**
     * Creates an error packet.
     *
     * @param errorType the type of error
     * @param message the message, generally containing specific information about the error
     */
    public ErrorPacket(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;

        byte[] messageBytes = StringUtil.getBytes(message);
        this.bytes = new byte[messageBytes.length + 4];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(errorType.getValue());
        buffer.put(messageBytes);
    }

    /**
     * Retrieves an error packet from raw packet bytes.
     *
     * @param bytes the buffer containing the packet bytes
     * @param length the length of the packet bytes in the buffer
     */
    public ErrorPacket(byte[] bytes, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(2);
        this.errorType = ErrorType.fromValue(buffer.getShort());
        this.message = StringUtil.getString(bytes, 4);
        this.bytes = new byte[length];
        System.arraycopy(bytes, 0, this.bytes, 0, length);
    }

    /**
     * @return the type of error represented by this packet
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * @return the error message contained in this packet, generally containing specific information about the error
     */
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacketType getPacketType() {
        return PacketType.ERROR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s[code=%d,message=%s]", getPacketType(), errorType.getValue(), message);
    }

}
