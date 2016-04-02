package tftp.core.packet;

import tftp.core.Configuration;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class DataPacket extends TFTPPacket {

    private static final int DATA_OFFSET = 4;

    private final short blockNumber;
    private final int dataLength;
    private final byte[] packetBuffer;

    public DataPacket(short blockNumber, byte[] dataBuffer) {
        this.blockNumber = blockNumber;
        this.dataLength = dataBuffer.length - DATA_OFFSET;
        this.packetBuffer = new byte[dataBuffer.length + DATA_OFFSET];
        ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNumber);
        buffer.put(dataBuffer);
    }

    public DataPacket(byte[] packetBuffer, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(packetBuffer);
        buffer.position(2);
        this.blockNumber = buffer.getShort();
        this.dataLength = length - DATA_OFFSET;

        this.packetBuffer = new byte[length];
        System.arraycopy(packetBuffer, 0, this.packetBuffer, 0, length);
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    public boolean isFinal() {
        return dataLength < Configuration.MAX_DATA_LENGTH;
    }

    @Override
    public byte[] getPacketBytes() {
        return packetBuffer;
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.DATA;
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", getPacketType(), getBlockNumber());
    }

}
