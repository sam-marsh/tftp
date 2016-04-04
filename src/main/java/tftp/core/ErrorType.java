package tftp.core;

/**
 * @author Sam Marsh
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

    private final short value;
    private final String meaning;

    ErrorType(int value, String meaning) {
        this.value = (short) value;
        this.meaning = meaning;
    }

    public short getValue() {
        return value;
    }

    @Override
    public String toString() {
        return meaning;
    }

    public static ErrorType fromValue(int value) {
        for (ErrorType error : values()) {
            if (error.value == value) {
                return error;
            }
        }
        return UNDEFINED;
    }

}
