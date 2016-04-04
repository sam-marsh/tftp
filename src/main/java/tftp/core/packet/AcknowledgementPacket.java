package tftp.core.packet;

import java.nio.ByteBuffer;

/**
 * Represents an ACK in the trivial file transfer protocol.
 */
public class AcknowledgementPacket extends TFTPPacket {

    /**
     * This packet has constant length: a short representing the opcode, and the short representing the
     * acknowledged block number.
     */
    private static final int PACKET_LENGTH = 4;

    /**
     * The block number to acknowledge.
     */
    private final short blockNumber;

    /**
     * The raw TFTP packet bytes.
     */
    private final byte[] bytes;

    /**
     * Creates a new ACK with the given block number.
     *
     * @param blockNumber the block number to acknowledge
     */
    public AcknowledgementPacket(short blockNumber) {
        this.blockNumber = blockNumber;
        this.bytes = new byte[PACKET_LENGTH];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNumber);
    }

    /**
     * Creates a new ACK from the raw packet bytes. The packet data is assumed to start at offset 0.
     *
     * @param packetData the buffer holding the packet bytes
     * @param length the length of the packet in the buffer. Should always be 4.
     */
    public AcknowledgementPacket(byte[] packetData, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(packetData);
        buffer.position(2);
        this.blockNumber = buffer.getShort();
        this.bytes = new byte[length];
        System.arraycopy(packetData, 0, bytes, 0, length);
    }

    /**
     * Gives the block number acknowledged by this packet.
     *
     * @return the block number
     */
    public int getBlockNumber() {
        return blockNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacketType getPacketType() {
        return PacketType.ACKNOWLEDGEMENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s{block=%d}", getPacketType(), getBlockNumber());
    }

}
