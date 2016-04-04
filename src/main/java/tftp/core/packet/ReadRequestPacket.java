package tftp.core.packet;

import tftp.core.Mode;
import tftp.core.TFTPException;

import java.nio.ByteBuffer;

/**
 * Represents a read-request (RRQ) packet in the trivial file transfer protocol.
 */
public class ReadRequestPacket extends RequestPacket {

    /**
     * {@inheritDoc}
     */
    public ReadRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    /**
     * {@inheritDoc}
     */
    public ReadRequestPacket(byte[] bytes, int length) throws TFTPException{
        super(bytes, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacketType getPacketType() {
        return PacketType.READ_REQUEST;
    }

}
