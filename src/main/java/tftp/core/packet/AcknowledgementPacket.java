package tftp.core.packet;

import tftp.core.Configuration;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class AcknowledgementPacket extends TFTPPacket {

    private static final int PACKET_LENGTH = 4;

    private final short blockNumber;
    private final byte[] bytes;

    public AcknowledgementPacket(short blockNumber) {
        this.blockNumber = (short) blockNumber;
        this.bytes = new byte[PACKET_LENGTH];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNumber);
    }

    public AcknowledgementPacket(byte[] packetData, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(packetData);
        buffer.position(2);
        this.blockNumber = buffer.getShort();
        this.bytes = new byte[length];
        System.arraycopy(packetData, 0, bytes, 0, length);
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    @Override
    public byte[] getPacketBytes() {
        return bytes;
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
