package tftp.core.packet;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class AcknowledgementPacket extends TFTPPacket {

    private static final int PACKET_LENGTH = 4;

    private final int blockNumber;
    private final ByteBuffer buffer;

    public AcknowledgementPacket(short blockNumber) {
        this.blockNumber = blockNumber;
        this.buffer = ByteBuffer.allocate(PACKET_LENGTH);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNumber);
        buffer.flip();
    }

    public AcknowledgementPacket(ByteBuffer buffer) {
        buffer.position(2);
        this.blockNumber = buffer.getShort();
        buffer.rewind();
        this.buffer = buffer;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    @Override
    public ByteBuffer getRawPacket() {
        return buffer;
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.ACKNOWLEDGEMENT;
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", getPacketType(), getBlockNumber());
    }

}
