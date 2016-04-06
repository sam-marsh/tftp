package tftp.core.packet;

import tftp.core.TFTPException;

import java.nio.ByteBuffer;

/**
 * A class that represents a generic TFTP packet.
 */
public abstract class TFTPPacket {

    /**
     * Gets the byte representation of this packet, as described in the TFTP RFC.
     *
     * @return the raw packet bytes
     */
    public abstract byte[] getPacketBytes();

    /**
     * @return the type of packet (eg. ACK, RRQ, ...)
     */
    public abstract PacketType getPacketType();

    /**
     * Given a byte array (and a length), this examines the opcode and then parses the packet
     * according to the packet type.
     *
     * @param buffer the buffer holding the TFTP packet bytes
     * @param length the length (in bytes) of the TFTP packet
     * @return a TFTPPacket with attributes described in the given byte array
     * @throws TFTPException if the packet type is unknown, or if the bytes could not be parsed correctly
     */
    public static TFTPPacket fromByteArray(byte[] buffer, int length) throws TFTPException {
        short opcode = ByteBuffer.wrap(buffer).getShort();
        PacketType type = PacketType.fromOpcode(opcode);

        switch (type) {
            case ACKNOWLEDGEMENT:
                return new AcknowledgementPacket(buffer, length);
            case DATA:
                return new DataPacket(buffer, length);
            case ERROR:
                return new ErrorPacket(buffer, length);
            case READ_REQUEST:
                return new ReadRequestPacket(buffer, length);
            case WRITE_REQUEST:
                return new WriteRequestPacket(buffer, length);
            default:
                throw new TFTPException("unknown packet type: " + type);
        }

    }

}
