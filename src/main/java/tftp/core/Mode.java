package tftp.core;

/**
 * @author Sam Marsh
 */
public enum Mode {

    ASCII("netascii"),
    OCTET("octet"),
    MAIL("mail");

    private final String name;

    Mode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Mode fromName(String name) throws TFTPException {
        for (Mode mode : values()) {
            if (mode.name.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new TFTPException("no such mode: " + name);
    }

}
