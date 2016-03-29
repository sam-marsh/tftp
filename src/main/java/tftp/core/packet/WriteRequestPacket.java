package tftp.core.packet;

import tftp.core.Mode;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class WriteRequestPacket extends RequestPacket {

    public WriteRequestPacket(ByteBuffer buffer) {
        super(buffer);
    }

    public WriteRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.WRITE_REQUEST;
    }

}
