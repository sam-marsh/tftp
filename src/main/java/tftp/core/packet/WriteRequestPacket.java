package tftp.core.packet;

import tftp.core.Mode;
import tftp.core.TFTPException;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class WriteRequestPacket extends RequestPacket {

    public WriteRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    public WriteRequestPacket(byte[] bytes, int length) throws TFTPException{
        super(bytes, length);
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.WRITE_REQUEST;
    }

}
