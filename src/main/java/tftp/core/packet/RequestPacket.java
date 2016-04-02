package tftp.core.packet;

import tftp.core.Configuration;
import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * @author Sam Marsh
 */
public abstract class RequestPacket extends TFTPPacket {

    private final String fileName;
    private final Mode mode;
    private final byte[] bytes;

    public RequestPacket(String fileName, Mode mode) {
        this.fileName = fileName;
        this.mode = mode;

        byte[] fileNameBytes = StringUtil.getBytes(fileName);
        byte[] modeBytes = StringUtil.getBytes(mode.getName());
        this.bytes = new byte[fileNameBytes.length + modeBytes.length + 2];

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putShort(getPacketType().getOpcode());
        buffer.put(fileNameBytes);
        buffer.put(modeBytes);
    }

    public RequestPacket(byte[] bytes, int length) throws TFTPException {
        this.fileName = StringUtil.getString(bytes, 2);

        int modeStringOffset = 2;
        while (bytes[modeStringOffset] != 0 && modeStringOffset < length) {
            ++modeStringOffset;
        }
        ++modeStringOffset;

        this.mode = Mode.fromName(StringUtil.getString(bytes, modeStringOffset));
        this.bytes = new byte[length];
        System.arraycopy(bytes, 0, this.bytes, 0, length);
    }

    public String getFileName() {
        return fileName;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public byte[] getPacketBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return String.format("%s[file=%s,mode=%s]", getPacketType(), getFileName(), getMode());
    }

}
