package tftp.core.packet;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public class DataPacket extends TFTPPacket {

    private final short blockNumber;
    private final byte[] data;
    private final ByteBuffer buffer;

    public DataPacket(short blockNumber, byte[] data) {
        this.blockNumber = blockNumber;
        this.data = data;
        this.buffer = ByteBuffer.allocate(4 + data.length);
        buffer.putShort(getPacketType().getOpcode());
        buffer.putShort(blockNumber);
        buffer.put(data);
        buffer.flip();
    }

    public DataPacket(ByteBuffer buffer) {
        buffer.position(2);
        blockNumber = buffer.getShort();
        data = new byte[buffer.remaining()];
        buffer.put(data);
        buffer.rewind();
        this.buffer = buffer;
    }

    @Override
    public ByteBuffer getRawPacket() {
        return buffer;
    }

    @Override
    public PacketType getPacketType() {
        return PacketType.DATA;
    }

}
