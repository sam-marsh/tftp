package tftp.core.util;

import java.nio.charset.StandardCharsets;

/**
 * Provides string-related utilities for parsing TFTP packets according to the TFTP RFC.
 */
public class StringUtil {

    /**
     * Gives the bytes of a string in netascii.
     *
     * @param string the string to return
     * @return the representation of the string in netascii as a byte array
     */
    public static byte[] getBytes(String string) {
        //get bytes with correct character format
        byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
        byte[] addNull = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, addNull, 0, bytes.length);
        //add a null character at the end
        addNull[addNull.length - 1] = 0;
        return addNull;
    }

    /**
     * Given a byte array representing a TFTP string, return a java String object.
     *
     * @param bytes the buffer holding the string
     * @param offset the offset where the string starts
     * @return a String object representing the same string as the one in the buffer
     */
    public static String getString(byte[] bytes, int offset) {
        //first, find the null byte position
        int nullPos = offset;
        while (nullPos < bytes.length && bytes[nullPos] != 0) {
            ++nullPos;
        }
        //given the null byte position, calculate the length of the string
        int length = nullPos - offset;
        //return a new string with the correct character format
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

}
