package tftp.core;

/**
 * Represents a TFTP mode of transfer.
 */
public enum Mode {

    ASCII("netascii"),
    OCTET("octet"),
    MAIL("mail");

    /**
     * The name used to identify this transfer mode.
     */
    private final String name;

    /**
     * Creates a new TFTP mode with the given name.
     *
     * @param name a string description of the mode
     */
    Mode(String name) {
        this.name = name;
    }

    /**
     * @return the name of the mode
     */
    public String getName() {
        return name;
    }

    /**
     * @return same as {@link #getName()}
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Finds the mode associated with the given name.
     *
     * @param name the name of the mode
     * @return the associated mode
     * @throws TFTPException if no such associated mode exists
     */
    public static Mode fromName(String name) throws TFTPException {
        for (Mode mode : values()) {
            if (mode.name.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new TFTPException("no such mode: " + name);
    }

}
