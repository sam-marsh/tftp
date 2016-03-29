package tftp.core.packet;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public abstract class TFTPPacket {

    public abstract ByteBuffer getRawPacket();

    public abstract PacketType getPacketType();

}
