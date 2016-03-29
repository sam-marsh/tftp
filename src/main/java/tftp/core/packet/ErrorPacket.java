package tftp.core.packet;

import tftp.core.Configuration;
import tftp.core.ErrorType;
import tftp.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class ErrorPacket extends TFTPPacket {

    private final ErrorType errorType;
    private final String message;
    private final ByteBuffer buffer;

    public ErrorPacket(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
        this.buffer = ByteBuffer.allocate(Configuration.MAX_PACKET_LENGTH);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(errorType.getValue());
        StringUtil.write(message, buffer);
        buffer.flip();
    }

    public ErrorPacket(ByteBuffer buffer) {
        buffer.position(2);
        this.errorType = ErrorType.fromValue(buffer.getShort());
        this.message = StringUtil.read(buffer);
        buffer.rewind();
        this.buffer = buffer;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public ByteBuffer getRawPacket() {
        return buffer;
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.ERROR;
    }

}
