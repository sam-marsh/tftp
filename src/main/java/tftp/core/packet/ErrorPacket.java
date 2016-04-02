package tftp.core.packet;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.TFTPException;
import tftp.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class ErrorPacket extends TFTPPacket {

    private final ErrorType errorType;
    private final String message;
    private final byte[] bytes;

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

    public ErrorPacket(byte[] bytes, int length) throws TFTPException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(2);
        this.errorType = ErrorType.fromValue(buffer.getShort());
        this.message = StringUtil.getString(bytes, 4);
        this.bytes = new byte[length];
        System.arraycopy(bytes, 0, this.bytes, 0, length);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.ERROR;
    }

}
