package tftp.core.packet;

/**
 * @author Sam Marsh
 */
public enum PacketType {

    READ_REQUEST("RRQ", 1),
    WRITE_REQUEST("WRQ", 2),
    DATA("DATA", 3),
    ACKNOWLEDGEMENT("ACK", 4),
    ERROR("ERR", 5);

    private final String abbreviation;
    private final short opcode;

    PacketType(String abbreviation, int opcode) {
        this.abbreviation = abbreviation;
        this.opcode = (short) opcode;
    }

    public short getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return abbreviation;
    }

    public static PacketType fromOpcode(short opcode) {
        for (PacketType type : values()) {
            if (type.opcode == opcode) {
                return type;
            }
        }
        throw new IllegalArgumentException("no such opcode: " + opcode);
    }

}
