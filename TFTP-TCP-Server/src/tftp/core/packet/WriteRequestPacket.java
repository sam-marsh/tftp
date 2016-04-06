package tftp.core.packet;

import tftp.core.Mode;
import tftp.core.TFTPException;

/**
 * Represents a write-request (WRQ) packet in the trivial file transfer protocol.
 */
public class WriteRequestPacket extends RequestPacket {

    /**
     * {@inheritDoc}
     */
    public WriteRequestPacket(String file, Mode mode) {
        super(file, mode);
    }

    /**
     * {@inheritDoc}
     */
    public WriteRequestPacket(byte[] bytes, int length) throws TFTPException{
        super(bytes, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacketType getPacketType() {
        return PacketType.WRITE_REQUEST;
    }

}
