package tftp.core.packet;

import tftp.core.Mode;
import tftp.core.TFTPException;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class ReadRequestPacket extends RequestPacket {

    public ReadRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    public ReadRequestPacket(byte[] bytes, int length) throws TFTPException{
        super(bytes, length);
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.READ_REQUEST;
    }

}
