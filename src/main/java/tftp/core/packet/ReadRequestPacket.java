package tftp.core.packet;

import tftp.core.Mode;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class ReadRequestPacket extends RequestPacket {

    public ReadRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    public ReadRequestPacket(ByteBuffer buffer) {
        super(buffer);
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.READ_REQUEST;
    }

}
