package tftp.core;

/**
 * Provides a representation of the different types of errors specified in the TFTP RFC.
 */
public enum ErrorType {

    UNDEFINED(0, "Not defined."),
    FILE_NOT_FOUND(1, "File not found."),
    ACCESS_VIOLATION(2, "Access violation."),
    DISK_FULL(3, "Disk full or allocation exceeded."),
    ILLEGAL_OPERATION(4, "Illegal TFTP operation."),
    UNKNOWN_ID(5, "Unknown transfer ID."),
    FILE_EXISTS(6, "File already exists."),
    NO_SUCH_USER(7, "No such user.");

    /**
     * The 'opcode' of this error, as specified in the RFC.
     */
    private final short value;

    /**
     * A generic string description of this error.
     */
    private final String meaning;

    /**
     * Creates a new error type with the given opcode and description.
     *
     * @param value the opcode of the error
     * @param meaning a brief generic description of the error
     */
    ErrorType(int value, String meaning) {
        this.value = (short) value;
        this.meaning = meaning;
    }

    /**
     * @return the error type opcode
     */
    public short getValue() {
        return value;
    }

    /**
     * @return a brief generic description of the error
     */
    @Override
    public String toString() {
        return meaning;
    }

    /**
     * Given an error opcode, this finds the associated error type. If no associated error type
     * is found, {@link #UNDEFINED} is returned.
     *
     * @param value the opcode value
     * @return the error type associated with the given value
     */
    public static ErrorType fromValue(int value) {
        for (ErrorType error : values()) {
            if (error.value == value) {
                return error;
            }
        }
        return UNDEFINED;
    }

}
