package tftp.core.packet;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public abstract class RequestPacket extends TFTPPacket {

    private final String file;
    private final Mode mode;
    private final ByteBuffer buffer;

    public RequestPacket(String file, Mode mode) {
        this.file = file;
        this.mode = mode;
        this.buffer = ByteBuffer.allocate(Configuration.MAX_PACKET_LENGTH);
        buffer.putShort(getPacketType().getOpcode());
        StringUtil.write(file, buffer);
        StringUtil.write(mode.getName(), buffer);
        buffer.flip();
    }

    public RequestPacket(ByteBuffer buffer) {
        buffer.position(2);
        this.file = StringUtil.read(buffer);
        this.mode = Mode.fromName(StringUtil.read(buffer));
        buffer.rewind();
        this.buffer = buffer;
    }

    @Override
    public ByteBuffer getRawPacket() {
        return buffer;
    }

}
