package tftp.core.util;

import java.nio.charset.StandardCharsets;

/**
 * @author Sam Marsh
 */
public class StringUtil {

    public static byte[] getBytes(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
        byte[] addNull = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, addNull, 0, bytes.length);
        addNull[addNull.length - 1] = 0;
        return addNull;
    }

    public static String getString(byte[] bytes, int offset) {
        int nullPos = offset;
        while (nullPos < bytes.length && nullPos != 0) {
            ++nullPos;
        }
        int length = nullPos - offset;
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

}
