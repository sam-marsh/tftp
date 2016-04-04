package tftp.core.packet;

import tftp.core.TFTPException;

import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This is a class to represent the different types of TFTP packets specified in the TFTP RFC.
 */
public enum PacketType {

    READ_REQUEST("RRQ", 1),
    WRITE_REQUEST("WRQ", 2),
    DATA("DATA", 3),
    ACKNOWLEDGEMENT("ACK", 4),
    ERROR("ERR", 5);

    /**
     * A human-readable short string representing the packet type.
     */
    private final String abbreviation;

    /**
     * The opcode of this packet type.
     */
    private final short opcode;

    /**
     * Creates a packet type with the given abbreviation and opcode.
     *
     * @param abbreviation a short string describing the packet type
     * @param opcode the opcode
     */
    PacketType(String abbreviation, int opcode) {
        this.abbreviation = abbreviation;
        this.opcode = (short) opcode;
    }

    /**
     * @return the opcode of this packet type, (the first two bytes in any TFTP packet)
     */
    public short getOpcode() {
        return opcode;
    }

    /**
     * @return a string representing this packet type
     */
    @Override
    public String toString() {
        return abbreviation;
    }

    /**
     * Finds the packet type associated with a particular opcode.
     *
     * @param opcode the opcode of a packet
     * @return the associated packet type
     * @throws TFTPException if there is not a packet type for the given opcode
     */
    public static PacketType fromOpcode(short opcode) throws TFTPException {
        for (PacketType type : values()) {
            if (type.opcode == opcode) {
                return type;
            }
        }
        throw new TFTPException("no such opcode: " + opcode);
    }

}
