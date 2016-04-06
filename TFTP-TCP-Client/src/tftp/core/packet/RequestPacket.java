package tftp.core.packet;

import tftp.core.Mode;
import tftp.core.TFTPException;
import tftp.core.util.StringUtil;

import java.nio.ByteBuffer;

/**
 * An (abstract) definition of a TFTP request packet. RRQs and WRQs share the same functionality, just with
 * differing opcodes - so their shared functionality is implemented in this class.
 */
public abstract class RequestPacket extends TFTPPacket {

    /**
     * The filename to get/put from/to the server.
     */
    private final String fileName;

    /**
     * The mode of transfer.
     */
    private final Mode mode;

    /**
     * The raw packet bytes.
     */
    private final byte[] bytes;

    /**
     * Creates a new request packet with the given file name and transfer mode.
     *
     * @param fileName the name of the file to get/put
     * @param mode the transfer mode to use
     */
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

    /**
     * Creates a new request packet from the raw packet bytes.
     *
     * @param bytes the buffer holding byte representation of the packet
     * @param length the length of the packet (in bytes)
     * @throws TFTPException if the transfer mode described in the raw packet bytes does not exist
     */
    public RequestPacket(byte[] bytes, int length) throws TFTPException {
        this.fileName = StringUtil.getString(bytes, 2);

        //we found the file-name string already (starting at offset 2). now need to find start of mode
        // string - so increment a counter until the null byte indicating the end of the filename is found,
        // then the mode string starts at the offset immediately after the null byte
        int modeStringOffset = 2;
        while (bytes[modeStringOffset] != 0 && modeStringOffset < length) {
            ++modeStringOffset;
        }
        ++modeStringOffset;

        this.mode = Mode.fromName(StringUtil.getString(bytes, modeStringOffset));
        this.bytes = new byte[length];
        System.arraycopy(bytes, 0, this.bytes, 0, length);
    }

    /**
     * @return the file name specified in this request packet
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the mode of transfer specified in this request packet
     */
    public Mode getMode() {
        return mode;
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
    public String toString() {
        return String.format("%s[file=%s,mode=%s]", getPacketType(), getFileName(), getMode());
    }

}
