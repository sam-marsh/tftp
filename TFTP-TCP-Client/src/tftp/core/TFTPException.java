package tftp.core;

/**
 * A generic TFTP checked exception class used to describe errors in parsing TFTP packets.
 */
public class TFTPException extends Exception {

    /**
     * {@inheritDoc}
     */
    public TFTPException(String message) {
        super(message);
    }

}
