package tftp.core.packet;

import tftp.core.Configuration;

import java.nio.ByteBuffer;

/**
 * Represents a data packet in the trivial file transfer protocol.
 */
public class DataPacket extends TFTPPacket {

    /**
     * The first four bytes of the data packet are the opcode and block number, so the offset of the raw
     * data contained in this TFTP packet starts at offset 4.
     */
    public static final int DATA_OFFSET = 4;

    /**
     * The block number assigned to this data packet.
     */
    private final short blockNumber;

    /**
     * The length of the data contained in this packet (in bytes).
     */
    private final int dataLength;

    /**
     * The raw TFTP packet bytes.
     */
    private final byte[] packetBuffer;

    /**
     * Creates a new data packet with the given block number and data bytes.
     *
     * @param blockNumber the block number assigned to this data packet
     * @param dataBuffer the buffer containing the data to send
     * @param dataLength the length of the data in the buffer
     */
    public DataPacket(short blockNumber, byte[] dataBuffer, int dataLength) {
        this.blockNumber = blockNumber;
        this.dataLength = dataLength;
        this.packetBuffer = new byte[dataLength + DATA_OFFSET];
        ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNumber);
        buffer.put(dataBuffer, 0, dataLength);
    }

    /**
     * Retrieves a data packet instance from a raw TFTP packet.
     *
     * @param packetBuffer the buffer containing the packet data
     * @param length the length of the packet in bytes
     */
    public DataPacket(byte[] packetBuffer, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
        buffer.position(2);
        this.blockNumber = buffer.getShort();
        this.dataLength = length - DATA_OFFSET;

        this.packetBuffer = new byte[length];
        System.arraycopy(packetBuffer, 0, this.packetBuffer, 0, length);
    }

    /**
     * @return the block number assigned to this data packet
     */
    public short getBlockNumber() {
        return blockNumber;
    }

    /**
     * @return the length of the data contained in this packet (512 unless the last packet)
     */
    public int getDataLength() {
        return dataLength;
    }

    /**
     * Checks if this is the final packet from the sender. This is done by checking
     * if the data length is 512 - if less than this, it is the last packet.
     *
     * @return true if this is the terminating packet, otherwise false.
     */
    public boolean isFinalPacket() {
        return dataLength < Configuration.MAX_DATA_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getPacketBytes() {
        return packetBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacketType getPacketType() {
        return PacketType.DATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s[block=%d,length=%d]", getPacketType(), getBlockNumber(), dataLength);
    }

}
