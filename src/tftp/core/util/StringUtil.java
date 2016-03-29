package tftp.core.util;

import tftp.core.Configuration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Sam Marsh
 */
public class StringUtil {

    public static void write(String string, ByteBuffer buffer) {
        buffer.put(string.getBytes(StandardCharsets.US_ASCII));
        buffer.put((byte) 0);
    }

    public static String read(ByteBuffer buffer) {
        byte[] bytes = new byte[Configuration.MAX_PACKET_LENGTH];
        byte current;
        int idx = 0;
        while (buffer.hasRemaining() && (current = buffer.get()) != 0) {
            bytes[idx++] = current;
        }
        return new String(bytes, 0, idx, StandardCharsets.US_ASCII);
    }

}
